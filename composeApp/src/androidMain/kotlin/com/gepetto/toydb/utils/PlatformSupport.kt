package com.gepetto.toydb.utils

import okio.FileSystem

actual val systemFileSystem: FileSystem = FileSystem.SYSTEM

actual fun userHomeDirectory(): String? = System.getProperty("user.home")

actual fun isWebPlatform(): Boolean = false

actual fun getDefaultBaseUrl(): String = "https://gepetto.club/database/"

actual fun createToyHttpClient(block: io.ktor.client.HttpClientConfig<*>.() -> Unit): io.ktor.client.HttpClient {
    return club.gepetto.composeutils.createPlatformHttpClient(block)
}
