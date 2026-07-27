package com.gepetto.common

import kotlinx.browser.window

actual fun platformExitApp() {
}

actual fun getPlatformBaseUrl(): String = window.location.origin + "/"

actual fun getSystemLanguage(): String {
    try {
        return window.navigator.language
    } catch (e: Exception) {
        return "en"
    }
}
