package com.gepetto.toydb.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.isSystemInLandscape
import club.gepetto.composeutils.sysForegroundColor

@Composable
fun HomeTitle(
    title: String,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null
) {
    val tint = sysForegroundColor()
    val startPadding = if (isSystemInLandscape()) 24.dp else 30.dp

    Row(
        modifier = modifier
            .padding(start = startPadding, end = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = tint,
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .then(if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier)
                .padding(end = 8.dp)
        )
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
private fun HomeTitlePreview() {
    GcTheme {
        Surface {
            HomeTitle(
                title = "Gepetto Toy Database Manager"
            )
        }
    }
}
