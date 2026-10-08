package com.gepetto.toydb.platform

expect object PlatformHostHelper {
    fun getPlatformName(): String
    fun getOperatingSystem(): String
    fun currentTimeMillis(): Long
}
