package com.gepetto.toydb.platform

actual object PlatformHostHelper {
    actual fun getPlatformName(): String = "Desktop"

    actual fun getOperatingSystem(): String {
        val os = System.getProperty("os.name", "").lowercase()
        return when {
            os.contains("mac") || os.contains("darwin") -> "MAC"
            os.contains("win") -> "WINDOWS"
            os.contains("nux") || os.contains("nix") -> "LINUX"
            else -> "MAC"
        }
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
