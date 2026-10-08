package com.gepetto.toydb.updater

import java.io.File
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
