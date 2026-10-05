package com.gepetto.toydb.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    // No-op on Android
}

@Composable
actual fun PlatformGridScrollbar(
    state: LazyGridState,
    modifier: Modifier
) {
    // No-op on Android
}
