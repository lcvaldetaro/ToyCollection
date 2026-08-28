package com.gepetto.toycollection.ui


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.scaffold.GcAdaptiveScaffold
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.toycollection.ui.common.images.NavigationButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopView(
    onIconCliked: (String) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: (@Composable () -> Unit),
) {
    val gcNavButtons = NavigationButtons("Home") { onIconCliked(it.label!!) }

    GcTheme(darkTheme = darkTheme) {
        GcAdaptiveScaffold(
            modifier = modifier.fillMaxSize(),
            iconBackgroundColor = sysBackgroundColor(),
            gcNavButtons = gcNavButtons.buttonList,
            content = { content() }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    GcTheme {
        Surface {
            TopView(
                onIconCliked = {},
                content = {}
            )
        }
    }
}


