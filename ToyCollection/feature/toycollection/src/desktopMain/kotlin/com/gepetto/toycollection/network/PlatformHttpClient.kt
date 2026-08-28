package com.gepetto.toycollection.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient

actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    val okHttpClient = OkHttpClient.Builder()
        .connectionSpecs(listOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.COMPATIBLE_TLS,
            ConnectionSpec.CLEARTEXT
        ))
        .build()
    
    return HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        block()
    }
}
