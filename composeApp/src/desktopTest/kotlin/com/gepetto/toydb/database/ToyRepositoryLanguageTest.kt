package com.gepetto.toydb.database

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ToyRepositoryLanguageTest {

    private fun withTestDb(block: (repository: ToyRepository) -> Unit) {
        val tempDir = Files.createTempDirectory("toydb_repo_lang_test_").toFile()
        val dbFile = File(tempDir, "test_toydb.db")
        val db = DesktopToyDatabase(dbFile.absolutePath)
        try {
            val repository = ToyRepository(db)
            block(repository)
        } finally {
            try { db.close() } catch (_: Throwable) {}
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLanguageSettingDefaultIsEmpty() = withTestDb { repo ->
        assertEquals("", repo.getLanguageSetting(), "Default language setting should be empty string (system default)")
    }

    @Test
    fun testLanguageSettingSaveAndRetrieve() = withTestDb { repo ->
        repo.setLanguageSetting("pt")
        assertEquals("pt", repo.getLanguageSetting(), "Language setting should persist and return 'pt'")

        repo.setLanguageSetting("de")
        assertEquals("de", repo.getLanguageSetting(), "Language setting should update to 'de'")

        repo.setLanguageSetting("")
        assertEquals("", repo.getLanguageSetting(), "Language setting should reset to empty string (system default)")
    }
}
