package com.gepetto.toydb.utils

import android.content.Context
import java.io.File

object AndroidContext {
    lateinit var appContext: Context
}

actual fun resolveImageUri(prefix: String, refNum: Int): String? {
    val extensions = listOf("jpg", "jpeg", "png", "gif", "webp", "JPG", "PNG", "GIF")
    val dir = File(AndroidContext.appContext.getExternalFilesDir(null), "images")
    if (!dir.exists()) return null
    
    for (ext in extensions) {
        val file = File(dir, "$prefix$refNum.$ext")
        if (file.exists()) {
            return file.absolutePath
        }
    }
    return null
}

actual fun resolveBitmapUri(filename: String): String? {
    val dir = File(AndroidContext.appContext.getExternalFilesDir(null), "images")
    if (!dir.exists()) return null
    val file = File(dir, filename.trim())
    return if (file.exists()) file.absolutePath else null
}

actual fun selectDirectoryDialog(title: String): String? {
    return null
}

actual fun selectFileDialog(title: String, allowedExtensions: List<String>): String? {
    return null
}

actual fun isDesktopPlatform(): Boolean = false
