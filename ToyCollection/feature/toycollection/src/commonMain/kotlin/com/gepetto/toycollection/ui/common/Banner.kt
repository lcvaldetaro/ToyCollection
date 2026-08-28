package com.gepetto.toycollection.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcBanner

@Composable
fun BoxScope.Banner(
    text: String,
    modifier: Modifier = Modifier,
) {
    GcBanner(modifier
        .align(Alignment.Center)
        .height(128.dp),
        imageInnerVerticalPadding = 4.dp
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = Color.Black,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(4.dp)
                .align(Alignment.TopCenter)
        )
    }
}