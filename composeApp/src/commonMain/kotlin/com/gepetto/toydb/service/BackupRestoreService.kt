package com.gepetto.toydb.service

import club.gepetto.GcLog
import club.gepetto.utils.ioDispatcher
import com.gepetto.toydb.CommonConfig
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.platform.BackupArchive
import com.gepetto.toydb.platform.BackupPhoto
import com.gepetto.toydb.utils.systemFileSystem
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink

const val BACKUP_FORMAT_ID = "gepetto-toy-collection-backup"
const val BACKUP_FORMAT_VERSION = 1

/**
 * Used to write and read manifest.json and photos.json, and to read the data/ files.
 * encodeDefaults = true: every field is written. Same ignoreUnknownKeys/coerceInputValues as ImportExportService.
 */
internal val backupJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class BackupManifest(
    val format: String,
    val formatVersion: Int,
    val createdAt: String,
    val appVersionCode: String,
    val categories: Int,
    val makers: Int,
    val toys: Int,
    val photos: Int,
    val categoryFiles: Map<String, String> = emptyMap()
)

@Serializable
data class BackupPhotoInfo(val name: String, val size: Long, val modified: Long)

@Serializable
data class BackupPhotoIndex(val photos: List<BackupPhotoInfo>)

internal fun isPortableSettingKey(key: String): Boolean =
    !key.startsWith("sftp_") &&
    !key.startsWith("html_sync_imported_") &&
    key !in setOf("images_path", "data_path", "import_export_path")

private fun extractJsonDate(text: String): String {
    return try {
        backupJson.parseToJsonElement(text).jsonObject["date"]?.jsonPrimitive?.content ?: ""
    } catch (_: Throwable) {
        ""
    }
}

object BackupRestoreService {
    data class BackupSummary(
        val categories: Int,
        val makers: Int,
        val toys: Int,
        val photos: Int,
        val missingPhotos: Int = 0
    )

    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

    class ValidatedBackup internal constructor(
        val archive: Path,
        val manifest: BackupManifest,
        internal val texts: Map<String, String>,
        internal val photoTimes: Map<String, Long>
    )

    data class BackupContent(
        val textEntries: Map<String, String>,
        val photos: List<BackupPhoto>,
        val summary: BackupSummary
    )

    private data class CategoryRow(val category: String, val imagePrefix: String)

