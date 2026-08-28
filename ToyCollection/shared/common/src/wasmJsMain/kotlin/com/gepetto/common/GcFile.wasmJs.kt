package com.gepetto.common

actual class GcFile actual constructor(val pathname: String) {
    actual constructor(parent: String, child: String) : this(if (parent.isEmpty()) child else "$parent/$child")
    actual constructor(parent: GcFile?, child: String) : this(if (parent == null) child else "${parent.pathname}/$child")

    actual val parentFile: GcFile? get() {
        val idx = pathname.lastIndexOf('/')
        return if (idx == -1) null else GcFile(pathname.substring(0, idx))
    }
    actual val absolutePath: String get() = pathname

    actual fun exists(): Boolean = false
    actual fun writeText(text: String) {}
    actual fun readText(): String = ""
    actual fun writeBytes(bytes: ByteArray) {}
    actual fun readBytes(): ByteArray = ByteArray(0)
    actual fun mkdirs(): Boolean = false
    actual fun length(): Long = 0L
    actual fun lastModified(): Long = 0L
    actual fun delete(): Boolean = false

    override fun toString(): String = pathname
}
