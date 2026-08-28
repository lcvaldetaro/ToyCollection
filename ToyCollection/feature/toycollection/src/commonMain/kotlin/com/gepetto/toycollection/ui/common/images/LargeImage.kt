package com.gepetto.toycollection.ui.common.images

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.image.GcImage
import club.gepetto.composeutils.getScreenHeight
import com.gepetto.common.Common
import com.gepetto.toycollection.utils.ImageCache
import club.gepetto.composeutils.PlatformBitmap
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun LargeImage (
    modifier: Modifier = Modifier,
    imageFile: String? = null,
    imageResourceRes : DrawableResource? = null,
    imageBitmap : PlatformBitmap? = null,
    containerHeight: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    percentage : Float = 0.25f,
    fullImageOnClick : Boolean = true,
    size: Dp = 0.dp,
    cornerSize: Dp = 8.dp,
    paddingSize: Dp = 8.dp,
    onUpdate: () -> Unit = {},
) {
    var reCompose by remember { mutableStateOf(false)}

    val dpHeight = getScreenHeight().value
    val height = if (containerHeight == 0.dp) (percentage * dpHeight).dp else containerHeight * percentage

    GcImage(
        modifier = modifier.heightIn(max = height).fillMaxWidth(),
        folder = "${Common.directoryFile}",
        imageFile = imageFile,
        imageBitmap = imageBitmap,
        imageResourceRes = imageResourceRes,
        fullImageOnClick = fullImageOnClick,
        size = size,
        cornerSize = cornerSize,
        paddingSize = paddingSize,
        contentScale = contentScale,
        needsDownload = { ImageCache.downloadFile(imageFile) { reCompose = ! reCompose } },
        onUpdate = onUpdate
    )
}
