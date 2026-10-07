package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.utils.isDesktopPlatform
import com.gepetto.toydb.utils.isWebPlatform
import com.gepetto.toydb.utils.selectDirectoryDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@Composable
fun GeneralSettingsTab(
    currentTheme: Int,
    onThemeChanged: (Int) -> Unit,
    appTitle: String,
    onAppTitleChanged: (String) -> Unit,
    dataPath: String?,
    onDataPathChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 850.dp),
        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
    ) {
        ThemeSelector(currentTheme, onThemeChanged)
        AppTitleSettings(
            title = appTitle,
            onTitleChange = onAppTitleChanged
        )
        if (isDesktopPlatform()) {
            DataDirectorySettings(
                dataPath = dataPath,
                onSelectPath = onDataPathChanged,
                onClearPath = { onDataPathChanged(null) }
            )
        }
        if (isWebPlatform()) {
            WebLocalDataNotice()
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: Int, onThemeChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.theme_mode_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val buttonColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                inactiveContainerColor = Color.Transparent,
                inactiveContentColor = sysTextColor(),
                activeBorderColor = MaterialTheme.colorScheme.primary,
                inactiveBorderColor = MaterialTheme.colorScheme.outline
            )
            SegmentedButton(
                selected = currentTheme == 0,
                onClick = { onThemeChanged(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                colors = buttonColors
            ) { Text(stringResource(Res.string.theme_system), fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 1,
                onClick = { onThemeChanged(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                colors = buttonColors
            ) { Text(stringResource(Res.string.theme_light), fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 2,
                onClick = { onThemeChanged(2) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                colors = buttonColors
            ) { Text(stringResource(Res.string.theme_dark), fontSize = 12.sp) }
        }
    }
}

@Composable
fun AppTitleSettings(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text(stringResource(Res.string.app_title_config_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.app_title_config_desc),
                fontSize = 12.sp,
                color = sysTextColor().copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.app_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysTextColor(),
                    unfocusedTextColor = sysTextColor(),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = sysTextColor().copy(alpha = 0.6f)
                )
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun AppTitleSettingsPreview() {
    GcTheme {
        AppTitleSettings(
            title = "My Awesome Toy Collection",
            onTitleChange = {}
        )
    }
}

@Composable
fun DataDirectorySettings(
    dataPath: String?,
    onSelectPath: (String) -> Unit,
    onClearPath: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.data_dir_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = if (dataPath.isNullOrEmpty()) {
                        stringResource(Res.string.images_dir_default)
                    } else {
                        stringResource(Res.string.images_dir_custom, dataPath)
                    },
                    fontSize = 14.sp,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val selectedDir = selectDirectoryDialog(getString(Res.string.data_dir_title))
                                if (selectedDir != null) {
                                    onSelectPath(selectedDir)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.select_directory))
                    }
                    if (!dataPath.isNullOrEmpty()) {
                        OutlinedButton(onClick = onClearPath) {
                            Text(stringResource(Res.string.clear_custom_path))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WebLocalDataNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text(
                text = stringResource(Res.string.web_local_data_notice),
                fontSize = 14.sp,
                color = sysTextColor()
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
internal fun WebLocalDataNoticePreview() {
    GcTheme {
        WebLocalDataNotice()
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun GeneralSettingsTabPreview() {
    GcTheme {
        GeneralSettingsTab(
            currentTheme = 0,
            onThemeChanged = {},
            appTitle = "Toy Collection",
            onAppTitleChanged = {},
            dataPath = "/path/to/toys",
            onDataPathChanged = {}
        )
    }
}
