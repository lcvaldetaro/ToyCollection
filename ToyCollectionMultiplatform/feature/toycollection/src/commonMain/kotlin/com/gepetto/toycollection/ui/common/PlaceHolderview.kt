package com.gepetto.toycollection.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun PlaceHolderView(
    modifier: Modifier = Modifier,
    text: String = "Toy not selected yet.",
) {
    Box(modifier.fillMaxSize()) {
        Text(text, Modifier.align(Alignment.Center))
    }
}