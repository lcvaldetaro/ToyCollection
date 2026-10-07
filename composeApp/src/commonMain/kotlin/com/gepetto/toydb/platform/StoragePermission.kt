package com.gepetto.toydb.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberStoragePermissionRequest(): suspend () -> Boolean
