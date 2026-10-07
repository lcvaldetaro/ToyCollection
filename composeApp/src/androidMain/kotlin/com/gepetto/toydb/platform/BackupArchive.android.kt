package com.gepetto.toydb.platform

import club.gepetto.GcLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import okio.Path
import okio.Sink
import okio.buffer

internal fun isValidPhotoName(name: String): Boolean {
    if (name.isEmpty() || name == "." || name == "..") return false
    if (name.contains('/') || name.contains('\\') || name.contains(':') || name.contains("..")) return false
    for (ch in name) {
        val code = ch.code
        if (code < 32 || code == 127) return false
    }
    return true
}

actual object BackupArchive {
    actual fun write(
        sink: Sink,
        textEntries: Map<String, String>,
        photos: List<BackupPhoto>,
        onPhoto: (done: Int, total: Int) -> Unit
    ) {
        try {
            ZipOutputStream(sink.buffer().outputStream()).use { zos ->
                for ((name, text) in textEntries) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    zos.write(text.encodeToByteArray())
                    zos.closeEntry()
                }
                val buffer = ByteArray(8192)
                val total = photos.size
                photos.forEachIndexed { index, photo ->
                    val entry = ZipEntry(photo.entryName)
                    if (photo.modified > 0) {
                        entry.time = photo.modified
                    }
                    zos.putNextEntry(entry)
                    val file = File(photo.source.toString())
                    if (file.exists() && file.isFile) {
                        FileInputStream(file).use { fis ->
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                zos.write(buffer, 0, read)
                            }
                        }
                    }
                    zos.closeEntry()
                    onPhoto(index + 1, total)
                }
                zos.finish()
            }
        } finally {
            runCatching { sink.close() }
        }
    }

    actual fun readTextEntries(archive: Path): Map<String, String> {
        val file = File(archive.toString())
        val entries = mutableMapOf<String, String>()
        ZipFile(file).use { zip ->
            val e = zip.entries()
            while (e.hasMoreElements()) {
                val entry = e.nextElement()
                val name = entry.name
                val isDataChild = name.startsWith("data/") && !entry.isDirectory &&
                    name.endsWith(".json") && !name.removePrefix("data/").contains('/')
                if (name == "manifest.json" || name == "photos.json" || isDataChild) {
                    val text = zip.getInputStream(entry).use { it.readBytes().decodeToString() }
                    entries[name] = text
                }
            }
        }
        return entries
    }

    actual fun extractPhotos(
        archive: Path,
        targetDir: Path,
        modifiedTimes: Map<String, Long>,
        onPhoto: (done: Int, total: Int) -> Unit
    ): Int {
        val targetFileDir = File(targetDir.toString())
        if (!targetFileDir.exists()) {
            targetFileDir.mkdirs()
        }
        var extractedCount = 0
        ZipFile(File(archive.toString())).use { zip ->
            val entriesList = mutableListOf<Pair<ZipEntry, String>>()
            val e = zip.entries()
            while (e.hasMoreElements()) {
                val entry = e.nextElement()
                if (entry.name.startsWith("images/") && !entry.isDirectory) {
                    val photoName = entry.name.removePrefix("images/")
                    if (isValidPhotoName(photoName)) {
                        entriesList.add(entry to photoName)
                    } else {
                        GcLog.w("BackupArchive: Skipping invalid image entry: ${entry.name}")
                    }
                }
            }
            val total = entriesList.size
            val buffer = ByteArray(8192)
            for ((index, pair) in entriesList.withIndex()) {
                val (entry, photoName) = pair
                val targetFile = File(targetFileDir, photoName)
                val partialFile = File(targetFileDir, "$photoName.partial")
                if (partialFile.exists()) {
                    partialFile.delete()
                }
                try {
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(partialFile).use { output ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    var success = false
                    for (attempt in 1..3) {
                        if (targetFile.exists()) {
                            targetFile.delete()
                        }
                        if (partialFile.renameTo(targetFile)) {
                            success = true
                            break
                        }
                        if (attempt < 3) {
                            try { Thread.sleep(100) } catch (_: InterruptedException) {}
                        }
                    }
                    if (!success) {
                        throw IOException("Failed to rename $partialFile to $targetFile after 3 attempts")
                    }
                    val time = modifiedTimes[photoName] ?: (if (entry.time > 0) entry.time else -1L)
                    if (time > 0) {
                        val ok = targetFile.setLastModified(time)
                        if (!ok) {
                            GcLog.w("BackupArchive: Could not set mtime for $photoName")
                        }
                    }
                    extractedCount++
                    onPhoto(index + 1, total)
                } catch (t: Throwable) {
                    if (partialFile.exists()) {
                        partialFile.delete()
                    }
                    throw t
                }
            }
        }
        return extractedCount
    }
}
