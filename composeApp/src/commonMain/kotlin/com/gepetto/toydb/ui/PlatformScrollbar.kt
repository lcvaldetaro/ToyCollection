package com.gepetto.toydb.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
)

@Composable
expect fun PlatformGridScrollbar(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier = Modifier
)
