package com.gepetto.toydb.service

import com.gepetto.toydb.database.DesktopToyDatabase
import com.gepetto.toydb.database.ToyRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class DatabaseResetServiceTest {

    private fun withTestEnvironment(block: (db: DesktopToyDatabase, photosDir: File, tempDir: File) -> Unit) {
        val tempDir = Files.createTempDirectory("toydb_reset_test_").toFile()
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

    @Test
    fun testResetDatabaseSuccessAndSharedImageProtection() = withTestEnvironment { db, photosDir, tempDir ->
        val repo = ToyRepository(db)

        // 1. Seed categories
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("slot", "car", "Slot Cars", "Slot car", "car")
        )
        db.execute(
            "INSERT INTO category_settings (category, image_prefix, label, title, icon) VALUES (?, ?, ?, ?, ?)",
            listOf("train", "tra", "Model Trains", "Train", "train")
        )

        // 2. Seed makers (Fly has bitmaps 'fly_logo.png', Scalextric has 'scx_logo.jpg')
        db.execute(
            "INSERT INTO makers (name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments) VALUES (?, ?, ?, ?, ?, ?)",
            listOf("Fly", "Spain", "fly_logo.png", "1024", "1000", "Slot car maker")
        )
        db.execute(
            "INSERT INTO makers (name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments) VALUES (?, ?, ?, ?, ?, ?)",
            listOf("Scalextric", "UK", "scx_logo.jpg", "2048", "2000", "Classic brand")
        )

        // 3. Seed toys:
        // Toy 1 uses 'fly_logo.png' in bitmaps (shared with maker Fly!) and 'car101.jpg' (exclusive to toy)
        db.execute(
            """
            INSERT INTO toys (
                ref_num, toy_type, description, maker_combo, scale, factory_car,
                body_maker, acquired, chassis_type, chassis_maker, condition, color,
                motor_maker, motor_details, catalog_number, comments, major_work,
                minor_work, repro, value, amount_paid, amount_sold, traded, buy,
                maintenance, to_make, detail, boxed, picture, picture_size,
                picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp,
                year_made, number, my_comments
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(
                101, "slot", "Porsche 917", "Fly", "1/32", "y",
                "Fly", "2020", "Standard", "Fly", "Mint", "White",
                "Mabuchi", "12V", "C-01", "", "",
                "", "n", 50.0, 40.0, "", "", "",
                "", "", "", "n", "car101.jpg", 1024,
                1000L, "y", "car101_side.jpg fly_logo.png", "1024 1024", "1000 1000",
                "1970", "23", ""
            )
        )

        // Toy 2 (train) has picture 'tra102.png'
        db.execute(
            """
            INSERT INTO toys (
                ref_num, toy_type, description, maker_combo, scale, factory_car,
                body_maker, acquired, chassis_type, chassis_maker, condition, color,
                motor_maker, motor_details, catalog_number, comments, major_work,
                minor_work, repro, value, amount_paid, amount_sold, traded, buy,
                maintenance, to_make, detail, boxed, picture, picture_size,
                picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp,
                year_made, number, my_comments
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(
                102, "train", "Steam Locomotive", "Hornby", "HO", "y",
                "Hornby", "2018", "", "", "Good", "Black",
                "", "", "H-02", "", "",
                "", "n", 80.0, 70.0, "", "", "",
                "", "", "", "n", "tra102.png", 2048,
                2000L, "y", "", "", "",
                "1955", "44", ""
            )
        )

        // 4. Seed app_settings
        repo.setBaseUrlSetting("https://gepetto.club/database/")
        repo.setAppTitleSetting("Gepetto Toy Database Manager")
        db.execute("INSERT OR REPLACE INTO app_settings (key, value) VALUES ('html_sync_imported_hash_cat', 'xyz999')")
        repo.setThemeSetting(1)

        // 5. Create files in photosDir
        val toyExclusiveFile1 = File(photosDir, "car101.jpg").apply { writeText("photo1") }
        val toyExclusiveFile2 = File(photosDir, "car101_side.jpg").apply { writeText("photo2") }
        val toyExclusiveFile3 = File(photosDir, "tra102.png").apply { writeText("photo3") }
        val sharedFile = File(photosDir, "fly_logo.png").apply { writeText("shared_logo") }
        val makerExclusiveFile = File(photosDir, "scx_logo.jpg").apply { writeText("maker_logo") }

        assertTrue(toyExclusiveFile1.exists())
        assertTrue(toyExclusiveFile2.exists())
        assertTrue(toyExclusiveFile3.exists())
        assertTrue(sharedFile.exists())
        assertTrue(makerExclusiveFile.exists())

        // 6. Execute reset
        val result = runBlocking {
            DatabaseResetService.resetDatabaseToDefaults(db, null)
        }
        assertTrue(result.isSuccess, "Reset should succeed: ${result.exceptionOrNull()?.message}")

        // 7. Verify database state
        // Toys table must have 0 records
        val toyCursor = db.query("SELECT COUNT(*) as c FROM toys")
        assertTrue(toyCursor.next())
        assertEquals(0, toyCursor.getInt("c"), "Toys must be 0 after reset")
        toyCursor.close()

        // Makers table must NOT be deleted (remains 2 makers)
        val makerCursor = db.query("SELECT COUNT(*) as c FROM makers")
        assertTrue(makerCursor.next())
        assertEquals(2, makerCursor.getInt("c"), "Makers must be kept intact after reset")
        makerCursor.close()

        // Standard categories must be present (5 standard categories)
        val catCursor = db.query("SELECT COUNT(*) as c FROM category_settings")
        assertTrue(catCursor.next())
        assertEquals(5, catCursor.getInt("c"), "Category settings must have 5 standard categories")
        catCursor.close()

        // base_url must be wiped out to empty string
        assertEquals("", repo.getBaseUrlSetting(), "base_url must be empty string after reset")

        // app_title must be initialized to 'My Toy Collection'
        assertEquals("My Toy Collection", repo.getAppTitleSetting(), "app_title must be 'My Toy Collection'")

        // Sync metadata must be wiped out
        val syncCursor = db.query("SELECT COUNT(*) as c FROM app_settings WHERE key LIKE 'html_sync_imported_%'")
        assertTrue(syncCursor.next())
        assertEquals(0, syncCursor.getInt("c"), "Sync metadata must be deleted")
        syncCursor.close()

        // Theme and data_path must be preserved
        assertEquals(1, repo.getThemeSetting(), "User theme setting must be preserved")
        assertEquals(photosDir.absolutePath, repo.getDataPathSetting(), "User data_path setting must be preserved")

        // 8. Verify photo retention & safe purging
        assertFalse(toyExclusiveFile1.exists(), "Exclusive toy photo 1 must be deleted")
        assertFalse(toyExclusiveFile2.exists(), "Exclusive toy photo 2 must be deleted")
        assertFalse(toyExclusiveFile3.exists(), "Exclusive toy photo 3 must be deleted")
        assertTrue(sharedFile.exists(), "Shared photo (used by maker) MUST be preserved")
        assertTrue(makerExclusiveFile.exists(), "Maker photo MUST be preserved")
    }

    @Test
    fun testBaseUrlEmptyStringBehavior() = withTestEnvironment { db, _, _ ->
        val repo = ToyRepository(db)
        // Explicitly set to empty string
        repo.setBaseUrlSetting("")
        assertEquals("", repo.getBaseUrlSetting(), "Explicit empty base_url should return empty string, not default URL")

        // Explicitly set to null (key removed)
        db.execute("DELETE FROM app_settings WHERE key = 'base_url'")
        assertEquals("https://gepetto.club/database/", repo.getBaseUrlSetting(), "When key is missing, should return default URL")
    }
}
