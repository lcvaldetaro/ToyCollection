package com.gepetto.toycollection.ui.maker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.sysBackgroundColor

@Composable
fun ToyPlaceHolder(
    modifier: Modifier = Modifier
) {
    Box (modifier.background(sysBackgroundColor()).fillMaxSize()) {
        Text("Not toy selected yet", modifier = Modifier.align(Alignment.Center))
    }
}