    /** Builds all JSON texts (manifest.json, photos.json, and data folder files) and the photo list. Writes nothing. Does not take the lock. Easy to unit test. */
    fun collectBackupContent(db: ToyDatabase): BackupContent {
        val dataPathStr = ToyRepository(db).getDataPathSetting()
        if (dataPathStr.isNullOrBlank()) {
            throw IllegalStateException("no data directory")
        }
        val imagesDirPath = dataPathStr.toPath()
        val dirMeta = systemFileSystem.metadataOrNull(imagesDirPath)
        if (dirMeta?.isDirectory != true) {
            throw IllegalStateException("no data directory")
        }

        val diskFiles = systemFileSystem.listOrNull(imagesDirPath) ?: emptyList()
        val caseInsensitiveDiskMap = LinkedHashMap<String, String>()
        for (p in diskFiles) {
            val meta = systemFileSystem.metadataOrNull(p)
            if (meta?.isRegularFile == true) {
                val fileName = p.name
                val key = fileName.lowercase()
                if (!caseInsensitiveDiskMap.containsKey(key)) {
                    caseInsensitiveDiskMap[key] = fileName
                }
            }
        }

        val categoriesList = mutableListOf<CategoryRow>()
        val catCursor = db.query("SELECT category, image_prefix FROM category_settings ORDER BY category ASC")
        while (catCursor.next()) {
            categoriesList.add(
                CategoryRow(
                    category = catCursor.getString("category") ?: "",
                    imagePrefix = catCursor.getString("image_prefix") ?: ""
                )
            )
        }
        catCursor.close()

        val categoryFiles = mutableMapOf<String, String>()
        val usedFileNames = mutableSetOf<String>()
        for (catRow in categoriesList) {
            val category = catRow.category
            val prefix = catRow.imagePrefix
            val candidateNames = sequence {
                yield("${prefix}list.json")
                yield("${category}list.json")
                var i = 2
                while (true) {
                    yield("${category}_${i}list.json")
                    i++
                }
            }
            val chosenFileName = candidateNames.first { it !in usedFileNames }
            usedFileNames.add(chosenFileName)
            categoryFiles[category] = "data/$chosenFileName"
        }

        val textEntries = LinkedHashMap<String, String>()

        // 1. Export data tables
        textEntries["data/category_settings.json"] = ImportExportService.exportCategorySettings(db)
        textEntries["data/carmaker.json"] = ImportExportService.exportMakers(db)
        textEntries["data/app_settings.json"] = ImportExportService.exportAppSettings(db, keyFilter = ::isPortableSettingKey)

        for (catRow in categoriesList) {
            val toyJson = ImportExportService.exportToys(db, catRow.category)
            val entryPath = categoryFiles[catRow.category]!!
            textEntries[entryPath] = toyJson
        }

        // Count makers and toys
        var makerCount = 0
        val makerCursor = db.query("SELECT COUNT(*) AS c FROM makers")
        if (makerCursor.next()) {
            makerCount = makerCursor.getInt("c") ?: 0
        }
        makerCursor.close()

        var totalToys = 0
        val toyCursor = db.query("SELECT COUNT(*) AS c FROM toys")
        if (toyCursor.next()) {
            totalToys = toyCursor.getInt("c") ?: 0
        }
        toyCursor.close()

        val knownCategories = categoriesList.map { it.category }.toSet()
        var unknownCatCount = 0
        val allToysCursor = db.query("SELECT toy_type FROM toys")
        while (allToysCursor.next()) {
            val t = allToysCursor.getString("toy_type") ?: ""
            if (t !in knownCategories) {
                unknownCatCount++
            }
        }
        allToysCursor.close()
        if (unknownCatCount > 0) {
            GcLog.w("BackupRestoreService: $unknownCatCount toys have unknown category")
        }

        // Collect photo names
        val photoNamesMap = LinkedHashMap<String, String>()
        fun addPhotoName(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            val key = trimmed.lowercase()
            if (!photoNamesMap.containsKey(key)) {
                photoNamesMap[key] = trimmed
            }
        }

        val makersCursor = db.query("SELECT bitmaps FROM makers")
        while (makersCursor.next()) {
            val bitmaps = makersCursor.getString("bitmaps") ?: ""
            bitmaps.split(' ').forEach { addPhotoName(it) }
        }
        makersCursor.close()

        val extensions = listOf("jpg", "jpeg", "png", "gif", "webp")
        for (catRow in categoriesList) {
            val prefix = catRow.imagePrefix
            val toysCursor = db.query(
                "SELECT ref_num, picture, bitmaps FROM toys WHERE toy_type = ?",
                listOf(catRow.category)
            )
            while (toysCursor.next()) {
                val refNum = toysCursor.getInt("ref_num") ?: 0
                val pic = toysCursor.getString("picture")?.trim() ?: ""
                if (pic.isNotEmpty()) {
                    addPhotoName(pic)
                }
                for (ext in extensions) {
                    val candidate = "${prefix}${refNum}.$ext".lowercase()
                    val actualDiskName = caseInsensitiveDiskMap[candidate]
                    if (actualDiskName != null) {
                        addPhotoName(actualDiskName)
                        break
                    }
                }
                val bitmaps = toysCursor.getString("bitmaps") ?: ""
                bitmaps.split(' ').forEach { addPhotoName(it) }
            }
            toysCursor.close()
        }

        val backupPhotos = mutableListOf<BackupPhoto>()
        val photoInfos = mutableListOf<BackupPhotoInfo>()
        var missingPhotos = 0

        for ((_, collectedName) in photoNamesMap) {
            val exactPath = imagesDirPath.div(collectedName)
            val exactMeta = systemFileSystem.metadataOrNull(exactPath)
            val resolvedSourcePath: Path? = if (exactMeta?.isRegularFile == true) {
                exactPath
            } else {
                val diskName = caseInsensitiveDiskMap[collectedName.lowercase()]
                if (diskName != null) {
                    val p = imagesDirPath.div(diskName)
                    if (systemFileSystem.metadataOrNull(p)?.isRegularFile == true) p else null
                } else {
                    null
                }
            }

            if (resolvedSourcePath != null) {
                val meta = systemFileSystem.metadataOrNull(resolvedSourcePath)
                val size = meta?.size ?: 0L
                val modified = meta?.lastModifiedAtMillis ?: 0L
                backupPhotos.add(
                    BackupPhoto(
                        entryName = "images/$collectedName",
                        source = resolvedSourcePath,
                        modified = modified
                    )
                )
                photoInfos.add(
                    BackupPhotoInfo(
                        name = collectedName,
                        size = size,
                        modified = modified
                    )
                )
            } else {
                missingPhotos++
                GcLog.w("BackupRestoreService: missing photo $collectedName")
            }
        }

        val photoIndex = BackupPhotoIndex(photos = photoInfos)
        val photosJsonText = backupJson.encodeToString(BackupPhotoIndex.serializer(), photoIndex)

        val manifest = BackupManifest(
            format = BACKUP_FORMAT_ID,
            formatVersion = BACKUP_FORMAT_VERSION,
            createdAt = getCurrentDateString(),
            appVersionCode = CommonConfig.versionCodeString,
            categories = categoriesList.size,
            makers = makerCount,
            toys = totalToys,
            photos = backupPhotos.size,
            categoryFiles = categoryFiles
        )
        val manifestJsonText = backupJson.encodeToString(BackupManifest.serializer(), manifest)

        // Ensure manifest.json and photos.json are placed at the beginning
        val finalEntries = LinkedHashMap<String, String>()
        finalEntries["manifest.json"] = manifestJsonText
        finalEntries["photos.json"] = photosJsonText
        finalEntries.putAll(textEntries)

        val summary = BackupSummary(
            categories = categoriesList.size,
            makers = makerCount,
            toys = totalToys,
            photos = backupPhotos.size,
            missingPhotos = missingPhotos
        )
        return BackupContent(textEntries = finalEntries, photos = backupPhotos, summary = summary)
    }

