package com.gepetto.toydb.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberStoragePermissionRequest(): suspend () -> Boolean = { true }
