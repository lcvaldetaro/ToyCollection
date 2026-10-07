package com.gepetto.toydb.service

import club.gepetto.GcLog
import com.gepetto.toydb.database.DesktopToyDatabase
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.platform.BackupArchive
import com.gepetto.toydb.platform.isValidPhotoName
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.sink

class BackupRestoreServiceTest {

    private fun withTestEnvironment(block: (db: DesktopToyDatabase, photosDir: File, tempDir: File) -> Unit) {
        val tempDir = Files.createTempDirectory("toydb_test_").toFile()
        val dbFile = File(tempDir, "test_toydb.db")
        val photosDir = File(tempDir, "photos").apply { mkdirs() }
        val db = DesktopToyDatabase(dbFile.absolutePath)
        try {
            ToyRepository(db).setDataPathSetting(photosDir.absolutePath)
            db.execute("DELETE FROM category_settings")
            db.execute("DELETE FROM makers")
            db.execute("DELETE FROM toys")
            db.execute("DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'")
            block(db, photosDir, tempDir)
        } finally {
            try { db.close() } catch (_: Throwable) {}
            tempDir.deleteRecursively()
        }
    }

    private fun createZip(zipFile: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for ((name, bytes) in entries) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
            zos.finish()
        }
    }

    @Test
    fun testBackupAndRestoreRoundTrip() = withTestEnvironment { db, photosDir, tempDir ->
        // Seed 2 categories
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot Cars Title", "car")
        )
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("train", "tra", "Trains", "Trains Title", "train")
        )

        // Seed 3 makers (one with bitmaps)
        db.execute("INSERT INTO makers (name, country, bitmaps, comments) VALUES (?, ?, ?, ?)", listOf("Scalextric", "UK", "maker1.jpg maker2.jpg", "Slot car maker"))
        db.execute("INSERT INTO makers (name, country, bitmaps, comments) VALUES (?, ?, ?, ?)", listOf("Hornby", "UK", "", "Train maker"))
        db.execute("INSERT INTO makers (name, country, bitmaps, comments) VALUES (?, ?, ?, ?)", listOf("Fly", "Spain", "", "Fly slot cars"))

        // Seed 5 toys
        db.execute(
            """
            INSERT INTO toys (ref_num, toy_type, description, picture, bitmaps, value, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(101, "slot", "Porsche 911", "car101.jpg", "", 45.50, 30.00)
        )
        db.execute(
            """
            INSERT INTO toys (ref_num, toy_type, description, picture, bitmaps, value, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(102, "slot", "Ferrari 250", "", "", 55.00, 40.00)
        )
        db.execute(
            """
            INSERT INTO toys (ref_num, toy_type, description, picture, bitmaps, value, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(103, "slot", "Ford GT", "", "", 35.00, 20.00)
        )
        db.execute(
            """
            INSERT INTO toys (ref_num, toy_type, description, picture, bitmaps, value, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(201, "train", "Flying Scotsman", "tra201.jpg", "", 120.00, 95.00)
        )
        db.execute(
            """
            INSERT INTO toys (ref_num, toy_type, description, picture, bitmaps, value, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(202, "train", "Mallard", "", "", 110.00, 80.00)
        )

        // Create 4 photo files with distinct non-zero millis timestamp
        val originalTimestamp = 1_700_000_123_457L
        val photoNames = listOf("maker1.jpg", "maker2.jpg", "car101.jpg", "tra201.jpg")
        for (name in photoNames) {
            val f = File(photosDir, name)
            f.writeBytes("photo_bytes_for_$name".encodeToByteArray())
            f.setLastModified(originalTimestamp)
        }

        val zipFile = File(tempDir, "roundtrip.zip")
        val content = runBlocking { BackupRestoreService.prepareBackup(db) }
        assertEquals(2, content.summary.categories)
        assertEquals(3, content.summary.makers)
        assertEquals(5, content.summary.toys)
        assertEquals(4, content.summary.photos)
        assertEquals(0, content.summary.missingPhotos)

        zipFile.sink().use {
            BackupRestoreService.writeBackup(content, it) { _, _ -> }
        }

        // Clear local database and photo directory
        db.execute("DELETE FROM toys")
        db.execute("DELETE FROM makers")
        db.execute("DELETE FROM category_settings")
        photosDir.listFiles()?.forEach { it.delete() }

        // Restore
        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        val restoreSummary = runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
        }

        assertEquals(2, restoreSummary.categories)
        assertEquals(3, restoreSummary.makers)
        assertEquals(5, restoreSummary.toys)
        assertEquals(4, restoreSummary.photos)

        // Verify photo files
        for (name in photoNames) {
            val restoredFile = File(photosDir, name)
            assertTrue(restoredFile.exists(), "Restored photo $name must exist")
            assertEquals("photo_bytes_for_$name", restoredFile.readText())
            assertEquals(originalTimestamp, restoredFile.lastModified(), "Photo $name timestamp must match original exactly")
        }
    }

    @Test
    fun testSecretsAreNotBackedUp() = withTestEnvironment { db, _, tempDir ->
        val customData = File(tempDir, "custom_data").apply { mkdirs() }
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('sftp_password', 'secret123')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('sftp_host', 'example.com')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('images_path', '/custom/images')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('data_path', '${customData.absolutePath}')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('import_export_path', '/custom/export')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_hash_slot', 'abc123hash')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('app_title', 'My Toy Collection')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('theme', '1')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('base_url', 'https://example.com/toydb/')")

        val zipFile = File(tempDir, "secrets_test.zip")
        val content = runBlocking { BackupRestoreService.prepareBackup(db) }
        zipFile.sink().use {
            BackupRestoreService.writeBackup(content, it) { _, _ -> }
        }

        val entries = BackupArchive.readTextEntries(zipFile.toOkioPath())
        val appSettingsJson = entries["data/app_settings.json"]
        assertNotNull(appSettingsJson)

        val parsed = backupJson.decodeFromString<JsonAppSettingsFile>(appSettingsJson)
        val keys = parsed.settings.map { it.key }.toSet()

        assertFalse("sftp_password" in keys)
        assertFalse("sftp_host" in keys)
        assertFalse("images_path" in keys)
        assertFalse("data_path" in keys)
        assertFalse("import_export_path" in keys)
        assertFalse("html_sync_imported_hash_slot" in keys)

        assertTrue("app_title" in keys)
        assertTrue("theme" in keys)
        assertTrue("base_url" in keys)
    }

    @Test
    fun testRestoreDoesNotOverwriteLocalOnlySettings() = withTestEnvironment { db, photosDir, tempDir ->
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('sftp_password', 'local_password')")

        // Create a backup with foreign settings
        val appSettings = JsonAppSettingsFile(
            settings = listOf(
                JsonAppSetting("sftp_password", "foreign_password"),
                JsonAppSetting("data_path", "/foreign/data"),
                JsonAppSetting("images_path", "/foreign/images"),
                JsonAppSetting("app_title", "Restored Title")
            )
        )
        val manifest = BackupManifest(
            format = BACKUP_FORMAT_ID,
            formatVersion = BACKUP_FORMAT_VERSION,
            createdAt = "October 7, 2026",
            appVersionCode = "100",
            categories = 0,
            makers = 0,
            toys = 0,
            photos = 0
        )
        val catSettings = JsonCategorySettingsFile(settings = emptyList())
        val makers = JsonMakersFile(makers = emptyList())
        val photoIndex = BackupPhotoIndex(photos = emptyList())

        val zipFile = File(tempDir, "foreign.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to backupJson.encodeToString(BackupPhotoIndex.serializer(), photoIndex).encodeToByteArray(),
                "data/category_settings.json" to backupJson.encodeToString(JsonCategorySettingsFile.serializer(), catSettings).encodeToByteArray(),
                "data/carmaker.json" to backupJson.encodeToString(JsonMakersFile.serializer(), makers).encodeToByteArray(),
                "data/app_settings.json" to backupJson.encodeToString(JsonAppSettingsFile.serializer(), appSettings).encodeToByteArray()
            )
        )

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
        }

        val repo = ToyRepository(db)
        assertEquals("local_password", repo.getSftpPasswordSetting())
        assertEquals(photosDir.absolutePath, repo.getDataPathSetting())
        assertEquals(photosDir.absolutePath, repo.getImagesPathSetting())
        assertEquals("Restored Title", repo.getAppTitleSetting())
    }

    @Test
    fun testInvalidBackupChangesNothing() = withTestEnvironment { db, photosDir, tempDir ->
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot Cars", "car")
        )

        // 1. Missing manifest.json
        val noManifestZip = File(tempDir, "no_manifest.zip")
        createZip(noManifestZip, mapOf("photos.json" to "{}".encodeToByteArray()))
        assertFailsWith<BackupRestoreService.InvalidBackupException> {
            BackupRestoreService.readBackup(noManifestZip.toOkioPath())
        }

        // 2. Broken carmaker.json
        val brokenMakersZip = File(tempDir, "broken_makers.zip")
        val manifest = BackupManifest(BACKUP_FORMAT_ID, BACKUP_FORMAT_VERSION, "October 7, 2026", "1", 0, 0, 0, 0)
        createZip(
            brokenMakersZip,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to "{\"photos\":[]}".encodeToByteArray(),
                "data/category_settings.json" to "{\"settings\":[]}".encodeToByteArray(),
                "data/carmaker.json" to "{ not json".encodeToByteArray()
            )
        )
        assertFailsWith<BackupRestoreService.InvalidBackupException> {
            BackupRestoreService.readBackup(brokenMakersZip.toOkioPath())
        }

        // 3. Plain text file named backup.zip
        val plainTextZip = File(tempDir, "plain_text.zip")
        plainTextZip.writeText("This is not a zip file.")
        assertFailsWith<BackupRestoreService.InvalidBackupException> {
            BackupRestoreService.readBackup(plainTextZip.toOkioPath())
        }

        // Assert DB was unchanged
        val cursor = db.query("SELECT COUNT(*) AS c FROM category_settings")
        assertTrue(cursor.next())
        assertEquals(1, cursor.getInt("c"))
        cursor.close()
    }

    @Test
    fun testPhotosJsonRequired() = withTestEnvironment { _, _, tempDir ->
        val manifest = BackupManifest(BACKUP_FORMAT_ID, BACKUP_FORMAT_VERSION, "October 7, 2026", "1", 0, 0, 0, 0)
        val zipFile = File(tempDir, "no_photos_json.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "data/category_settings.json" to "{\"settings\":[]}".encodeToByteArray(),
                "data/carmaker.json" to "{\"makers\":[]}".encodeToByteArray()
            )
        )
        assertFailsWith<BackupRestoreService.InvalidBackupException> {
            BackupRestoreService.readBackup(zipFile.toOkioPath())
        }
    }

    @Test
    fun testUnsafeEntryNamesAreSkipped() = withTestEnvironment { db, photosDir, tempDir ->
        val manifest = BackupManifest(BACKUP_FORMAT_ID, BACKUP_FORMAT_VERSION, "October 7, 2026", "1", 0, 0, 0, 1)
        val photoIndex = BackupPhotoIndex(
            photos = listOf(
                BackupPhotoInfo("good.jpg", 4, 1000L),
                BackupPhotoInfo("../evil.jpg", 4, 1000L),
                BackupPhotoInfo("a/b.jpg", 4, 1000L),
                BackupPhotoInfo("C:evil.jpg", 4, 1000L),
                BackupPhotoInfo("a\u0001.jpg", 4, 1000L)
            )
        )
        val zipFile = File(tempDir, "unsafe_entries.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to backupJson.encodeToString(BackupPhotoIndex.serializer(), photoIndex).encodeToByteArray(),
                "data/category_settings.json" to "{\"settings\":[]}".encodeToByteArray(),
                "data/carmaker.json" to "{\"makers\":[]}".encodeToByteArray(),
                "images/good.jpg" to "good".encodeToByteArray(),
                "images/../evil.jpg" to "evil".encodeToByteArray(),
                "images/a/b.jpg" to "evil".encodeToByteArray(),
                "../x.jpg" to "evil".encodeToByteArray(),
                "images/C:evil.jpg" to "evil".encodeToByteArray(),
                "images/a\u0001.jpg" to "evil".encodeToByteArray()
            )
        )

        assertFalse(isValidPhotoName("../evil.jpg"))
        assertFalse(isValidPhotoName("a/b.jpg"))
        assertFalse(isValidPhotoName("C:evil.jpg"))
        assertFalse(isValidPhotoName("a\u0001.jpg"))
        assertTrue(isValidPhotoName("good.jpg"))

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        val summary = runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
        }

        assertEquals(1, summary.photos)
        assertTrue(File(photosDir, "good.jpg").exists())
        assertFalse(File(photosDir, "evil.jpg").exists())
        assertFalse(File(tempDir, "evil.jpg").exists())
        assertFalse(File(tempDir, "x.jpg").exists())
        assertFalse(File(photosDir, "b.jpg").exists())
    }

    @Test
    fun testFailedRestoreRollsBack() = withTestEnvironment { db, photosDir, tempDir ->
        // Seed initial data
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot Cars", "car")
        )
        db.execute("INSERT INTO makers (name, country) VALUES (?, ?)", listOf("Fly", "Spain"))
        db.execute("INSERT INTO toys (ref_num, toy_type, description) VALUES (?, ?, ?)", listOf(1, "slot", "Original Porsche"))
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_date_slot', 'original_date')")

        // Create backup with a new toy
        val backupToys = JsonToysFile(
            cars = listOf(
                JsonToy(refNum = "999", description = "New Toy That Triggers Error")
            )
        )
        val manifest = BackupManifest(
            format = BACKUP_FORMAT_ID,
            formatVersion = BACKUP_FORMAT_VERSION,
            createdAt = "October 7, 2026",
            appVersionCode = "1",
            categories = 1,
            makers = 1,
            toys = 1,
            photos = 0,
            categoryFiles = mapOf("slot" to "data/carlist.json")
        )
        val catSettings = JsonCategorySettingsFile(
            settings = listOf(JsonCategorySetting("slot", "car", "Slot Cars"))
        )
        val makers = JsonMakersFile(makers = listOf(JsonMaker("NewMaker", "UK")))
        val photoIndex = BackupPhotoIndex(photos = emptyList())

        val zipFile = File(tempDir, "error_test.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to backupJson.encodeToString(BackupPhotoIndex.serializer(), photoIndex).encodeToByteArray(),
                "data/category_settings.json" to backupJson.encodeToString(JsonCategorySettingsFile.serializer(), catSettings).encodeToByteArray(),
                "data/carmaker.json" to backupJson.encodeToString(JsonMakersFile.serializer(), makers).encodeToByteArray(),
                "data/carlist.json" to backupJson.encodeToString(JsonToysFile.serializer(), backupToys).encodeToByteArray()
            )
        )

        class ThrowingToyDatabase(private val delegate: DesktopToyDatabase) : ToyDatabase by delegate {
            override fun <T> transaction(block: () -> T): T = delegate.transaction(block)
            override fun execute(sql: String, bindArgs: List<Any?>) {
                if (sql.contains("INSERT OR REPLACE INTO toys", ignoreCase = true)) {
                    throw RuntimeException("Simulated error inserting toys")
                }
                delegate.execute(sql, bindArgs)
            }
        }

        val throwingDb = ThrowingToyDatabase(db)
        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())

        assertFailsWith<RuntimeException> {
            runBlocking {
                BackupRestoreService.restoreBackup(throwingDb, validated, photosDir.toOkioPath()) { _, _ -> }
            }
        }

        // Verify DB rolled back completely to original state
        val catCursor = db.query("SELECT category FROM category_settings")
        assertTrue(catCursor.next())
        assertEquals("slot", catCursor.getString("category"))
        catCursor.close()

        val makerCursor = db.query("SELECT name FROM makers")
        assertTrue(makerCursor.next())
        assertEquals("Fly", makerCursor.getString("name"))
        makerCursor.close()

        val toyCursor = db.query("SELECT ref_num, description FROM toys")
        assertTrue(toyCursor.next())
        assertEquals(1, toyCursor.getInt("ref_num"))
        assertEquals("Original Porsche", toyCursor.getString("description"))
        toyCursor.close()

        val markerCursor = db.query("SELECT value FROM app_settings WHERE key = 'html_sync_imported_date_slot'")
        assertTrue(markerCursor.next())
        assertEquals("original_date", markerCursor.getString("value"))
        markerCursor.close()
    }

    @Test
    fun testMissingPhotoIsSkipped() = withTestEnvironment { db, _, _ ->
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot Cars", "car")
        )
        db.execute(
            "INSERT INTO toys (ref_num, toy_type, description, picture) VALUES (?, ?, ?, ?)",
            listOf(1, "slot", "Car with missing photo", "non_existent_car1.jpg")
        )

        val content = runBlocking { BackupRestoreService.prepareBackup(db) }
        assertEquals(1, content.summary.missingPhotos)
        assertEquals(0, content.photos.size)
    }

    @Test
    fun testRestoreSetsSyncMarkers() = withTestEnvironment { db, photosDir, tempDir ->
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_date_slot', 'old_date')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_hash_slot', 'old_hash')")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_date_obsolete', 'obsolete')")

        val catSettings = JsonCategorySettingsFile(
            date = "October 7, 2026",
            settings = listOf(JsonCategorySetting("slot", "car", "Slot Cars"))
        )
        val makers = JsonMakersFile(
            date = "October 6, 2026",
            makers = listOf(JsonMaker("Fly", "Spain"))
        )
        val toys = JsonToysFile(
            date = "October 5, 2026",
            cars = listOf(JsonToy("1", description = "Test Toy"))
        )
        val manifest = BackupManifest(
            format = BACKUP_FORMAT_ID,
            formatVersion = BACKUP_FORMAT_VERSION,
            createdAt = "October 7, 2026",
            appVersionCode = "1",
            categories = 1,
            makers = 1,
            toys = 1,
            photos = 0,
            categoryFiles = mapOf("slot" to "data/carlist.json")
        )
        val photoIndex = BackupPhotoIndex(photos = emptyList())

        val catText = backupJson.encodeToString(JsonCategorySettingsFile.serializer(), catSettings)
        val makersText = backupJson.encodeToString(JsonMakersFile.serializer(), makers)
        val toysText = backupJson.encodeToString(JsonToysFile.serializer(), toys)

        val zipFile = File(tempDir, "markers_test.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to backupJson.encodeToString(BackupPhotoIndex.serializer(), photoIndex).encodeToByteArray(),
                "data/category_settings.json" to catText.encodeToByteArray(),
                "data/carmaker.json" to makersText.encodeToByteArray(),
                "data/carlist.json" to toysText.encodeToByteArray()
            )
        )

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
        }

        fun getSetting(k: String): String? {
            val c = db.query("SELECT value FROM app_settings WHERE key = ?", listOf(k))
            val v = if (c.next()) c.getString("value") else null
            c.close()
            return v
        }

        assertEquals("October 7, 2026", getSetting("html_sync_imported_date_category_settings.json"))
        assertEquals(HtmlSyncService.calculateHash(catText), getSetting("html_sync_imported_hash_category_settings.json"))

        assertEquals("October 6, 2026", getSetting("html_sync_imported_date_makers"))
        assertEquals(HtmlSyncService.calculateHash(makersText), getSetting("html_sync_imported_hash_makers"))

        assertEquals("October 5, 2026", getSetting("html_sync_imported_date_slot"))
        assertEquals(HtmlSyncService.calculateHash(toysText), getSetting("html_sync_imported_hash_slot"))

        assertNull(getSetting("html_sync_imported_date_obsolete"))
    }

    @Test
    fun testRestoreWaitsForLock() = withTestEnvironment { db, photosDir, tempDir ->
        val manifest = BackupManifest(BACKUP_FORMAT_ID, BACKUP_FORMAT_VERSION, "October 7, 2026", "1", 1, 0, 0, 0)
        val catSettings = JsonCategorySettingsFile(settings = listOf(JsonCategorySetting("slot", "car", "Slot Cars")))
        val zipFile = File(tempDir, "lock_test.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to "{\"photos\":[]}".encodeToByteArray(),
                "data/category_settings.json" to backupJson.encodeToString(JsonCategorySettingsFile.serializer(), catSettings).encodeToByteArray(),
                "data/carmaker.json" to "{\"makers\":[]}".encodeToByteArray()
            )
        )

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())

        runBlocking {
            CollectionWriteLock.mutex.lock()
            var completed = false
            val job = launch(Dispatchers.IO) {
                BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
                completed = true
            }

            delay(100)
            assertFalse(completed, "Restore should be waiting for CollectionWriteLock")

            // Categories should not be restored yet
            val c = db.query("SELECT COUNT(*) AS c FROM category_settings")
            assertTrue(c.next())
            assertEquals(0, c.getInt("c"))
            c.close()

            CollectionWriteLock.mutex.unlock()
            job.join()
            assertTrue(completed, "Restore should complete after lock is released")

            val c2 = db.query("SELECT COUNT(*) AS c FROM category_settings")
            assertTrue(c2.next())
            assertEquals(1, c2.getInt("c"))
            c2.close()
        }
    }

    @Test
    fun testManifestWithoutFormatIsRejected() = withTestEnvironment { db, _, tempDir ->
        // (a) manifest from collectBackupContent contains format and formatVersion
        val content = BackupRestoreService.collectBackupContent(db)
        val manifestJson = content.textEntries["manifest.json"]
        assertNotNull(manifestJson)
        assertTrue(manifestJson.contains("\"format\": \"$BACKUP_FORMAT_ID\""))
        assertTrue(manifestJson.contains("\"formatVersion\": $BACKUP_FORMAT_VERSION"))

        // (b) Zip whose manifest has no format
        val zipFile = File(tempDir, "missing_format.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to "{\"categories\":0,\"makers\":0,\"toys\":0,\"photos\":0}".encodeToByteArray(),
                "photos.json" to "{\"photos\":[]}".encodeToByteArray(),
                "data/category_settings.json" to "{\"settings\":[]}".encodeToByteArray(),
                "data/carmaker.json" to "{\"makers\":[]}".encodeToByteArray()
            )
        )
        assertFailsWith<BackupRestoreService.InvalidBackupException> {
            BackupRestoreService.readBackup(zipFile.toOkioPath())
        }
    }

    @Test
    fun testCategoriesWithSamePrefix() = withTestEnvironment { db, photosDir, tempDir ->
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot", "car")
        )
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("racing", "car", "Racing Cars", "Racing", "car")
        )

        db.execute("INSERT INTO toys (ref_num, toy_type, description) VALUES (?, ?, ?)", listOf(1, "slot", "Slot Porsche"))
        db.execute("INSERT INTO toys (ref_num, toy_type, description) VALUES (?, ?, ?)", listOf(2, "racing", "Racing Ferrari"))

        val content = BackupRestoreService.collectBackupContent(db)
        val manifest = backupJson.decodeFromString<BackupManifest>(content.textEntries["manifest.json"]!!)

        assertEquals("data/carlist.json", manifest.categoryFiles["racing"])
        assertEquals("data/slotlist.json", manifest.categoryFiles["slot"])
        assertTrue("data/carlist.json" in content.textEntries)
        assertTrue("data/slotlist.json" in content.textEntries)

        val zipFile = File(tempDir, "duplicate_prefix.zip")
        zipFile.sink().use {
            BackupRestoreService.writeBackup(content, it) { _, _ -> }
        }

        db.execute("DELETE FROM toys")
        db.execute("DELETE FROM category_settings")

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.toOkioPath()) { _, _ -> }
        }

        val slotToy = db.query("SELECT description FROM toys WHERE toy_type = 'slot'")
        assertTrue(slotToy.next())
        assertEquals("Slot Porsche", slotToy.getString("description"))
        slotToy.close()

        val racingToy = db.query("SELECT description FROM toys WHERE toy_type = 'racing'")
        assertTrue(racingToy.next())
        assertEquals("Racing Ferrari", racingToy.getString("description"))
        racingToy.close()
    }

    @Test
    fun testCaseOnlyDuplicatePhotoStoredOnce() = withTestEnvironment { db, photosDir, tempDir ->
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot", "car")
        )
        db.execute(
            "INSERT INTO toys (ref_num, toy_type, description, picture) VALUES (?, ?, ?, ?)",
            listOf(12, "slot", "Porsche 911", "car12.jpg")
        )

        // Create only car12.JPG on disk
        val diskFile = File(photosDir, "car12.JPG")
        diskFile.writeBytes("image_data".encodeToByteArray())

        val content = BackupRestoreService.collectBackupContent(db)
        assertEquals(0, content.summary.missingPhotos)
        assertEquals(1, content.photos.size)
        assertEquals("images/car12.jpg", content.photos[0].entryName)

        val zipFile = File(tempDir, "case_test.zip")
        zipFile.sink().use {
            BackupRestoreService.writeBackup(content, it) { _, _ -> }
        }

        val entries = BackupArchive.readTextEntries(zipFile.toOkioPath())
        assertNotNull(entries["manifest.json"])
    }

    @Test
    fun testRestorePathCheckIgnoresTrailingSlash() = withTestEnvironment { db, photosDir, tempDir ->
        val manifest = BackupManifest(BACKUP_FORMAT_ID, BACKUP_FORMAT_VERSION, "October 7, 2026", "1", 0, 0, 0, 0)
        val zipFile = File(tempDir, "slash_test.zip")
        createZip(
            zipFile,
            mapOf(
                "manifest.json" to backupJson.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray(),
                "photos.json" to "{\"photos\":[]}".encodeToByteArray(),
                "data/category_settings.json" to "{\"settings\":[]}".encodeToByteArray(),
                "data/carmaker.json" to "{\"makers\":[]}".encodeToByteArray()
            )
        )

        // Store data_path with trailing slash
        ToyRepository(db).setDataPathSetting(photosDir.absolutePath + "/")

        val validated = BackupRestoreService.readBackup(zipFile.toOkioPath())
        // Call restoreBackup with path without trailing slash
        runBlocking {
            BackupRestoreService.restoreBackup(db, validated, photosDir.absolutePath.toPath()) { _, _ -> }
        }
    }

    @Test
    fun testBackupWithoutDataFolderFails() = withTestEnvironment { db, _, _ ->
        ToyRepository(db).setDataPathSetting("/non/existent/path/12345/data")
        assertFailsWith<IllegalStateException> {
            BackupRestoreService.collectBackupContent(db)
        }
    }

    @Test
    fun testBackupWaitsForLock() = withTestEnvironment { db, _, _ ->
        runBlocking {
            CollectionWriteLock.mutex.lock()
            var completed = false
            val job = launch(Dispatchers.IO) {
                BackupRestoreService.prepareBackup(db)
                completed = true
            }

            delay(100)
            assertFalse(completed, "prepareBackup should be waiting for CollectionWriteLock")

            CollectionWriteLock.mutex.unlock()
            job.join()
            assertTrue(completed, "prepareBackup should complete after lock is released")
        }
    }
}