    /** D13: CollectionWriteLock.mutex.withLock { withContext(ioDispatcher) { collectBackupContent(db) } }. Waits while a web sync or a restore runs. */
    suspend fun prepareBackup(db: ToyDatabase): BackupContent =
        CollectionWriteLock.mutex.withLock {
            withContext(ioDispatcher) {
                collectBackupContent(db)
            }
        }

    /** Streams [content] into [sink] (BackupArchive.write). Does not read the database. Returns content.summary. */
    fun writeBackup(content: BackupContent, sink: Sink, onPhoto: (Int, Int) -> Unit): BackupSummary {
        BackupArchive.write(sink, content.textEntries, content.photos, onPhoto)
        return content.summary
    }

    /** Reads and checks the archive. Throws only InvalidBackupException. Changes nothing. Call it on ioDispatcher. */
    fun readBackup(archive: Path): ValidatedBackup {
        try {
            val texts = BackupArchive.readTextEntries(archive)
            val manifestText = texts["manifest.json"]
                ?: throw InvalidBackupException("manifest.json missing")
            val manifest = backupJson.decodeFromString<BackupManifest>(manifestText)
            if (manifest.format != BACKUP_FORMAT_ID) {
                throw InvalidBackupException("Invalid format ID: ${manifest.format}")
            }
            if (manifest.formatVersion < 1 || manifest.formatVersion > BACKUP_FORMAT_VERSION) {
                throw InvalidBackupException("Unsupported format version: ${manifest.formatVersion}")
            }
            val photosText = texts["photos.json"]
                ?: throw InvalidBackupException("photos.json missing")
            val photoIndex = backupJson.decodeFromString<BackupPhotoIndex>(photosText)
            val photoTimes = photoIndex.photos.associate { it.name to it.modified }

            val catSettingsText = texts["data/category_settings.json"]
                ?: throw InvalidBackupException("data/category_settings.json missing")
            backupJson.decodeFromString<JsonCategorySettingsFile>(catSettingsText)

            val makersText = texts["data/carmaker.json"]
                ?: throw InvalidBackupException("data/carmaker.json missing")
            backupJson.decodeFromString<JsonMakersFile>(makersText)

            val appSettingsText = texts["data/app_settings.json"]
            if (appSettingsText != null) {
                backupJson.decodeFromString<JsonAppSettingsFile>(appSettingsText)
            }

            for ((entryName, entryText) in texts) {
                if (entryName.startsWith("data/") && !entryName.removePrefix("data/").contains('/')) {
                    when (entryName) {
                        "data/category_settings.json", "data/carmaker.json", "data/app_settings.json" -> {}
                        else -> {
                            backupJson.decodeFromString<JsonToysFile>(entryText)
                        }
                    }
                }
            }

            for ((cat, entryName) in manifest.categoryFiles) {
                if (entryName !in texts) {
                    throw InvalidBackupException("Category file $entryName for category $cat is missing in archive")
                }
            }

            return ValidatedBackup(
                archive = archive,
                manifest = manifest,
                texts = texts,
                photoTimes = photoTimes
            )
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException(e.message ?: e::class.simpleName ?: "invalid backup", e)
        }
    }

