package com.gepetto.common

actual fun platformExitApp() {
    java.lang.System.exit(0)
}

actual fun getPlatformBaseUrl(): String = MOBILE_BASE_URL

actual fun getSystemLanguage(): String {
    return android.content.res.Resources.getSystem().configuration.locales[0].language
}

actual fun getDefaultBaseUrl(): String = "https://gepetto.club/database/"

