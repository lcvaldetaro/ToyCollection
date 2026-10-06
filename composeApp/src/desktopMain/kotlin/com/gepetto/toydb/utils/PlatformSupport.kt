package com.gepetto.toydb.utils

import club.gepetto.composeutils.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import okio.FileSystem

actual val systemFileSystem: FileSystem = FileSystem.SYSTEM

actual fun userHomeDirectory(): String? = System.getProperty("user.home")

actual fun isWebPlatform(): Boolean = false

actual fun getDefaultBaseUrl(): String = "https://gepetto.club/database/"

fun enableTlsRsaSuitesIfNeeded() {
    try {
        val disabledAlgorithms = java.security.Security.getProperty("jdk.tls.disabledAlgorithms")
        if (disabledAlgorithms != null && disabledAlgorithms.contains("TLS_RSA_*")) {
            val newDisabledAlgorithms = disabledAlgorithms
                .replace(", TLS_RSA_*", "")
                .replace("TLS_RSA_*, ", "")
                .replace("TLS_RSA_*", "")
            java.security.Security.setProperty("jdk.tls.disabledAlgorithms", newDisabledAlgorithms)
            println("Security Override: Removed TLS_RSA_* from disabled algorithms list.")
        }
    } catch (e: Exception) {
        System.err.println("Failed to override jdk.tls.disabledAlgorithms: ${e.message}")
    }
}

actual fun createToyHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    enableTlsRsaSuitesIfNeeded()
    return createPlatformHttpClient(block)
}
