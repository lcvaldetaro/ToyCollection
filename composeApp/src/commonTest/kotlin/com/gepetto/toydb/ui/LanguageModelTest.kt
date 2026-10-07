package com.gepetto.toydb.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageModelTest {

    @Test
    fun testSupportedLanguagesCount() {
        assertEquals(6, languages.size, "Should support exactly 6 languages")
        val codes = languages.map { it.thisLanguage }
        assertTrue(codes.contains("en"))
        assertTrue(codes.contains("pt"))
        assertTrue(codes.contains("fr"))
        assertTrue(codes.contains("es"))
        assertTrue(codes.contains("it"))
        assertTrue(codes.contains("de"))
    }

    @Test
    fun testDefaultLanguageValues() {
        val defaultLang = Language()
        assertEquals("en", defaultLang.thisLanguage)
        assertEquals("English", defaultLang.english)
        assertEquals("Portuguese", defaultLang.portuguese)
        assertEquals("French", defaultLang.french)
        assertEquals("Spanish", defaultLang.spanish)
        assertEquals("Italian", defaultLang.italian)
        assertEquals("German", defaultLang.german)
    }

    @Test
    fun testPortugueseTranslations() {
        val ptLang = languages.find { it.thisLanguage == "pt" }
        assertNotNull(ptLang)
        assertEquals("Inglês", ptLang.english)
        assertEquals("Português", ptLang.portuguese)
        assertEquals("Francês", ptLang.french)
        assertEquals("Espanhol", ptLang.spanish)
        assertEquals("Italiano", ptLang.italian)
        assertEquals("Alemão", ptLang.german)
    }

    @Test
    fun testGermanTranslations() {
        val deLang = languages.find { it.thisLanguage == "de" }
        assertNotNull(deLang)
        assertEquals("Englisch", deLang.english)
        assertEquals("Portugiesisch", deLang.portuguese)
        assertEquals("Französisch", deLang.french)
        assertEquals("Spanisch", deLang.spanish)
        assertEquals("Italienisch", deLang.italian)
        assertEquals("Deutsch", deLang.german)
    }

    @Test
    fun testFrenchTranslations() {
        val frLang = languages.find { it.thisLanguage == "fr" }
        assertNotNull(frLang)
        assertEquals("Anglais", frLang.english)
        assertEquals("Portugais", frLang.portuguese)
        assertEquals("Français", frLang.french)
        assertEquals("Espagnol", frLang.spanish)
        assertEquals("Italien", frLang.italian)
        assertEquals("Allemand", frLang.german)
    }

    @Test
    fun testSpanishTranslations() {
        val esLang = languages.find { it.thisLanguage == "es" }
        assertNotNull(esLang)
        assertEquals("Inglés", esLang.english)
        assertEquals("Portugués", esLang.portuguese)
        assertEquals("Francés", esLang.french)
        assertEquals("Español", esLang.spanish)
        assertEquals("Italiano", esLang.italian)
        assertEquals("Alemán", esLang.german)
    }

    @Test
    fun testItalianTranslations() {
        val itLang = languages.find { it.thisLanguage == "it" }
        assertNotNull(itLang)
        assertEquals("Inglese", itLang.english)
        assertEquals("Portoghese", itLang.portuguese)
        assertEquals("Francese", itLang.french)
        assertEquals("Spagnolo", itLang.spanish)
        assertEquals("Italiano", itLang.italian)
        assertEquals("Tedesco", itLang.german)
    }

    @Test
    fun testGetSystemLanguageNonEmpty() {
        val sysLang = getSystemLanguage()
        assertTrue(sysLang.isNotEmpty(), "System language should not be empty")
    }
}
