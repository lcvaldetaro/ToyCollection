package com.gepetto.toydb.utils

import okio.FileSystem

/** File system used for local images and exports. On the web target it is an empty stub. */
expect val systemFileSystem: FileSystem

/** User home directory, or null when the platform has none (web). */
expect fun userHomeDirectory(): String?

/** True only on the browser (wasmJs) target. */
expect fun isWebPlatform(): Boolean
