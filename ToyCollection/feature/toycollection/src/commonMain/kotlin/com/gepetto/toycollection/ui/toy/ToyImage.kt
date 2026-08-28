package com.gepetto.toycollection.ui.toy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.ui.common.images.LargeImage

@Composable
fun ToyImage(
    toy: Toy,
    modifier: Modifier = Modifier,
) {
    LargeImage(modifier = modifier, imageFile = toy.picture, contentScale = ContentScale.Fit)
}
