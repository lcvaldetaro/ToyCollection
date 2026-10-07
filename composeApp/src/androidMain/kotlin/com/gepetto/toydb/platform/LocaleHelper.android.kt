package com.gepetto.toydb.platform

import android.content.Context
import android.os.Build
import android.os.LocaleList
import club.gepetto.utils.GcAppInfo
import java.util.Locale

actual object LocaleHelper {
    private val systemDefaultLocale: Locale = Locale.getDefault()
    private val systemDefaultLocaleList: LocaleList? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleList.getDefault()
    } else null

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = if (languageCode.isNotEmpty()) {
                LocaleList(targetLocale)
            } else {
                systemDefaultLocaleList ?: LocaleList(targetLocale)
            }
            LocaleList.setDefault(list)
        }
        val context = GcAppInfo.application_Context as? Context
        if (context != null) {
            try {
                val res = context.resources
                val config = res.configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val list = if (languageCode.isNotEmpty()) {
                        LocaleList(targetLocale)
                    } else {
                        systemDefaultLocaleList ?: LocaleList(targetLocale)
                    }
                    config.setLocales(list)
                } else {
                    @Suppress("DEPRECATION")
                    config.locale = targetLocale
                }
                @Suppress("DEPRECATION")
                res.updateConfiguration(config, res.displayMetrics)
            } catch (_: Throwable) {}
        }
    }
}
