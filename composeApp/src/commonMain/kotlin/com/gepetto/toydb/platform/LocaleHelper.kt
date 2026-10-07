package com.gepetto.toydb.platform

expect object LocaleHelper {
    fun getSystemLanguageCode(): String
    fun setAppLocale(languageCode: String)
}
