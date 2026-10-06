@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.utils

import androidx.compose.runtime.Composable

actual fun resolveImageUri(prefix: String, refNum: Int): String? = null

actual fun resolveBitmapUri(filename: String): String? = null

actual fun selectDirectoryDialog(title: String): String? = null

actual fun selectFileDialog(title: String, allowedExtensions: List<String>): String? = null

actual fun isDesktopPlatform(): Boolean = false

@Composable
actual fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit = {}

@JsFun("""(ms) => {
    const d = new Date(ms);
    const p = (n) => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
}""")
private external fun jsFormatTimestamp(millis: Double): String

actual fun formatTimestamp(timestamp: Long): String = jsFormatTimestamp(timestamp.toDouble())
