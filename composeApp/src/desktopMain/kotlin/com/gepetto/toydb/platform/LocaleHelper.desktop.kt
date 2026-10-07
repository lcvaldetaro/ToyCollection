package com.gepetto.toydb.platform

import java.util.Locale

actual object LocaleHelper {
    private val systemDefaultLocale: Locale = Locale.getDefault()

    actual fun getSystemLanguageCode(): String {
        return systemDefaultLocale.language
    }

    actual fun setAppLocale(languageCode: String) {
        val targetLocale = if (languageCode.isNotEmpty()) {
            Locale.forLanguageTag(languageCode)
        } else {
            systemDefaultLocale
        }
        Locale.setDefault(targetLocale)
    }
}
