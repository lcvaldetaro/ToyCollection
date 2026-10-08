package com.gepetto.toydb.platform

actual object PlatformHostHelper {
    actual fun getPlatformName(): String = "Android"

    actual fun getOperatingSystem(): String = "Android"

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
