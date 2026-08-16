package com.gepetto.toydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.image.GcImage
import club.gepetto.composeutils.PlatformBitmap
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.utils.resolveImageUri
import com.gepetto.toydb.utils.resolveBitmapUri
import com.gepetto.toydb.utils.ImageResolverConfig
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private val htmlHttpClient = HttpClient(OkHttp)

fun resolveImagesDir(db: com.gepetto.toydb.database.ToyDatabase): Path {
    val customPath = ImageResolverConfig.imagesPath
    if (!customPath.isNullOrEmpty()) {
        return customPath.toPath()
    }
    // Desktop / Fallbacks
    val homeDir = System.getProperty("user.home")
    if (homeDir != null) {
        val f = homeDir.toPath().div("valdetaro/ToyCollection/ToyDb/images")
        if (FileSystem.SYSTEM.exists(f)) return f
    }
    val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
    possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) }?.let { return it }
    
    return "images".toPath()
}

@Composable
fun SyncImage(
    toy: Toy? = null,
    repository: ToyRepository? = null,
    modifier: Modifier = Modifier,
    prefix: String = "car",
    filename: String? = null,
    isMainImage: Boolean = true,
    size: Dp = 0.dp,
    cornerSize: Dp = 0.dp,
    paddingSize: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.FillBounds,
    fullImageOnClick: Boolean = false,
    files: Array<String>? = null,
    fallbackBitmap: PlatformBitmap? = null,
    timestamp: Long? = null,
    onClick: () -> Unit = {},
    onDownloaded: () -> Unit = {}
) {
    val refNum = toy?.refNum ?: 0
    val toyDescription = toy?.description ?: filename ?: ""
    var downloadSuccessTrigger by remember { mutableStateOf(0) }

    // Resolve local path
    val localPath = remember(refNum, filename, isMainImage, downloadSuccessTrigger) {
        if (isMainImage && toy != null) {
            resolveImageUri(prefix, toy.refNum)
        } else {
            filename?.let { resolveBitmapUri(it) }
        }
    }
    
    val expectedDbTimestamp = remember(toy, isMainImage, filename, timestamp) {
        if (timestamp != null) {
            timestamp
        } else if (isMainImage && toy != null) {
            toy.pictureTimeStamp
        } else if (!isMainImage && toy != null && filename != null) {
            toy.getSecondaryImages().find { it.filename == filename }?.timestamp ?: 0L
        } else {
            0L
        }
    }

    val localFileTime = remember(localPath) {
        if (localPath != null) {
            try {
                val metadata = FileSystem.SYSTEM.metadataOrNull(localPath.toPath())
                metadata?.lastModifiedAtMillis ?: 0L
            } catch (e: Exception) {
                0L
            }
        } else {
            0L
        }
    }

    val isOutdated = expectedDbTimestamp > 0L && localFileTime > 0L && expectedDbTimestamp > localFileTime
    var localFileExists by remember(refNum, localPath, isOutdated) { 
        mutableStateOf(localPath != null && !isOutdated) 
    }
    
    if (localFileExists && localPath != null) {
        GcImage(
            modifier = modifier,
            imageFile = localPath,
            contentDescription = toyDescription,
            fullImageOnClick = fullImageOnClick,
            size = size,
            cornerSize = cornerSize,
            paddingSize = paddingSize,
            files = files,
            contentScale = contentScale,
            onClick = onClick
        )
    } else {
        val baseUrl = remember { repository?.getBaseUrlSetting() }
        val dlFilename = if (isMainImage) toy?.picture?.trim() ?: "" else filename?.trim() ?: ""
        
        if (baseUrl.isNullOrBlank() || dlFilename.isEmpty() || repository == null) {
            GcImage(
                modifier = modifier,
                imageFile = localPath, // fallback to local image if exists even if we can't download
                imageBitmap = fallbackBitmap,
                contentDescription = toyDescription,
                fullImageOnClick = fullImageOnClick,
                size = size,
                cornerSize = cornerSize,
                paddingSize = paddingSize,
                contentScale = contentScale,
                onClick = onClick
            )
        } else {
            var downloadFailed by remember { mutableStateOf(false) }
            
            LaunchedEffect(refNum, baseUrl, dlFilename) {
                val repoDb = repository.db
                withContext(club.gepetto.utils.ioDispatcher) {
                    try {
                        val imagesDir = resolveImagesDir(repoDb)
                        val targetFile = imagesDir.div(dlFilename)
                        val url = if (baseUrl.endsWith("/")) "$baseUrl$dlFilename" else "$baseUrl/$dlFilename"
                        
                        val response = htmlHttpClient.get(url)
                        if (response.status.value == 200) {
                            val bytes = response.readBytes()
                            FileSystem.SYSTEM.write(targetFile) {
                                write(bytes)
                            }
                            withContext(Dispatchers.Main) {
                                localFileExists = true
                                downloadSuccessTrigger++
                                onDownloaded()
                            }
                        } else {
                            downloadFailed = true
                        }
                    } catch (e: Exception) {
                        club.gepetto.GcLog.e("SyncImage", "Failed to download image $dlFilename: ${e.message}", e)
                        downloadFailed = true
                    }
                }
            }
            
            if (downloadFailed) {
                GcImage(
                    modifier = modifier,
                    imageFile = localPath, // fallback to local image if exists
                    imageBitmap = fallbackBitmap,
                    contentDescription = toyDescription,
                    fullImageOnClick = fullImageOnClick,
                    size = size,
                    cornerSize = cornerSize,
                    paddingSize = paddingSize,
                    contentScale = contentScale,
                    onClick = onClick
                )
            } else {
                // Show placeholder (Hourglass icon)
                val iconSize = if (size != 0.dp) size / 2 else 24.dp
                Box(
                    modifier = if (size != 0.dp) modifier.size(size) else modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Downloading image...",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
