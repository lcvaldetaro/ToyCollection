package com.gepetto.toydb.updater

import club.gepetto.GcLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val downloadUrl: String
) {
    fun isNewerThan(currentVersionCode: Long): Boolean {
        return versionCode > currentVersionCode
    }
}

object DesktopUpdater {
    const val DEFAULT_UPDATE_JSON_URL = "https://gepetto.club/database/version.json"

    fun isMac(): Boolean {
        return System.getProperty("os.name").lowercase().contains("mac")
    }

    fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    fun isAppleSilicon(): Boolean {
        if (!isMac()) return false
        val osArch = System.getProperty("os.arch").lowercase()
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return true
        }
        return try {
            val process = ProcessBuilder("sysctl", "-n", "hw.optional.arm64").start()
            val output = process.inputStream.bufferedReader().readText().trim()
            output == "1"
        } catch (_: Exception) {
            false
        }
    }

    fun parseUpdateInfo(
        jsonText: String,
        isMac: Boolean = isMac(),
        isWindows: Boolean = isWindows(),
        isMacM1: Boolean = isAppleSilicon()
    ): UpdateInfo? {
        try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            val jsonObject = jsonElement.jsonObject
            val versionName = jsonObject["versionName"]?.jsonPrimitive?.content ?: ""

            val (versionCode, downloadUrl) = when {
                isMac -> {
                    val code = jsonObject["versionCodeMac"]?.jsonPrimitive?.longOrNull ?: 0L
                    val urlKey = if (isMacM1) "urlMacM1" else "urlMacIntel"
                    val url = jsonObject[urlKey]?.jsonPrimitive?.content ?: ""
                    Pair(code, url)
                }
                isWindows -> {
                    val code = jsonObject["versionCodeWindows"]?.jsonPrimitive?.longOrNull ?: 0L
                    val url = jsonObject["urlWindows"]?.jsonPrimitive?.content ?: ""
                    Pair(code, url)
                }
                else -> Pair(0L, "")
            }

            if (versionCode > 0 && downloadUrl.isNotEmpty()) {
                return UpdateInfo(versionName, versionCode, downloadUrl)
            }
        } catch (e: Exception) {
            GcLog.e("DesktopUpdater: Error parsing update info", e)
        }
        return null
    }

    fun checkForUpdates(updateUrl: String = DEFAULT_UPDATE_JSON_URL): UpdateInfo? {
        try {
            val url = URI.create(updateUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection is HttpsURLConnection) {
                connection.sslSocketFactory = EnabledCiphersSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory())
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                GcLog.d("DesktopUpdater: Failed to fetch version.json. Status: $responseCode")
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseUpdateInfo(body)
        } catch (e: Exception) {
            GcLog.e("DesktopUpdater: Error checking for updates", e)
        }
        return null
    }

    fun openBrowser(url: String) {
        try {
            val uri = URI(url)
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri)
            } else {
                val osName = System.getProperty("os.name").lowercase()
                when {
                    osName.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", url))
                    osName.contains("win") -> Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                    else -> Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                }
            }
        } catch (e: Exception) {
            GcLog.e("DesktopUpdater: Error opening browser to URL: $url", e)
        }
    }
}

class EnabledCiphersSSLSocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        val socket = delegate.createSocket(s, host, port, autoClose) as SSLSocket
        enableCiphers(socket)
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val socket = delegate.createSocket(host, port) as SSLSocket
        enableCiphers(socket)
        return socket
    }

    override fun createSocket(host: String?, port: Int, localHost: java.net.InetAddress?, localPort: Int): Socket {
        val socket = delegate.createSocket(host, port, localHost, localPort) as SSLSocket
        enableCiphers(socket)
        return socket
    }

    override fun createSocket(address: java.net.InetAddress?, port: Int): Socket {
        val socket = delegate.createSocket(address, port) as SSLSocket
        enableCiphers(socket)
        return socket
    }

    override fun createSocket(address: java.net.InetAddress?, port: Int, localAddress: java.net.InetAddress?, localPort: Int): Socket {
        val socket = delegate.createSocket(address, port, localAddress, localPort) as SSLSocket
        enableCiphers(socket)
        return socket
    }

    private fun enableCiphers(socket: SSLSocket) {
        val supported = socket.supportedCipherSuites
        val enabled = supported.filter { 
            it.contains("TLS_RSA_WITH_AES_256_GCM_SHA384") || 
            it.contains("TLS_RSA_WITH_AES_128_GCM_SHA256") ||
            it.contains("TLS_RSA_WITH_AES_256_CBC_SHA256") ||
            it.contains("TLS_RSA_WITH_AES_128_CBC_SHA256") ||
            it.contains("TLS_RSA_WITH_AES_256_CBC_SHA") ||
            it.contains("TLS_RSA_WITH_AES_128_CBC_SHA") ||
            it.contains("TLS_ECDHE_") ||
            it.contains("TLS_AES_")
        }.toTypedArray()
        try {
            socket.enabledCipherSuites = enabled
        } catch (_: Exception) {
            // ignore
        }
    }
}
