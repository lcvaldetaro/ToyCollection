package com.gepetto.toycollection.ui.common.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.image.GcImage
import com.gepetto.common.Common
import com.gepetto.common.WEBSITE_BASE_URL
import com.gepetto.toycollection.utils.ImageCache
import club.gepetto.composeutils.PlatformBitmap
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun SmallImage (
    modifier: Modifier = Modifier,
    imageFile: String? = null,
    imageBitmap: PlatformBitmap? = null,
    imageResource: DrawableResource? = null,
    fullImageOnClick: Boolean = false,
    size: Dp = 48.dp,
    cornerSize: Dp = 16.dp,
    paddingSize: Dp = 4.dp,
    onUpdate: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    var reCompose by remember { mutableStateOf(false)}

    GcImage(
        urlForImages = Common.getActiveBaseUrl(),
        modifier = modifier,
        folder = "${Common.directoryFile}",
        imageFile = imageFile,
        imageBitmap = imageBitmap,
        imageResourceRes = imageResource,
        fullImageOnClick = fullImageOnClick,
        size = size,
        cornerSize = cornerSize,
        paddingSize = paddingSize,
        needsDownload = { ImageCache.downloadFile(imageFile) { reCompose = ! reCompose } },
        onClick = onClick,
        onUpdate = onUpdate
    )
}
