package com.gepetto.toycollection.ui.common.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.image.GcImage
import com.gepetto.common.Common
import com.gepetto.toycollection.utils.ImageCache
import club.gepetto.composeutils.PlatformBitmap
import club.gepetto.composeutils.gcCurrentTimeMillis
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun ImageListItem (
    modifier: Modifier = Modifier,
    imageFile: String? = null,
    imageResourceRes: DrawableResource?  = null,
    imageBitmap : PlatformBitmap? = null,
    files: Array<String>? = null,
    fullImageOnClick: Boolean = true,
    onClick: () -> Unit = {},
    onUpdate: (Long) -> Unit = {},
) {
    var reCompose by remember { mutableStateOf(false)}

    GcImage(
        modifier = modifier,
        folder = "${Common.directoryFile}",
        imageFile = imageFile,
        imageResourceRes = imageResourceRes,
        imageBitmap = imageBitmap,
        files = files,
        fullImageOnClick = fullImageOnClick,
        onUpdate = { onUpdate(gcCurrentTimeMillis()) },
        size = 84.dp,
        cornerSize = 16.dp,
        paddingSize = 8.dp,
        needsDownload = { ImageCache.downloadFile(imageFile) { reCompose = ! reCompose } },
        onClick = onClick
    )
}
