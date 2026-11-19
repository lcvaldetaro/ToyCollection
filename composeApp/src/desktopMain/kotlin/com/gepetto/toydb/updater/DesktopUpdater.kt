package com.gepetto.toydb.updater

import club.gepetto.GcLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
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

    fun resolveInstallerFileName(downloadUrl: String): String {
        val rawName = try {
            val path = URI.create(downloadUrl).path ?: ""
            path.substringAfterLast('/').takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: downloadUrl.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
        return rawName ?: if (isMac()) "toydatabasemanager.dmg" else "toydatabasemanager.msi"
    }

    fun defaultDownloadsDir(): File {
        val userHome = System.getProperty("user.home")
        if (!userHome.isNullOrBlank()) {
            val downloads = File(userHome, "Downloads")
            if (downloads.exists() && downloads.isDirectory && downloads.canWrite()) {
                return downloads
            }
        }
        return File(System.getProperty("java.io.tmpdir"))
    }

    /**
     * Downloads the installer package directly inside the JVM process (which avoids browser
     * attachment of the macOS `com.apple.quarantine` extended attribute) and clears any
     * quarantine attributes on macOS before returning the downloaded file.
     */
    fun downloadUpdateFile(
        downloadUrl: String,
        targetDir: File = defaultDownloadsDir(),
        onProgress: ((Float?) -> Unit)? = null
    ): File? {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val fileName = resolveInstallerFileName(downloadUrl)
            val targetFile = File(targetDir, fileName)
            val tempFile = File(targetDir, "$fileName.part")

            val url = URI.create(downloadUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"

            if (connection is HttpsURLConnection) {
                connection.sslSocketFactory = EnabledCiphersSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                GcLog.e("DesktopUpdater: Failed to download installer from $downloadUrl. Status: $responseCode")
                return null
            }

            val totalBytes = connection.contentLengthLong
            onProgress?.invoke(if (totalBytes > 0) 0f else null)

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloadedBytes = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress?.invoke(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            if (isMac()) {
                clearMacQuarantine(targetFile)
            }

            onProgress?.invoke(1f)
            return targetFile
        } catch (e: Exception) {
            GcLog.e("DesktopUpdater: Error downloading update from $downloadUrl", e)
            return null
        }
    }

    fun clearMacQuarantine(file: File) {
        if (!isMac() || !file.exists()) return
        try {
            ProcessBuilder("xattr", "-d", "com.apple.quarantine", file.absolutePath)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (_: Exception) {
            // Ignore if attribute is not present
        }
    }

    fun openInstaller(installerFile: File): Boolean {
        if (!installerFile.exists()) return false
        return try {
            if (isMac()) {
                clearMacQuarantine(installerFile)
                ProcessBuilder("open", installerFile.absolutePath).start()
                true
            } else if (isWindows()) {
                if (installerFile.name.lowercase().endsWith(".msi")) {
                    ProcessBuilder("msiexec", "/i", installerFile.absolutePath).start()
                } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(installerFile)
                } else {
                    ProcessBuilder("cmd", "/c", "start", "", installerFile.absolutePath).start()
                }
                true
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(installerFile)
                true
            } else {
                ProcessBuilder("xdg-open", installerFile.absolutePath).start()
                true
            }
        } catch (e: Exception) {
            GcLog.e("DesktopUpdater: Error opening installer: ${installerFile.absolutePath}", e)
            false
        }
    }

    /**
     * Downloads the update installer directly without using a web browser (preventing macOS
     * browser quarantine flags) and launches it. Falls back to opening the URL in the system
     * browser only if the direct download or launch fails.
     */
    fun downloadAndOpenUpdate(
        downloadUrl: String,
        targetDir: File = defaultDownloadsDir(),
        onProgress: ((Float?) -> Unit)? = null
    ): Boolean {
        val downloaded = downloadUpdateFile(downloadUrl, targetDir, onProgress)
        if (downloaded != null && openInstaller(downloaded)) {
            return true
        }
        openBrowser(downloadUrl)
        return false
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
