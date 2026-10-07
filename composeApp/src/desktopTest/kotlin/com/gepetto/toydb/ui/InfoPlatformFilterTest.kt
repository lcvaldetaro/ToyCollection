package com.gepetto.toydb.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfoPlatformFilterTest {

    @Test
    fun testGeneralInfoFiltering() {
        val markdown = """
            <!-- desktop -->
            Customize appearance, collection title, and data storage location.
            <!-- /desktop -->
            <!-- !desktop -->
            Customize appearance and collection title.
            <!-- /!desktop -->

            ### Features
            - **Theme**: Light or Dark.
            - **Collection Title**: Change title.
            <!-- desktop -->
            - **Data Folder**: Choose folder.
            <!-- /desktop -->
        """.trimIndent()

        // Desktop
        val desktopResult = filterPlatformContent(markdown, isDesktop = true, isWeb = false)
        assertTrue(desktopResult.contains("Data Folder"))
        assertTrue(desktopResult.contains("data storage location"))
        assertFalse(desktopResult.contains("<!--"))

        // Android
        val androidResult = filterPlatformContent(markdown, isDesktop = false, isWeb = false)
        assertFalse(androidResult.contains("Data Folder"))
        assertFalse(androidResult.contains("data storage location"))
        assertTrue(androidResult.contains("Customize appearance and collection title."))
        assertFalse(androidResult.contains("<!--"))

        // Web
        val webResult = filterPlatformContent(markdown, isDesktop = false, isWeb = true)
        assertFalse(webResult.contains("Data Folder"))
        assertFalse(webResult.contains("data storage location"))
        assertTrue(webResult.contains("Customize appearance and collection title."))
        assertFalse(webResult.contains("<!--"))
    }

    @Test
    fun testServerSyncInfoFiltering() {
        val markdown = """
            <!-- !web -->
            ### 1. Web Page Creation
            - Export pages to server.
            <!-- /!web -->

            ### Web Server URL
            - Load photos and check updates.
            <!-- !web -->

            ### 3. SFTP Information
            <!-- desktop -->
            - **Username & Password / Key**: Credentials.
            <!-- /desktop -->
            <!-- android -->
            - **Username & Password**: Credentials.
            <!-- /android -->
            <!-- /!web -->
        """.trimIndent()

        // Web
        val webResult = filterPlatformContent(markdown, isDesktop = false, isWeb = true)
        assertTrue(webResult.contains("Web Server URL"))
        assertFalse(webResult.contains("Web Page Creation"))
        assertFalse(webResult.contains("SFTP Information"))
        assertFalse(webResult.contains("<!--"))

        // Desktop
        val desktopResult = filterPlatformContent(markdown, isDesktop = true, isWeb = false)
        assertTrue(desktopResult.contains("Web Server URL"))
        assertTrue(desktopResult.contains("Web Page Creation"))
        assertTrue(desktopResult.contains("SFTP Information"))
        assertTrue(desktopResult.contains("Password / Key"))
        assertFalse(desktopResult.contains("<!--"))

        // Android
        val androidResult = filterPlatformContent(markdown, isDesktop = false, isWeb = false)
        assertTrue(androidResult.contains("Web Server URL"))
        assertTrue(androidResult.contains("Web Page Creation"))
        assertTrue(androidResult.contains("SFTP Information"))
        assertTrue(androidResult.contains("Username & Password"))
        assertFalse(androidResult.contains("Password / Key"))
        assertFalse(androidResult.contains("<!--"))
    }

    @Test
    fun testBackupRestoreFiltering() {
        val markdown = """
            <!-- !web -->
            Safeguard your collection data with full archive backups.

            ### Back Up Collection
            - Saves your entire collection into a single .zip file.
            <!-- /!web -->
            <!-- web -->
            Data storage and synchronization in the web browser.

            ### Browser Storage
            - Your collection changes are saved directly in this web browser.
            <!-- /web -->
        """.trimIndent()

        // Web
        val webResult = filterPlatformContent(markdown, isDesktop = false, isWeb = true)
        assertTrue(webResult.contains("Browser Storage"))
        assertFalse(webResult.contains("Back Up Collection"))
        assertFalse(webResult.contains("<!--"))

        // Desktop
        val desktopResult = filterPlatformContent(markdown, isDesktop = true, isWeb = false)
        assertTrue(desktopResult.contains("Back Up Collection"))
        assertFalse(desktopResult.contains("Browser Storage"))
        assertFalse(desktopResult.contains("<!--"))

        // Android
        val androidResult = filterPlatformContent(markdown, isDesktop = false, isWeb = false)
        assertTrue(androidResult.contains("Back Up Collection"))
        assertFalse(androidResult.contains("Browser Storage"))
        assertFalse(androidResult.contains("<!--"))
    }

    @Test
    fun testAboutFiltering() {
        val markdown = """
            # About
            <!-- !web -->
            - **Backup & Restore**: Zip file.
            - **Website Pages**: Create html.
            - **Cloud & Network Synchronization**: SFTP or Web.
            <!-- /!web -->
            <!-- web -->
            - **Web Catalog Synchronization**: Stream photos from web server.
            <!-- /web -->
        """.trimIndent()

        // Web
        val webResult = filterPlatformContent(markdown, isDesktop = false, isWeb = true)
        assertTrue(webResult.contains("Web Catalog Synchronization"))
        assertFalse(webResult.contains("Backup & Restore"))
        assertFalse(webResult.contains("Website Pages"))
        assertFalse(webResult.contains("Cloud & Network Synchronization"))
        assertFalse(webResult.contains("<!--"))

        // Desktop
        val desktopResult = filterPlatformContent(markdown, isDesktop = true, isWeb = false)
        assertTrue(desktopResult.contains("Backup & Restore"))
        assertTrue(desktopResult.contains("Website Pages"))
        assertTrue(desktopResult.contains("Cloud & Network Synchronization"))
        assertFalse(desktopResult.contains("Web Catalog Synchronization"))
        assertFalse(desktopResult.contains("<!--"))

        // Android
        val androidResult = filterPlatformContent(markdown, isDesktop = false, isWeb = false)
        assertTrue(androidResult.contains("Backup & Restore"))
        assertTrue(androidResult.contains("Website Pages"))
        assertTrue(androidResult.contains("Cloud & Network Synchronization"))
        assertFalse(androidResult.contains("Web Catalog Synchronization"))
        assertFalse(androidResult.contains("<!--"))
    }
}

