package com.gepetto.toydb.utils

import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source

/** The browser has no disk. Every lookup reports "not found" and every write fails. */
private object NoFileSystem : FileSystem() {
    private fun unsupported(): Nothing = throw IOException("File system not available on web")
    override fun canonicalize(path: Path): Path = path
    override fun metadataOrNull(path: Path): FileMetadata? = null
    override fun list(dir: Path): List<Path> = unsupported()
    override fun listOrNull(dir: Path): List<Path>? = null
    override fun openReadOnly(file: Path): FileHandle = unsupported()
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle = unsupported()
    override fun source(file: Path): Source = unsupported()
    override fun sink(file: Path, mustCreate: Boolean): Sink = unsupported()
    override fun appendingSink(file: Path, mustExist: Boolean): Sink = unsupported()
    override fun createDirectory(dir: Path, mustCreate: Boolean) = unsupported()
    override fun atomicMove(source: Path, target: Path) = unsupported()
    override fun delete(path: Path, mustExist: Boolean) = unsupported()
    override fun createSymlink(source: Path, target: Path) = unsupported()
}

actual val systemFileSystem: FileSystem = NoFileSystem

actual fun userHomeDirectory(): String? = null

actual fun isWebPlatform(): Boolean = true

actual fun getDefaultBaseUrl(): String = "http://valdetaro.com/database/"

actual fun createToyHttpClient(block: io.ktor.client.HttpClientConfig<*>.() -> Unit): io.ktor.client.HttpClient {
    return club.gepetto.composeutils.createPlatformHttpClient(block)
}
