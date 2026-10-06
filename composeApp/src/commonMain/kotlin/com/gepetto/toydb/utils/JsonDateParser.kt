package com.gepetto.toydb.utils

/** Parses dates like "October 4, 2026" (US English, UTC). Returns epoch milliseconds, or 0 when invalid. */
object JsonDateParser {
    private val months = listOf("january","february","march","april","may","june","july","august","september","october","november","december")

    fun parse(text: String): Long {
        val match = Regex("""^\s*([A-Za-z]+)\s+(\d{1,2}),\s*(\d{4})\s*$""").find(text) ?: return 0L
        val name = match.groupValues[1].lowercase()
        val month = months.indexOfFirst { it == name || (name.length >= 3 && it.startsWith(name)) } + 1
        val day = match.groupValues[2].toInt()
        val year = match.groupValues[3].toInt()
        if (month == 0 || day !in 1..31) return 0L
        return daysFromCivil(year, month, day) * 86_400_000L
    }

    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }
}
