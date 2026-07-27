package com.gepetto.common

actual fun platformExitApp() {
    java.lang.System.exit(0)
}

actual fun getPlatformBaseUrl(): String = DESKTOP_BASE_URL

actual fun getSystemLanguage(): String {
    return java.util.Locale.getDefault().language
}
