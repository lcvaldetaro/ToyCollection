package com.gepetto.toydb.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.GcLog
import toydb.composeapp.generated.resources.Res
import club.gepetto.composeutils.*
import androidx.compose.ui.graphics.Color
import okio.FileSystem
import okio.Path.Companion.toPath
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import club.gepetto.composeutils.GcE2eBox
import club.gepetto.composeutils.GcTheme
import androidx.compose.ui.platform.LocalWindowInfo
import club.gepetto.composeutils.navbar.GcNavButton
import club.gepetto.composeutils.navigation3.GcNavDisplay
import club.gepetto.composeutils.navigation3.GcSceneStrategy
import club.gepetto.composeutils.navigation3.rememberGcSceneStrategy
import club.gepetto.composeutils.navigation3.removeUpToInclusive
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*
import club.gepetto.composeutils.scaffold.GcAdaptiveScaffold
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.SftpService
import com.gepetto.toydb.utils.isDesktopPlatform

private fun copyMakerImages(repository: ToyRepository, selectedImagesPath: String) {
    val possibleImagesDirs = listOf(
        "images",
        "../images",
        "ToyDb/images",
        "../ToyDb/images"
    )
    val sourceDir = possibleImagesDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) }
    if (sourceDir == null) {
        GcLog.e("ToyDbSetup", "Source images directory not found in possible default locations.")
        return
    }

    val targetDir = selectedImagesPath.toPath()
    if (sourceDir.toString() == targetDir.toString()) {
        GcLog.d("ToyDbSetup", "Selected images directory is same as default directory. No copy needed.")
        return
    }

    try {
        if (!FileSystem.SYSTEM.exists(targetDir)) {
            FileSystem.SYSTEM.createDirectories(targetDir)
        }
        val makers = repository.getMakers()
        val filenames = makers.flatMap { maker ->
            maker.bitmaps.split(" ").filter { it.trim().isNotEmpty() }
        }.distinct()

        var copiedCount = 0
        for (filename in filenames) {
            val srcFile = sourceDir.div(filename)
            if (FileSystem.SYSTEM.exists(srcFile)) {
                val destFile = targetDir.div(filename)
                if (!FileSystem.SYSTEM.exists(destFile)) {
                    FileSystem.SYSTEM.copy(srcFile, destFile)
                    copiedCount++
                }
            }
        }
        GcLog.d("ToyDbSetup", "Successfully copied $copiedCount maker images to $selectedImagesPath")
    } catch (e: Exception) {
        GcLog.e("ToyDbSetup", "Error copying maker images: ${e.message}", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToyDbNavigation(
    db: ToyDatabase,
    sftpService: SftpService,
    onAppTitleChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val repository = remember { ToyRepository(db) }
    var themeMode by remember { mutableStateOf(repository.getThemeSetting()) }

    var showSetupPrompt by remember {
        mutableStateOf(
            isDesktopPlatform() && repository.getDataPathSetting().isNullOrEmpty()
        )
    }

    // Initialize the global images path resolver config on startup
    LaunchedEffect(repository) {
        com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = repository.getDataPathSetting()
    }

    val isDark = when (themeMode) {
        1 -> false // Light
        2 -> true  // Dark
        else -> androidx.compose.foundation.isSystemInDarkTheme() // System
    }

    GcTheme(darkTheme = isDark) {
        val backStack = remember { mutableStateListOf<Destination>(Destination.Dashboard) }
        val gcSceneStrategy = rememberGcSceneStrategy<NavKey>()
        var categoriesSettings by remember { mutableStateOf(repository.getCategorySettings()) }

        if (showSetupPrompt) {
            var dataPath by remember { mutableStateOf(repository.getDataPathSetting() ?: "") }

            AlertDialog(
                onDismissRequest = {}, // Non-dismissible
                containerColor = sysBackgroundColor(),
                titleContentColor = sysTextColor(),
                textContentColor = sysTextColor(),
                title = {
                    Text(
                        text = stringResource(Res.string.welcome_setup_title),
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.welcome_setup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = sysTextColor().copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Data path
                        Text(
                            text = stringResource(Res.string.data_dir_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = sysTextColor()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = if (dataPath.isEmpty()) stringResource(Res.string.not_configured) else dataPath,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sysTextColor(),
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = {
                                    val dir = com.gepetto.toydb.utils.selectDirectoryDialog("Select Data Folder")
                                    if (dir != null) {
                                        dataPath = dir
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(stringResource(Res.string.choose))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dataPath.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            disabledContentColor = sysTextColor().copy(alpha = 0.4f)
                        ),
                        onClick = {
                            repository.setDataPathSetting(dataPath)
                            com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = dataPath
                            
                            // Copy maker images
                            copyMakerImages(repository, dataPath)
                            
                            showSetupPrompt = false
                        }
                    ) {
                        Text(stringResource(Res.string.finish_setup_btn))
                    }
                }
            )
        }

        val currentChoice = when (val last = backStack.lastOrNull()) {
            is Destination.Dashboard -> "dashboard"
            is Destination.CategoryExplorer -> "explorer_${last.category}"
            is Destination.MakerDirectory -> "makers"
            is Destination.Settings -> "settings"
            is Destination.Info -> "info"
            else -> null
        }

        BoxWithConstraints {
            val isLandscape = maxWidth > maxHeight

        val buttons = remember(categoriesSettings, isLandscape) {
            val list = mutableListOf<GcNavButton>()
            list.add(
                GcNavButton(label = "Stats", imageVector = Icons.Default.Dashboard, navChoice = "dashboard", onClick = {
                    backStack.clear()
                    backStack.add(Destination.Dashboard)
                })
            )
            if (isLandscape) {
                categoriesSettings.forEach { setting ->
                    val icon = getIconByName(setting.icon)
                    list.add(
                        GcNavButton(
                            label = setting.label,
                            imageVector = icon,
                            navChoice = "explorer_${setting.category}",
                            onClick = {
                                backStack.clear()
                                backStack.add(Destination.CategoryExplorer(setting.category))
                            }
                        )
                    )
                }
            }
            list.add(
                GcNavButton(label = "Makers", imageVector = Icons.Default.Business, navChoice = "makers", onClick = {
                    backStack.clear()
                    backStack.add(Destination.MakerDirectory)
                })
            )
            list.add(
                GcNavButton(label = if (!isLandscape) "Setup" else "Settings", imageVector = Icons.Default.Settings, navChoice = "settings", onClick = {
                    backStack.clear()
                    backStack.add(Destination.Settings)
                })
            )
            list.add(
                GcNavButton(label = "Info", imageVector = Icons.Default.Info, navChoice = "info", onClick = {
                    backStack.clear()
                    backStack.add(Destination.Info)
                })
            )
            list
        }

        val showBackgroundImage = backStack.lastOrNull() is Destination.Dashboard

        GcE2eBox(
            imageResourceRes = if (showBackgroundImage) club.gepetto.composeutils.Res.drawable.gepetto else null,
            darkImageResourceRes = if (showBackgroundImage) club.gepetto.composeutils.Res.drawable.invertedgepetto else null,
            backgroundColor = sysBackgroundColor()
        ) {
            GcAdaptiveScaffold(
                modifier = modifier,
                navChoice = currentChoice,
                gcNavButtons = buttons,
                iconBackgroundColor = sysBackgroundColor(),
                content = { landscape ->
                    GcNavDisplay(
                        backStack = backStack,
                        sceneStrategies = listOf(gcSceneStrategy),
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<Destination.Dashboard> {
                                DashboardScreen(repository, onNavigate = { backStack.add(it) })
                            }
                            entry<Destination.CategoryExplorer> { key ->
                                ExplorerScreen(repository, category = key.category, onNavigate = { backStack.add(it) })
                            }
                            entry<Destination.MakerDirectory> {
                                MakerDirectoryScreen(repository, onNavigate = { backStack.add(it) })
                            }
                            entry<Destination.MakerDetail> { key ->
                                MakerDetailScreen(repository, makerName = key.makerName, onNavigate = { backStack.add(it) }, onBack = { backStack.removeLastOrNull() })
                            }
                            entry<Destination.ToyDetail>(
                                metadata = GcSceneStrategy.detailPane(resizeable = true)
                            ) { key ->
                                ToyDetailScreen(repository, toyType = key.toyType, refNum = key.refNum, onNavigate = { backStack.add(it) }, onBack = { backStack.removeUpToInclusive(key) })
                            }
                            entry<Destination.EditToy>(
                                metadata = GcSceneStrategy.bottomSheetPane()
                            ) { key ->
                                EditToyScreen(repository, toyType = key.toyType, refNum = key.refNum, onBack = { backStack.removeUpToInclusive(key) })
                            }
                            entry<Destination.AddToy>(
                                metadata = GcSceneStrategy.detailPane(resizeable = true)
                            ) { key ->
                                AddToyScreen(repository, category = key.category, onBack = { backStack.removeUpToInclusive(key) })
                            }
                            entry<Destination.AddMaker>(
                                metadata = GcSceneStrategy.detailPane(resizeable = true)
                            ) { key ->
                                AddMakerScreen(repository, onBack = { backStack.removeUpToInclusive(key) })
                            }
                            entry<Destination.EditMaker>(
                                metadata = GcSceneStrategy.bottomSheetPane()
                            ) { key ->
                                EditMakerScreen(repository, makerName = key.makerName, onBack = { backStack.removeUpToInclusive(key) })
                            }
                            entry<Destination.Settings> {
                                SettingsScreen(
                                    db = db,
                                    sftpService = sftpService,
                                    currentTheme = themeMode,
                                    onThemeChanged = { newTheme ->
                                        repository.setThemeSetting(newTheme)
                                        themeMode = newTheme
                                    },
                                    onCategoriesChanged = {
                                        categoriesSettings = repository.getCategorySettings()
                                    },
                                    onNavigate = { backStack.add(it) },
                                    onAppTitleChanged = { newTitle ->
                                        onAppTitleChanged?.invoke(newTitle)
                                    }
                                )
                            }
                            entry<Destination.Info> {
                                InfoScreen(onNavigateToSftpSetup = { backStack.add(Destination.SftpSetup) })
                            }
                            entry<Destination.SftpSetup> {
                                SftpSetupScreen(onBack = { backStack.removeUpToInclusive(Destination.SftpSetup) })
                            }
                        }
                    )
                }
            )
        }
        }
    }
}

fun getIconByName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name.lowercase()) {
        "car", "directionscar", "slot", "static" -> Icons.Default.DirectionsCar
        "train" -> Icons.Default.Train
        "build", "kit", "tool" -> Icons.Default.Build
        "category", "misc" -> Icons.Default.Category
        "brush" -> Icons.Default.Brush
        "toy", "toys" -> Icons.Default.Toys
        "star" -> Icons.Default.Star
        "game", "controller" -> Icons.Default.SportsEsports
        "palette" -> Icons.Default.Palette
        "extension", "puzzle" -> Icons.Default.Extension
        "robot", "smarttoy" -> Icons.Default.SmartToy
        else -> Icons.Default.Category
    }
}
