package com.gepetto.toydb.service

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    return sdf.format(Date())
}
