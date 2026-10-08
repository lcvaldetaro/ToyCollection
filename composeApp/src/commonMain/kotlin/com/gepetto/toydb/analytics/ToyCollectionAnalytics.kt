package com.gepetto.toydb.analytics

import club.gepetto.GcLog
import club.gepetto.gcadslib.AnalyticsTracker
import club.gepetto.gcadslib.Bundle
import com.gepetto.toydb.platform.PlatformHostHelper
import com.gepetto.toydb.platform.SettingsStorage

object ToyCollectionAnalytics {

    const val KEY_FIRST_RUN_LOGGED = "analytics_first_run_logged"

    private var hasLoggedMainLoad: Boolean = false

    /**
     * Resolves the exact event literal for first-time installation based on the operating system.
     */
    fun getFirstInstallEventLiteral(os: String = PlatformHostHelper.getOperatingSystem()): String {
        return when (os) {
            "MAC" -> "toy_collection_desktop_new_installation_MAC"
            "WINDOWS" -> "toy_collection_desktop_new_installation_WINDOWS"
            "LINUX" -> "toy_collection_desktop_new_installation_LINUX"
            "Android" -> "toy_collection_mobile_new_installation"
            "Web" -> "toy_collection_web_new_installation"
            else -> "toy_collection_new_installation"
        }
    }

    /**
     * Resolves the exact event literal for main screen load impression based on the operating system.
     */
    fun getMainLoadEventLiteral(os: String = PlatformHostHelper.getOperatingSystem()): String {
        return when (os) {
            "MAC" -> "toy_collection_desktop_home_impression_MAC"
            "WINDOWS" -> "toy_collection_desktop_home_impression_WINDOWS"
            "LINUX" -> "toy_collection_desktop_home_impression_LINUX"
            "Android" -> "toy_collection_mobile_home_impression"
            "Web" -> "toy_collection_web_home_impression"
            else -> "toy_collection_desktop_home_impression_MAC"
        }
    }

    /**
     * Checks if this is the first execution on this device/installation and logs the platform-specific event.
     * Guaranteed to execute at most once across the lifecycle of the installation via SettingsStorage.
     */
    fun checkAndLogFirstRun() {
        val alreadyLogged = SettingsStorage.getBoolean(KEY_FIRST_RUN_LOGGED, false)
        if (!alreadyLogged) {
            SettingsStorage.putBoolean(KEY_FIRST_RUN_LOGGED, true)
            val eventTag = getFirstInstallEventLiteral()
            val bundle = Bundle()
            bundle.putString("platform", PlatformHostHelper.getPlatformName())
            bundle.putString("os", PlatformHostHelper.getOperatingSystem())
            bundle.putLong("timestamp", PlatformHostHelper.currentTimeMillis())
            try {
                AnalyticsTracker.logEvent(eventTag, bundle)
                GcLog.i("Analytics", "Logged first installation event: '$eventTag'")
            } catch (e: Throwable) {
                GcLog.e("Analytics", "Failed to log first installation event: ${e.message}", e)
            }
        }
    }

    /**
     * Logs the main screen load impression event for the active platform.
     * Fires once per app session.
     */
    fun logMainLoad() {
        if (hasLoggedMainLoad) return
        hasLoggedMainLoad = true

        val eventTag = getMainLoadEventLiteral()
        val bundle = Bundle()
        bundle.putString("platform", PlatformHostHelper.getPlatformName())
        bundle.putString("os", PlatformHostHelper.getOperatingSystem())
        bundle.putLong("timestamp", PlatformHostHelper.currentTimeMillis())
        try {
            AnalyticsTracker.logEvent(eventTag, bundle)
            GcLog.i("Analytics", "Logged main load impression event: '$eventTag'")
        } catch (e: Throwable) {
            GcLog.e("Analytics", "Failed to log main load impression: ${e.message}", e)
        }
    }

    /**
     * Resets session-level state flags. Primarily for unit testing.
     */
    fun resetSessionStateForTesting() {
        hasLoggedMainLoad = false
    }
}
