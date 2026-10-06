@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.service

@JsFun("""() => {
    const d = new Date();
    const months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
    return months[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear();
}""")
private external fun jsCurrentDate(): String

actual fun getCurrentDateString(): String = jsCurrentDate()
