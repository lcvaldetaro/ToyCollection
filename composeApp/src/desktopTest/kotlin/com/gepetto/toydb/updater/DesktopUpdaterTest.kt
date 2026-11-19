package com.gepetto.toydb.updater

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopUpdaterTest {

    private val sampleVersionJson = """
        {
          "versionName": "3.0.38",
          "versionCodeMac": 2384,
          "versionCodeWindows": 2385,
          "urlMacIntel": "https://gepetto.club/database/toydatabasemanager-intel.dmg",
          "urlMacM1": "https://gepetto.club/database/toydatabasemanager-m1.dmg",
          "urlWindows": "https://gepetto.club/database/toydatabasemanager.msi"
        }
    """.trimIndent()

    @Test
    fun testUpdateInfoIsNewerThan() {
        val info = UpdateInfo(
            versionName = "3.0.39",
            versionCode = 2394L,
            downloadUrl = "https://gepetto.club/database/toydatabasemanager-m1.dmg"
        )
        assertTrue(info.isNewerThan(2384L))
        assertFalse(info.isNewerThan(2394L))
        assertFalse(info.isNewerThan(2400L))
    }

    @Test
    fun testParseUpdateInfoForMacM1() {
        val info = DesktopUpdater.parseUpdateInfo(
            jsonText = sampleVersionJson,
            isMac = true,
            isWindows = false,
            isMacM1 = true
        )
        assertNotNull(info)
        assertEquals("3.0.38", info.versionName)
        assertEquals(2384L, info.versionCode)
        assertEquals("https://gepetto.club/database/toydatabasemanager-m1.dmg", info.downloadUrl)
    }

    @Test
    fun testParseUpdateInfoForMacIntel() {
        val info = DesktopUpdater.parseUpdateInfo(
            jsonText = sampleVersionJson,
            isMac = true,
            isWindows = false,
            isMacM1 = false
        )
        assertNotNull(info)
        assertEquals("3.0.38", info.versionName)
        assertEquals(2384L, info.versionCode)
        assertEquals("https://gepetto.club/database/toydatabasemanager-intel.dmg", info.downloadUrl)
    }

    @Test
    fun testParseUpdateInfoForWindows() {
        val info = DesktopUpdater.parseUpdateInfo(
            jsonText = sampleVersionJson,
            isMac = false,
            isWindows = true,
            isMacM1 = false
        )
        assertNotNull(info)
        assertEquals("3.0.38", info.versionName)
        assertEquals(2385L, info.versionCode)
        assertEquals("https://gepetto.club/database/toydatabasemanager.msi", info.downloadUrl)
    }

    @Test
    fun testParseUpdateInfoInvalidJson() {
        val info = DesktopUpdater.parseUpdateInfo(
            jsonText = "{ not valid json }",
            isMac = true,
            isWindows = false,
            isMacM1 = true
        )
        assertNull(info)
    }

    @Test
    fun testResolveInstallerFileName() {
        assertEquals(
            "toydatabasemanager-m1.dmg",
            DesktopUpdater.resolveInstallerFileName("https://gepetto.club/database/toydatabasemanager-m1.dmg")
        )
        assertEquals(
            "toydatabasemanager-intel.dmg",
            DesktopUpdater.resolveInstallerFileName("https://gepetto.club/database/toydatabasemanager-intel.dmg?v=123")
        )
        assertEquals(
            "toydatabasemanager.msi",
            DesktopUpdater.resolveInstallerFileName("https://gepetto.club/database/toydatabasemanager.msi")
        )
    }

    @Test
    fun testDownloadUpdateFileWithoutQuarantine() {
        val payload = "mock-toydb-dmg-binary-payload-content".repeat(100).toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/database/toydatabasemanager-m1.dmg") { exchange ->
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()

        val tempDir = Files.createTempDirectory("toydb_desktop_updater_test").toFile()
        try {
            val port = server.address.port
            val url = "http://127.0.0.1:$port/database/toydatabasemanager-m1.dmg"
            val progressValues = mutableListOf<Float?>()

            val downloadedFile = DesktopUpdater.downloadUpdateFile(
                downloadUrl = url,
                targetDir = tempDir,
                onProgress = { progressValues.add(it) }
            )

            assertNotNull(downloadedFile)
            assertTrue(downloadedFile.exists())
            assertEquals("toydatabasemanager-m1.dmg", downloadedFile.name)
            assertTrue(payload.contentEquals(downloadedFile.readBytes()))
            assertTrue(progressValues.isNotEmpty())
            assertEquals(1f, progressValues.last())

            if (DesktopUpdater.isMac()) {
                val proc = ProcessBuilder("xattr", downloadedFile.absolutePath).start()
                val attrs = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                assertFalse(attrs.contains("com.apple.quarantine"))
            }
        } finally {
            server.stop(0)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testParseUpdateInfoWebsiteFileMatchesSchema() {
        val possibleWebsiteFiles = listOf(
            File("website/version.json"),
            File("../website/version.json"),
            File("ToyCollection/website/version.json"),
            File("../../website/version.json")
        )
        val websiteFile = possibleWebsiteFiles.find { it.exists() }
        if (websiteFile != null) {
            val content = websiteFile.readText()

            val macM1Info = DesktopUpdater.parseUpdateInfo(content, isMac = true, isWindows = false, isMacM1 = true)
            assertNotNull(macM1Info)
            assertTrue(macM1Info.versionCode > 0)
            assertTrue(macM1Info.downloadUrl.endsWith("-m1.dmg"))

            val macIntelInfo = DesktopUpdater.parseUpdateInfo(content, isMac = true, isWindows = false, isMacM1 = false)
            assertNotNull(macIntelInfo)
            assertTrue(macIntelInfo.versionCode > 0)
            assertTrue(macIntelInfo.downloadUrl.endsWith("-intel.dmg"))

            val winInfo = DesktopUpdater.parseUpdateInfo(content, isMac = false, isWindows = true, isMacM1 = false)
            assertNotNull(winInfo)
            assertTrue(winInfo.versionCode > 0)
            assertTrue(winInfo.downloadUrl.endsWith(".msi"))
        }
    }
}
