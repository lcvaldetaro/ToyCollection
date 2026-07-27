package com.gepetto.common

actual class GcFile(val file: java.io.File) {
    actual constructor(pathname: String) : this(java.io.File(pathname))
    actual constructor(parent: String, child: String) : this(java.io.File(parent, child))
    actual constructor(parent: GcFile?, child: String) : this(java.io.File(parent?.file, child))

    actual val parentFile: GcFile? get() = file.parentFile?.let { GcFile(it) }
    actual val absolutePath: String get() = file.absolutePath

    actual fun exists(): Boolean = file.exists()
    actual fun mkdirs(): Boolean = file.mkdirs()
    actual fun length(): Long = file.length()
    actual fun lastModified(): Long = file.lastModified()
    actual fun delete(): Boolean = file.delete()

    actual fun writeText(text: String) = file.writeText(text)
    actual fun readText(): String = file.readText()
    actual fun writeBytes(bytes: ByteArray) = file.writeBytes(bytes)
    actual fun readBytes(): ByteArray = file.readBytes()

    override fun toString(): String = file.toString()
}
