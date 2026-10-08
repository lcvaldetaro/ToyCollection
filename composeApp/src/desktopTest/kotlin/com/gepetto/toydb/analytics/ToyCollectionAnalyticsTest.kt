package com.gepetto.toydb.analytics

import com.gepetto.toydb.platform.SettingsStorage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToyCollectionAnalyticsTest {

    @BeforeTest
    fun setUp() {
        ToyCollectionAnalytics.resetSessionStateForTesting()
    }

    @Test
    fun testAllLiteralsContainToyCollection() {
        val osList = listOf("MAC", "WINDOWS", "LINUX", "Android", "Web", "OTHER")

        for (os in osList) {
            val installTag = ToyCollectionAnalytics.getFirstInstallEventLiteral(os)
            assertTrue(
                installTag.contains("toy_collection"),
                "First install tag '$installTag' for OS '$os' must contain 'toy_collection'"
            )

            val mainLoadTag = ToyCollectionAnalytics.getMainLoadEventLiteral(os)
            assertTrue(
                mainLoadTag.contains("toy_collection"),
                "Main load tag '$mainLoadTag' for OS '$os' must contain 'toy_collection'"
            )
        }
    }

    @Test
    fun testExactFirstInstallLiterals() {
        assertEquals("toy_collection_desktop_new_installation_MAC", ToyCollectionAnalytics.getFirstInstallEventLiteral("MAC"))
        assertEquals("toy_collection_desktop_new_installation_WINDOWS", ToyCollectionAnalytics.getFirstInstallEventLiteral("WINDOWS"))
        assertEquals("toy_collection_desktop_new_installation_LINUX", ToyCollectionAnalytics.getFirstInstallEventLiteral("LINUX"))
        assertEquals("toy_collection_mobile_new_installation", ToyCollectionAnalytics.getFirstInstallEventLiteral("Android"))
        assertEquals("toy_collection_web_new_installation", ToyCollectionAnalytics.getFirstInstallEventLiteral("Web"))
        assertEquals("toy_collection_new_installation", ToyCollectionAnalytics.getFirstInstallEventLiteral("UNKNOWN"))
    }

    @Test
    fun testExactMainLoadLiterals() {
        assertEquals("toy_collection_desktop_home_impression_MAC", ToyCollectionAnalytics.getMainLoadEventLiteral("MAC"))
        assertEquals("toy_collection_desktop_home_impression_WINDOWS", ToyCollectionAnalytics.getMainLoadEventLiteral("WINDOWS"))
        assertEquals("toy_collection_desktop_home_impression_LINUX", ToyCollectionAnalytics.getMainLoadEventLiteral("LINUX"))
        assertEquals("toy_collection_mobile_home_impression", ToyCollectionAnalytics.getMainLoadEventLiteral("Android"))
        assertEquals("toy_collection_web_home_impression", ToyCollectionAnalytics.getMainLoadEventLiteral("Web"))
    }

    @Test
    fun testFirstRunPersistenceSetsKey() {
        val originalState = SettingsStorage.getBoolean(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED, false)
        try {
            SettingsStorage.remove(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED)
            assertFalse(
                SettingsStorage.getBoolean(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED, false),
                "Key should not be present initially"
            )

            ToyCollectionAnalytics.checkAndLogFirstRun()

            assertTrue(
                SettingsStorage.getBoolean(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED, false),
                "First run flag should be persisted in SettingsStorage as true"
            )

            // Calling again should keep it true and not crash
            ToyCollectionAnalytics.checkAndLogFirstRun()
            assertTrue(SettingsStorage.getBoolean(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED, false))
        } finally {
            SettingsStorage.putBoolean(ToyCollectionAnalytics.KEY_FIRST_RUN_LOGGED, originalState)
        }
    }

    @Test
    fun testAnalyticsMethodInvocationsDoNotThrow() {
        ToyCollectionAnalytics.logMainLoad()
        ToyCollectionAnalytics.logMainLoad() // duplicate call no-op
        ToyCollectionAnalytics.checkAndLogFirstRun()
    }
}
