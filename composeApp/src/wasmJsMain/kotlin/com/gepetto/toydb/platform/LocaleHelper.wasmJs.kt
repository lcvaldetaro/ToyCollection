package com.gepetto.toydb.platform

import kotlinx.browser.window

actual object LocaleHelper {
    actual fun getSystemLanguageCode(): String {
        return try {
            val navLang: String = window.navigator.language
            navLang.split('-')[0].split('_')[0].lowercase()
        } catch (_: Throwable) {
            "en"
        }
    }

    actual fun setAppLocale(languageCode: String) {
        // Browser sandboxed runtime
    }
}
