package com.gepetto.toydb.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = defaultScrollbarStyle().copy(
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            hoverColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
actual fun PlatformGridScrollbar(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = defaultScrollbarStyle().copy(
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            hoverColor = MaterialTheme.colorScheme.primary
        )
    )
}
