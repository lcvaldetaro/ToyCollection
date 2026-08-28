package com.gepetto.common

expect class GcFile {
    constructor(pathname: String)
    constructor(parent: String, child: String)
    constructor(parent: GcFile?, child: String)

    val parentFile: GcFile?
    val absolutePath: String

    fun exists(): Boolean
    fun writeText(text: String)
    fun readText(): String
    fun writeBytes(bytes: ByteArray)
    fun readBytes(): ByteArray
    fun mkdirs(): Boolean
    fun length(): Long
    fun lastModified(): Long
    fun delete(): Boolean
}
