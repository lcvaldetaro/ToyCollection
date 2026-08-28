package com.gepetto.common

import coil3.ImageLoader
import toycollectionmultiplatform.shared.common.generated.resources.Res

object Common  {
    lateinit var directoryFile: GcFile
    lateinit var packageFolder: String
    lateinit var imageLoader: ImageLoader
    var caching: Boolean = false
    var forceLightMode: Boolean = false
    var forceDarkMode: Boolean = false
    var customBaseUrl: String = ""

    fun getActiveBaseUrl(): String {
        return if (customBaseUrl.isNotEmpty()) customBaseUrl else getDefaultBaseUrl()
    }

    var versionString = ""
    var versionBuild = 0L
    var appName = ""
    var releaseVersion = true
    var privacyPolicyMarkdown = ""

    suspend fun initializePrivacyPolicy() {
        try {
            val sysLang = getSystemLanguage()
            val language = if (sysLang.length >= 2) sysLang.substring(0, 2).lowercase() else sysLang.lowercase()
            val fileName = if (language == "pt" || language == "es" || language == "it" || language == "de" || language == "fr") {
                "${language}_privacypolicy.md"
            } else {
                "en_privacypolicy.md"
            }
            privacyPolicyMarkdown = readResourceTextFile(fileName)
        } catch (e: Exception) {
            try {
                privacyPolicyMarkdown = readResourceTextFile("en_privacypolicy.md")
            } catch (ex: Exception) {
                privacyPolicyMarkdown = ""
            }
        }
    }

    private suspend fun readResourceTextFile(fileName: String): String {
        return Res.readBytes("files/$fileName").decodeToString()
    }

    fun testInit() {
        directoryFile = GcFile("")
        packageFolder = ""
    }

    fun clearGcImageCache() {
        if (::imageLoader.isInitialized) {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
        }
    }

    fun clearGcImageCache(image: String) {
        if (::imageLoader.isInitialized) {
            imageLoader.diskCache?.remove("${packageFolder}${image}")
            imageLoader.memoryCache?.remove(coil3.memory.MemoryCache.Key("${packageFolder}${image}"))
        }
    }
}