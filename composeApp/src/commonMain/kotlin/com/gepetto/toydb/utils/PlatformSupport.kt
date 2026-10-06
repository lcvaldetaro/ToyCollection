package com.gepetto.toydb.utils

import okio.FileSystem

/** File system used for local images and exports. On the web target it is an empty stub. */
expect val systemFileSystem: FileSystem

/** User home directory, or null when the platform has none (web). */
expect fun userHomeDirectory(): String?

/** True only on the browser (wasmJs) target. */
expect fun isWebPlatform(): Boolean

/** Default base URL for database synchronization. On Web, resolves to origin + /database/. */
expect fun getDefaultBaseUrl(): String

/** Platform HTTP client configured to support server cipher suites on desktop, android, and web. */
expect fun createToyHttpClient(block: io.ktor.client.HttpClientConfig<*>.() -> Unit = {}): io.ktor.client.HttpClient
