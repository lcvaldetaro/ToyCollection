package com.gepetto.toycollection.ui.collection.main


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import com.gepetto.toycollection.intentprocessors.CollectionTapAction
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import club.gepetto.composeutils.GcTheme
import com.gepetto.toycollection.ui.common.Banner
import com.gepetto.toycollection.ui.common.GepettoImage

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    onIntentCommand: (CollectionIntent) -> Unit = {},
) {


    Box(modifier.background(Color.Transparent).fillMaxSize()) {
        //GepettoImage()
        Banner(text = "Content being loaded from the network. Please wait...")
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    GcTheme {
        Surface {
            Loading()
        }
    }
}



