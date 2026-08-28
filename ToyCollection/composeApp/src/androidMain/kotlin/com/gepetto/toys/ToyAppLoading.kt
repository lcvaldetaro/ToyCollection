package com.gepetto.toys

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import club.gepetto.composeutils.GcE2eBox
import club.gepetto.composeutils.isSystemInLandscape

import club.gepetto.composeutils.Res
import club.gepetto.composeutils.gepetto
import club.gepetto.composeutils.invertedgepetto

@Composable
fun ToyAppLoading() {
    GcE2eBox(
        shaded = true,
        progress = true,
        imageResourceRes = Res.drawable.gepetto,
        darkImageResourceRes = Res.drawable.invertedgepetto,
        contentScale = if (isSystemInLandscape()) ContentScale.FillHeight else ContentScale.Crop,
    )
}