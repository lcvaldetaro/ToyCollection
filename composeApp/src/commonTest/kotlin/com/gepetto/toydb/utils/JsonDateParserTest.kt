package com.gepetto.toydb.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonDateParserTest {
    @Test
    fun testParseDates() {
        assertEquals(1791072000000L, JsonDateParser.parse("October 4, 2026"))
        assertEquals(1791072000000L, JsonDateParser.parse("Oct 4, 2026"))
        assertEquals(0L, JsonDateParser.parse("not a date"))
        assertEquals(0L, JsonDateParser.parse(""))
        assertEquals(1709164800000L, JsonDateParser.parse("February 29, 2024"))
    }
}
