package com.gepetto.toydb.platform

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual object PlatformHostHelper {
    actual fun getPlatformName(): String = "Web"

    actual fun getOperatingSystem(): String = "Web"

    actual fun currentTimeMillis(): Long = jsDateNow().toLong()
}
