package com.gepetto.toydb.utils

import com.gepetto.toydb.database.DesktopToyDatabase
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okio.Path.Companion.toPath

class ImageResolverMainPictureTest {

    @Test
    fun testResolveMainPictureAndTimestampConsistency() {
        val tempDir = Files.createTempDirectory("toydb_img_test_").toFile()
        val originalImagesPath = ImageResolverConfig.imagesPath
        try {
            val imagesDir = File(tempDir, "images").apply { mkdirs() }
            ImageResolverConfig.imagesPath = imagesDir.absolutePath

            val dbFile = File(tempDir, "test_toydb.db")
            val db = DesktopToyDatabase(dbFile.absolutePath)
            try {
                val repo = ToyRepository(db)
                repo.setDataPathSetting(imagesDir.absolutePath)

                // Simulate copying a main picture like ToyForm does
                val srcFile = File(tempDir, "picked_photo.jpg")
                srcFile.writeBytes("fake_image_content".encodeToByteArray())

                val destPath = imagesDir.absolutePath.toPath().div("car105.jpg")
                systemFileSystem.copy(srcFile.absolutePath.toPath(), destPath)

                val meta = systemFileSystem.metadataOrNull(destPath)
                assertNotNull(meta)
                val size = meta.size ?: 0L
                val fileTimestamp = meta.lastModifiedAtMillis ?: 0L
                assertTrue(fileTimestamp > 0L, "Copied file must have a valid lastModifiedAtMillis")

                val savedToy = Toy(
                    refNum = 105,
                    toyType = "slot",
                    description = "Test Slot Car",
                    picture = "car105.jpg",
                    pictureSize = size.toInt(),
                    pictureTimeStamp = fileTimestamp,
                    hasPicture = "y"
                )
                repo.saveToy(savedToy)

                val loadedToy = repo.getToy("slot", 105)
                assertNotNull(loadedToy)
                assertEquals("car105.jpg", loadedToy.picture)
                assertEquals(fileTimestamp, loadedToy.pictureTimeStamp)

                val resolvedPath = resolveImageUri("car", loadedToy.refNum, loadedToy.picture)
                assertNotNull(resolvedPath)
                assertEquals(File(imagesDir, "car105.jpg").absolutePath, resolvedPath)

                val localFileTime = systemFileSystem.metadataOrNull(resolvedPath.toPath())?.lastModifiedAtMillis ?: 0L
                val isOutdated = loadedToy.pictureTimeStamp > 0L && localFileTime > 0L && loadedToy.pictureTimeStamp > localFileTime
                assertFalse(isOutdated, "Newly added local main picture must not be marked as outdated relative to its DB timestamp")
            } finally {
                try { db.close() } catch (_: Throwable) {}
            }
        } finally {
            ImageResolverConfig.imagesPath = originalImagesPath
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testResolveImageUriPrefersExplicitPictureName() {
        val tempDir = Files.createTempDirectory("toydb_img_explicit_test_").toFile()
        val originalImagesPath = ImageResolverConfig.imagesPath
        try {
            val imagesDir = File(tempDir, "images").apply { mkdirs() }
            ImageResolverConfig.imagesPath = imagesDir.absolutePath

            val customFile = File(imagesDir, "custom_main_pic.png")
            customFile.writeBytes("png_bytes".encodeToByteArray())

            val resolved = resolveImageUri("car", 999, "custom_main_pic.png")
            assertNotNull(resolved)
            assertEquals(customFile.absolutePath, resolved)
        } finally {
            ImageResolverConfig.imagesPath = originalImagesPath
            tempDir.deleteRecursively()
        }
    }
}
