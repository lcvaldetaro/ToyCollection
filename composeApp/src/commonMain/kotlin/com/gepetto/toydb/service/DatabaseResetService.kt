package com.gepetto.toydb.service

import club.gepetto.GcLog
import club.gepetto.utils.ioDispatcher
import coil3.SingletonImageLoader
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.systemFileSystem
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import toydb.composeapp.generated.resources.Res

object DatabaseResetService {
    private const val TAG = "DatabaseResetService"

    /**
     * Resets the database to defaults for a new user to maintain their own collection:
     * - Deletes all toy records (0 toys).
     * - Preserves the makers table intact (directory of 198 manufacturers).
     * - Ensures default category settings are present.
     * - Wipes out the web synchronization URL (base_url = "") so the app never re-downloads
     *   Gepetto's collection.
     * - Sets the collection title to "My Toy Collection".
     * - Clears server sync metadata (html_sync_imported_*).
     * - Purges toy-specific photos from data_path while strictly preserving any photo
     *   referenced by a manufacturer (makers.bitmaps).
     * - Clears Coil image memory and disk caches.
     */
    suspend fun resetDatabaseToDefaults(
        db: ToyDatabase,
        platformContext: Any? = null
    ): Result<Unit> = CollectionWriteLock.mutex.withLock {
        withContext(ioDispatcher) {
            try {
                GcLog.i(TAG, "Starting reset of database to defaults...")

                // 1. Build whitelist of protected maker photos
                val protectedMakerPhotos = mutableSetOf<String>()
                try {
                    val makerCursor = db.query("SELECT bitmaps FROM makers")
                    while (makerCursor.next()) {
                        val bitmaps = makerCursor.getString("bitmaps") ?: ""
                        bitmaps.split(' ').filter { it.isNotBlank() }.forEach {
                            val trimmed = it.trim().lowercase()
                            protectedMakerPhotos.add(trimmed)
                            val withoutExt = trimmed.substringBeforeLast('.')
                            if (withoutExt.isNotEmpty()) {
                                protectedMakerPhotos.add(withoutExt)
                            }
                        }
                    }
                    makerCursor.close()
                    GcLog.d(TAG, "Collected ${protectedMakerPhotos.size} protected maker photo tokens.")
                } catch (e: Exception) {
                    GcLog.e(TAG, "Error collecting protected maker photos: ${e.message}", e)
                }

                // 2. Identify candidate toy photos from toys table before deletion
                val candidateToyPhotos = mutableSetOf<String>()
                try {
                    val categoryPrefixMap = mutableMapOf<String, String>()
                    val catCursor = db.query("SELECT category, image_prefix FROM category_settings")
                    while (catCursor.next()) {
                        val cat = catCursor.getString("category") ?: ""
                        val prefix = catCursor.getString("image_prefix") ?: ""
                        if (cat.isNotEmpty()) categoryPrefixMap[cat] = prefix
                    }
                    catCursor.close()

                    val extensions = listOf("jpg", "jpeg", "png", "gif", "webp")
                    val toyCursor = db.query("SELECT ref_num, toy_type, picture, bitmaps FROM toys")
                    while (toyCursor.next()) {
                        val refNum = toyCursor.getInt("ref_num") ?: 0
                        val toyType = toyCursor.getString("toy_type") ?: ""
                        val picture = toyCursor.getString("picture")?.trim() ?: ""
                        val bitmaps = toyCursor.getString("bitmaps")?.trim() ?: ""

                        if (picture.isNotEmpty()) {
                            candidateToyPhotos.add(picture.lowercase())
                        }
                        bitmaps.split(' ').filter { it.isNotBlank() }.forEach {
                            candidateToyPhotos.add(it.trim().lowercase())
                        }
                        val prefix = categoryPrefixMap[toyType] ?: "car"
                        for (ext in extensions) {
                            candidateToyPhotos.add("${prefix}${refNum}.$ext".lowercase())
                        }
                    }
                    toyCursor.close()
                    GcLog.d(TAG, "Identified ${candidateToyPhotos.size} candidate toy photos for removal.")
                } catch (e: Exception) {
                    GcLog.e(TAG, "Error identifying candidate toy photos: ${e.message}", e)
                }

                // 3. Whitelist Subtraction: Never delete any photo used by a maker!
                val filesToDelete = candidateToyPhotos.filter { candidate ->
                    candidate !in protectedMakerPhotos && candidate.substringBeforeLast('.') !in protectedMakerPhotos
                }.toSet()
                GcLog.d(TAG, "${filesToDelete.size} toy-exclusive photos scheduled for file deletion.")

                // 4. In-transaction database wipe & reset
                val categorySettingsJson = Res.readBytes("files/category_settings.json").decodeToString()
                db.transaction {
                    db.execute("DELETE FROM toys")
                    ImportExportService.importCategorySettings(db, categorySettingsJson)
                    // makers table is NOT deleted
                    db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('base_url', '')")
                    db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('app_title', 'My Toy Collection')")
                    db.execute("DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'")
                }
                GcLog.i(TAG, "Database transaction committed successfully.")

                // 5. Purge safe toy photos from data_path
                val repository = ToyRepository(db)
                val dataPath = repository.getDataPathSetting()
                if (!dataPath.isNullOrBlank()) {
                    val dir = dataPath.toPath()
                    if (systemFileSystem.metadataOrNull(dir)?.isDirectory == true) {
                        var deletedCount = 0
                        for (fileName in filesToDelete) {
                            val file = dir.div(fileName)
                            try {
                                if (systemFileSystem.exists(file)) {
                                    systemFileSystem.delete(file)
                                    deletedCount++
                                }
                            } catch (delEx: Exception) {
                                GcLog.w(TAG, "Failed to delete toy photo $fileName: ${delEx.message}")
                            }
                        }
                        GcLog.i(TAG, "Purged $deletedCount toy photos from $dataPath. Preserved all maker photos.")
                    }
                }

                // 6. Coil image cache eviction
                if (platformContext != null) {
                    try {
                        val loader = SingletonImageLoader.get(platformContext as coil3.PlatformContext)
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                        GcLog.d(TAG, "Cleared Coil memory and disk caches.")
                    } catch (coilEx: Throwable) {
                        GcLog.w(TAG, "Failed to clear Coil cache: ${coilEx.message}")
                    }
                }

                Result.success(Unit)
            } catch (e: Throwable) {
                GcLog.e(TAG, "Failed to reset database to defaults: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