    /** Throws on error. On error the DB is unchanged (transaction rollback); photos may be partly copied. Holds CollectionWriteLock. */
    suspend fun restoreBackup(
        db: ToyDatabase,
        backup: ValidatedBackup,
        imagesDir: Path,
        onPhoto: (Int, Int) -> Unit
    ): BackupSummary = CollectionWriteLock.mutex.withLock {
        withContext(ioDispatcher) {
            val repository = ToyRepository(db)
            val dataPath = repository.getDataPathSetting()
            if (dataPath.isNullOrBlank() || imagesDir != dataPath.toPath()) {
                throw IllegalStateException("data directory changed")
            }
            repository.setDataPathSetting(dataPath)

            val photosExtracted = BackupArchive.extractPhotos(backup.archive, imagesDir, backup.photoTimes, onPhoto)

            val catSettingsText = backup.texts["data/category_settings.json"]
                ?: throw InvalidBackupException("data/category_settings.json missing")
            val makersText = backup.texts["data/carmaker.json"]
                ?: throw InvalidBackupException("data/carmaker.json missing")

            val importedCategoryJsons = mutableMapOf<String, String>()

            db.transaction {
                db.execute("DELETE FROM toys")
                db.execute("DELETE FROM makers")
                db.execute("DELETE FROM category_settings")

                ImportExportService.importCategorySettings(db, catSettingsText)
                ImportExportService.importMakers(db, makersText)

                val categories = mutableListOf<Pair<String, String>>()
                val catCursor = db.query("SELECT category, image_prefix FROM category_settings ORDER BY category ASC")
                while (catCursor.next()) {
                    val cat = catCursor.getString("category") ?: ""
                    val prefix = catCursor.getString("image_prefix") ?: ""
                    categories.add(cat to prefix)
                }
                catCursor.close()

                for ((cat, prefix) in categories) {
                    val explicitFile = backup.manifest.categoryFiles[cat]
                    val categoryJson = if (explicitFile != null && explicitFile in backup.texts) {
                        backup.texts[explicitFile]
                    } else {
                        val candidates = listOf(
                            "data/${prefix}list.json",
                            "data/${cat}s.json",
                            "data/${cat}list.json",
                            "data/${cat}.json"
                        )
                        candidates.firstNotNullOfOrNull { backup.texts[it] }
                    }
                    if (categoryJson != null) {
                        ImportExportService.importToys(db, cat, categoryJson)
                        importedCategoryJsons[cat] = categoryJson
                    }
                }

                val appSettingsText = backup.texts["data/app_settings.json"]
                if (appSettingsText != null) {
                    ImportExportService.importAppSettings(db, appSettingsText, keyFilter = ::isPortableSettingKey)
                }

                db.execute("DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'")

                val catDate = extractJsonDate(catSettingsText)
                val catHash = HtmlSyncService.calculateHash(catSettingsText)
                db.execute(
                    "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                    listOf("html_sync_imported_date_category_settings.json", catDate)
                )
                db.execute(
                    "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                    listOf("html_sync_imported_hash_category_settings.json", catHash)
                )

                val makersDate = extractJsonDate(makersText)
                val makersHash = HtmlSyncService.calculateHash(makersText)
                db.execute(
                    "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                    listOf("html_sync_imported_date_makers", makersDate)
                )
                db.execute(
                    "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                    listOf("html_sync_imported_hash_makers", makersHash)
                )

                for ((cat, catJson) in importedCategoryJsons) {
                    val date = extractJsonDate(catJson)
                    val hash = HtmlSyncService.calculateHash(catJson)
                    db.execute(
                        "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                        listOf("html_sync_imported_date_$cat", date)
                    )
                    db.execute(
                        "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
                        listOf("html_sync_imported_hash_$cat", hash)
                    )
                }
            }

            var finalCategories = 0
            val fcCursor = db.query("SELECT COUNT(*) AS c FROM category_settings")
            if (fcCursor.next()) {
                finalCategories = fcCursor.getInt("c") ?: 0
            }
            fcCursor.close()

            var finalMakers = 0
            val fmCursor = db.query("SELECT COUNT(*) AS c FROM makers")
            if (fmCursor.next()) {
                finalMakers = fmCursor.getInt("c") ?: 0
            }
            fmCursor.close()

            var finalToys = 0
            val ftCursor = db.query("SELECT COUNT(*) AS c FROM toys")
            if (ftCursor.next()) {
                finalToys = ftCursor.getInt("c") ?: 0
            }
            ftCursor.close()

            BackupSummary(
                categories = finalCategories,
                makers = finalMakers,
                toys = finalToys,
                photos = photosExtracted,
                missingPhotos = 0
            )
        }
    }
}
