package com.gepetto.toycollection.utils

fun String.toTimeStamp () : Long {
    return this.toSize()
}

fun String.toSize () : Long {
    var result = 0L
    val str = this.replace(" ", "")
    if (str.isNotEmpty()) try { result = str.toLong() } catch (e: Exception) { println("Exception ${e}") }
    return result
}

fun String.hasData() : Boolean = this.isNotEmpty() && this.isNotBlank()

fun String.stripExcessSpaces(): String {
    return this.trim().replace("\\s+".toRegex(), " ")
}
