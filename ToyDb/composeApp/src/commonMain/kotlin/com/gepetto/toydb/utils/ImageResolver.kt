package com.gepetto.toydb.utils

object ImageResolverConfig {
    var imagesPath: String? = null
}

expect fun resolveImageUri(prefix: String, refNum: Int): String?

expect fun resolveBitmapUri(filename: String): String?

expect fun selectDirectoryDialog(title: String): String?

expect fun selectFileDialog(title: String, allowedExtensions: List<String>): String?

expect fun isDesktopPlatform(): Boolean

