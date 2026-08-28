package com.gepetto.toycollection.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import club.gepetto.composeutils.isSystemInLandscape
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*

@Composable
fun GepettoImage(modifier: Modifier = Modifier) {
    Image(
       painter = painterResource(if (isSystemInDarkTheme()) Res.drawable.invertedgepetto else Res.drawable.gepetto),
       contentDescription = "",
       modifier = modifier.fillMaxSize(),
       contentScale = if (isSystemInLandscape()) ContentScale.FillHeight else ContentScale.Crop,
    )
}