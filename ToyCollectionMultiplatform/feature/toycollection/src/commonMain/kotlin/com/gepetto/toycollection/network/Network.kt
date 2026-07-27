package com.gepetto.toycollection.network

import com.gepetto.common.BASE_URL
import com.gepetto.common.Common
import com.gepetto.common.FOLDER
import com.gepetto.common.MAKERS_QUERY_STRING
import com.gepetto.common.WEBSITE_BASE_URL
import com.gepetto.toycollection.models.ToysResponse
import com.gepetto.toycollection.models.CollectionData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.gepetto.common.GcFile
import io.ktor.http.ContentType
import club.gepetto.utils.ioDispatcher

class Network {
    companion object {
        val client = createPlatformHttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }, contentType = ContentType.Application.Json)
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }, contentType = ContentType.Application.OctetStream)
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }, contentType = ContentType.Text.Plain)
            }
        }

        fun getMakers(
            forceOption : Boolean = true,
            cleanOption: Boolean = false,
            onError: (Int) -> Unit = {},
            onSuccess: (CollectionData) -> Unit = {},
        ) {
            CoroutineScope(ioDispatcher).launch {
                try {
                    val url = "${Common.getActiveBaseUrl()}$MAKERS_QUERY_STRING"
                    println("getMakers requesting url: $url")
                    val response = client.get(url)
                    println("getMakers response status: ${response.status.value}")
                    if (response.status.value == 200) {
                        val collectionData = response.body<CollectionData>()
                        onSuccess(collectionData.normalizeData())
                    } else {
                        onError(response.status.value)
                    }
                } catch (e: Exception) {
                    println("getMakers exception: ${e.message}")
                    e.printStackTrace()
                    onError(-1)
                }
            }
        }
 
        fun getToys(
            typeQuery: String,
            forceOption : Boolean = true,
            cleanOption: Boolean = false,
            onError: (Int) -> Unit = {},
            onSuccess: (ToysResponse) -> Unit = {},
        ) {
            println("getToys will download $typeQuery")
            getFile(
                filename = typeQuery,
                forceOption = forceOption,
                cleanOption = cleanOption,
                onError = onError,
                onSuccess = { rc, bytes ->
                    val jsonStr = bytes.decodeToString()
                    val toysResponse = ToysResponse.fromJson(jsonStr)
                    if (toysResponse != null)
                        onSuccess(toysResponse)
                    else
                        onError(401)
                }
            )
        }
 
        fun getFile(
            forceOption: Boolean = false,
            cleanOption: Boolean = false,
            directory: GcFile? = Common.directoryFile,
            baseUrl: String = Common.getActiveBaseUrl(),
            filename: String,
            onError: (Int) -> Unit = {},
            onSuccess: (Int, ByteArray) -> Unit,
        ) {
            CoroutineScope(ioDispatcher).launch {
                try {
                    val url = baseUrl + filename
                    println("getFile requesting url: $url")
                    val response = client.get(url)
                    println("getFile response status: ${response.status.value}")
                    if (response.status.value == 200) {
                        val bytes = response.readBytes()
                        if (directory != null) {
                            val destFile = GcFile(directory, filename)
                            println("getFile writing to: ${destFile.absolutePath}")
                            destFile.parentFile?.mkdirs()
                            destFile.writeBytes(bytes)
                        }
                        onSuccess(response.status.value, bytes)
                    } else {
                        onError(response.status.value)
                    }
                } catch (e: Exception) {
                    println("getFile exception: ${e.message}")
                    e.printStackTrace()
                    onError(-1)
                }
            }
        }
    }
}