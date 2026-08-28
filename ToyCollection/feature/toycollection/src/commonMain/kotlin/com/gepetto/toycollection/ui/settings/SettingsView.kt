package com.gepetto.toycollection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import com.gepetto.common.Common
import com.gepetto.common.WEBSITE_BASE_URL
import com.gepetto.common.getDefaultBaseUrl
import com.gepetto.common.models.Settings
import club.gepetto.composeutils.image.gCsetImagesBaseUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    onThemeChanged: (Boolean, Boolean) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeSelection by remember {
        mutableStateOf(
            when {
                Common.forceDarkMode -> 2
                Common.forceLightMode -> 1
                else -> 0
            }
        )
    }

    var baseUrlInput by remember { mutableStateOf(Common.getActiveBaseUrl()) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = sysBackgroundColor(),
                    titleContentColor = sysForegroundColor(),
                    navigationIconContentColor = sysForegroundColor()
                )
            )
        },
        containerColor = sysBackgroundColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme selection section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = sysBackgroundColor(),
                    inactiveContentColor = sysForegroundColor(),
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = MaterialTheme.colorScheme.outline
                )

                SegmentedButton(
                    selected = themeSelection == 0,
                    onClick = {
                        themeSelection = 0
                        Common.forceLightMode = false
                        Common.forceDarkMode = false
                        onThemeChanged(false, false)
                        saveSettings()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    colors = buttonColors
                ) {
                    Text("System")
                }

                SegmentedButton(
                    selected = themeSelection == 1,
                    onClick = {
                        themeSelection = 1
                        Common.forceLightMode = true
                        Common.forceDarkMode = false
                        onThemeChanged(true, false)
                        saveSettings()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    colors = buttonColors
                ) {
                    Text("Light")
                }

                SegmentedButton(
                    selected = themeSelection == 2,
                    onClick = {
                        themeSelection = 2
                        Common.forceLightMode = false
                        Common.forceDarkMode = true
                        onThemeChanged(false, true)
                        saveSettings()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    colors = buttonColors
                ) {
                    Text("Dark")
                }
            }

            HorizontalDivider()

            // Base URL section
            Text(
                text = "Data Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base URL") },
                placeholder = { Text(WEBSITE_BASE_URL) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysForegroundColor(),
                    unfocusedTextColor = sysForegroundColor(),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = sysForegroundColor().copy(alpha = 0.38f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = sysForegroundColor().copy(alpha = 0.6f),
                    cursorColor = sysForegroundColor()
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val trimmedUrl = baseUrlInput.trim()
                        val finalUrl = if (trimmedUrl.isNotEmpty() && !trimmedUrl.endsWith("/")) "$trimmedUrl/" else trimmedUrl
                        val normalizedUrl = if (finalUrl == getDefaultBaseUrl()) "" else finalUrl
                        val changed = Common.customBaseUrl != normalizedUrl
                        Common.customBaseUrl = normalizedUrl
                        saveSettings()
                        if (changed) {
                            gCsetImagesBaseUrl(Common.getActiveBaseUrl())
                            Common.clearGcImageCache()
                        }
                        baseUrlInput = Common.getActiveBaseUrl()
                    },
                    enabled = baseUrlInput != Common.getActiveBaseUrl(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save URL")
                }

                OutlinedButton(
                    onClick = {
                        baseUrlInput = getDefaultBaseUrl()
                        val changed = Common.customBaseUrl != ""
                        Common.customBaseUrl = ""
                        saveSettings()
                        if (changed) {
                            gCsetImagesBaseUrl(Common.getActiveBaseUrl())
                            Common.clearGcImageCache()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset to Default")
                }
            }
        }
    }
}

private fun saveSettings() {
    club.gepetto.utils.ioCoroutine {
        val s = Settings(
            caching = Common.caching,
            forceLightMode = Common.forceLightMode,
            forceDarkMode = Common.forceDarkMode,
            baseUrl = Common.customBaseUrl
        )
        s.save()
    }
}
