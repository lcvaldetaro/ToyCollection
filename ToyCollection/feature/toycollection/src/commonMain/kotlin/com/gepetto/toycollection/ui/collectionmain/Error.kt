package com.gepetto.toycollection.ui.collection.main


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.toycollection.ui.common.Banner
import com.gepetto.toycollection.ui.common.GepettoImage

@Composable
fun Error(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {



    Box (modifier.fillMaxSize().background(sysBackgroundColor())) {
        GepettoImage()
        Banner("Error loading collection from the network")

        Button (onClick = onClick, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text("Continue")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    GcTheme {
        Surface {
            Error()
        }
    }
}



