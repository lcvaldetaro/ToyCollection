package com.gepetto.toydb.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonDateParserParityTest {
    @Test
    fun testDateParityWithSimpleDateFormat() {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }

        val jsonDir = File("json")
        if (!jsonDir.exists() || !jsonDir.isDirectory) {
            println("Skipping JsonDateParserParityTest because json/ directory was not found relative to working directory.")
            return
        }

        val dateRegex = Regex(""""date"\s*:\s*"([^"]+)"""")
        var dateCount = 0

        jsonDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            val content = file.readText()
            dateRegex.findAll(content).forEach { match ->
                val dateStr = match.groupValues[1]
                if (dateStr.isNotBlank()) {
                    val parsedByJsonDateParser = JsonDateParser.parse(dateStr)
                    val parsedBySdf = try {
                        sdf.parse(dateStr)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                    assertEquals(
                        parsedBySdf,
                        parsedByJsonDateParser,
                        "Mismatch for date '$dateStr' in ${file.name}"
                    )
                    dateCount++
                }
            }
        }

        println("Verified $dateCount dates against SimpleDateFormat with 100% parity.")
        assertTrue(dateCount > 0, "Expected to verify at least one date from json files")
    }
}
