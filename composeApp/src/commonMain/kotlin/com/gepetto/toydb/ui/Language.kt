package com.gepetto.toydb.ui

import com.gepetto.toydb.platform.LocaleHelper

data class Language(
    val thisLanguage: String = "en",
    val english: String = "English",
    val portuguese: String = "Portuguese",
    val french: String = "French",
    val spanish: String = "Spanish",
    val italian: String = "Italian",
    val german: String = "German"
)

val languages = listOf(
    Language("en"),
    Language("pt", "Inglês", "Português", "Francês", "Espanhol", "Italiano", "Alemão"),
    Language("fr", "Anglais", "Portugais", "Français", "Espagnol", "Italien", "Allemand"),
    Language("es", "Inglés", "Portugués", "Francés", "Español", "Italiano", "Alemán"),
    Language("it", "Inglese", "Portoghese", "Francese", "Spagnolo", "Italiano", "Tedesco"),
    Language("de", "Englisch", "Portugiesisch", "Französisch", "Spanisch", "Italienisch", "Deutsch")
)

fun getSystemLanguage(): String {
    return LocaleHelper.getSystemLanguageCode()
}
