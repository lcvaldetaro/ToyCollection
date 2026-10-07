package com.gepetto.toydb.platform

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import club.gepetto.GcLog
import club.gepetto.utils.GcAppInfo
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.buffer
import okio.sink
import okio.source

private data class MediaStoreEntry(val uri: android.net.Uri, val displayName: String, val size: Long)

actual object BackupFileHelper {

    private fun queryDownloads(context: Context): List<MediaStoreEntry> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val list = mutableListOf<MediaStoreEntry>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("toy_collection_backup%.zip", "Download%")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                list.add(MediaStoreEntry(uri, name, size))
            }
        }
        return list
    }

    actual suspend fun saveBackup(
        suggestedName: String,
        dialogTitle: String,
        write: suspend (Sink) -> Unit
    ): BackupSaveResult = withContext(Dispatchers.IO) {
        val context = GcAppInfo.application_Context as? Context
            ?: return@withContext BackupSaveResult.Failed("no context")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val existing = queryDownloads(context)
            val exact = existing.firstOrNull { it.displayName == ANDROID_BACKUP_FILE_NAME }
            val chosen = exact ?: existing.firstOrNull()
            val (uri, isNewEntry) = if (chosen != null) {
                chosen.uri to false
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, ANDROID_BACKUP_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val insertedUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext BackupSaveResult.Failed("Could not create MediaStore entry in Downloads")
                insertedUri to true
            }

            try {
                val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Could not open output stream for $uri")
                val sink = outputStream.sink()
                try {
                    write(sink)
                } finally {
                    runCatching { sink.close() }
                }
                if (isNewEntry) {
                    val doneValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, doneValues, null, null)
                }
                val finalName = context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: ANDROID_BACKUP_FILE_NAME
                BackupSaveResult.Saved("Downloads/$finalName")
            } catch (c: CancellationException) {
                if (isNewEntry) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
                throw c
            } catch (e: Throwable) {
                GcLog.e("BackupFileHelper: Error saving backup on Android: ${e.message}")
                if (isNewEntry) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
                BackupSaveResult.Failed(e.message ?: "Unknown error")
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val target = File(downloadsDir, ANDROID_BACKUP_FILE_NAME)
            val partial = File(downloadsDir, "$ANDROID_BACKUP_FILE_NAME.partial")
            if (partial.exists()) {
                partial.delete()
            }
            try {
                val sink = partial.sink()
                try {
                    write(sink)
                } finally {
                    runCatching { sink.close() }
                }
                if (target.exists()) {
                    target.delete()
                }
                if (!partial.renameTo(target)) {
                    throw IOException("Failed to rename partial backup file to $target")
                }
                BackupSaveResult.Saved("Downloads/$ANDROID_BACKUP_FILE_NAME")
            } catch (c: CancellationException) {
                if (partial.exists()) {
                    partial.delete()
                }
                throw c
            } catch (e: Throwable) {
                GcLog.e("BackupFileHelper: Error saving backup on Android: ${e.message}")
                if (partial.exists()) {
                    partial.delete()
                }
                BackupSaveResult.Failed(e.message ?: "Unknown error")
            }
        }
    }

    actual suspend fun openBackup(dialogTitle: String): BackupOpenResult = withContext(Dispatchers.IO) {
        val context = GcAppInfo.application_Context as? Context
            ?: return@withContext BackupOpenResult.Failed("no context")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val entries = queryDownloads(context)
            if (entries.isEmpty()) {
                return@withContext BackupOpenResult.NotFound
            }
            val entry = entries.first()
            var size = entry.size
            if (size <= 0L) {
                try {
                    context.contentResolver.openFileDescriptor(entry.uri, "r")?.use { pfd ->
                        size = pfd.statSize
                    }
                } catch (_: Throwable) {}
            }
            val neededSpace = 2 * size + 50 * 1024 * 1024L
            if (context.cacheDir.usableSpace < neededSpace) {
                return@withContext BackupOpenResult.NoSpace
            }
            val tempFile = File(context.cacheDir, "restore_backup.zip")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            try {
                val inputStream = context.contentResolver.openInputStream(entry.uri)
                    ?: return@withContext BackupOpenResult.Failed("Could not open ${entry.uri}")
                inputStream.source().buffer().use { src ->
                    tempFile.sink().buffer().use { dst ->
                        src.readAll(dst)
                    }
                }
                BackupOpenResult.Opened(tempFile.absolutePath.toPath(), isTemporary = true)
            } catch (e: Throwable) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                GcLog.e("BackupFileHelper: Failed to copy backup to cache: ${e.message}")
                BackupOpenResult.Failed(e.message ?: "Failed to open backup")
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, ANDROID_BACKUP_FILE_NAME)
            if (!file.exists() || !file.isFile) {
                return@withContext BackupOpenResult.NotFound
            }
            val neededSpace = file.length() + 50 * 1024 * 1024L
            if (context.filesDir.usableSpace < neededSpace) {
                return@withContext BackupOpenResult.NoSpace
            }
            BackupOpenResult.Opened(file.absolutePath.toPath(), isTemporary = false)
        }
    }

    actual fun release(path: Path) {
        try {
            val file = File(path.toString())
            val context = GcAppInfo.application_Context as? Context
            if (context != null) {
                val cacheCanonical = context.cacheDir.canonicalPath
                if (file.canonicalPath.startsWith(cacheCanonical)) {
                    file.delete()
                }
            }
        } catch (e: Throwable) {
            GcLog.w("BackupFileHelper: release failed: ${e.message}")
        }
    }
}
