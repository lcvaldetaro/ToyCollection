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
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.GcLog
import okio.FileSystem
import okio.Path.Companion.toPath
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import club.gepetto.composeutils.GcE2eBox
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.navbar.GcNavButton
import club.gepetto.composeutils.navigation3.GcNavDisplay
import club.gepetto.composeutils.navigation3.GcSceneStrategy
import club.gepetto.composeutils.navigation3.rememberGcSceneStrategy
import club.gepetto.composeutils.navigation3.removeUpToInclusive
import club.gepetto.composeutils.scaffold.GcAdaptiveScaffold
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
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
fun ToyDbNavigation(db: ToyDatabase, modifier: Modifier = Modifier) {
    val repository = remember { ToyRepository(db) }
    var themeMode by remember { mutableStateOf(repository.getThemeSetting()) }

    var showSetupPrompt by remember {
        mutableStateOf(
            isDesktopPlatform() && (
                repository.getImagesPathSetting().isNullOrEmpty() ||
                repository.getImportExportPathSetting().isNullOrEmpty()
            )
        )
    }

    // Initialize the global images path resolver config on startup
    LaunchedEffect(repository) {
        com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = repository.getImagesPathSetting()
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
            var importExportPath by remember { mutableStateOf(repository.getImportExportPathSetting() ?: "") }
            var imagesPath by remember { mutableStateOf(repository.getImagesPathSetting() ?: "") }

            AlertDialog(
                onDismissRequest = {}, // Non-dismissible
                containerColor = sysBackgroundColor(),
                titleContentColor = sysTextColor(),
                textContentColor = sysTextColor(),
                title = {
                    Text(
                        text = "Initial System Setup",
                        fontWeight = FontWeight.Bold,
                        color = sysTextColor()
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Please configure the required directory paths to initialize the application.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sysTextColor().copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Import/Export path
                        Text(
                            text = "Import / Export Directory",
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
                                    text = if (importExportPath.isEmpty()) "Not Configured" else importExportPath,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sysTextColor(),
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = {
                                    val dir = com.gepetto.toydb.utils.selectDirectoryDialog("Select Import/Export Folder")
                                    if (dir != null) {
                                        importExportPath = dir
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Choose")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Images path
                        Text(
                            text = "Images Directory",
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
                                    text = if (imagesPath.isEmpty()) "Not Configured" else imagesPath,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sysTextColor(),
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = {
                                    val dir = com.gepetto.toydb.utils.selectDirectoryDialog("Select Images Folder")
                                    if (dir != null) {
                                        imagesPath = dir
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Choose")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = importExportPath.isNotEmpty() && imagesPath.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            disabledContentColor = sysTextColor().copy(alpha = 0.4f)
                        ),
                        onClick = {
                            repository.setImportExportPathSetting(importExportPath)
                            repository.setImagesPathSetting(imagesPath)
                            com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = imagesPath
                            
                            // Copy maker images
                            copyMakerImages(repository, imagesPath)
                            
                            showSetupPrompt = false
                        }
                    ) {
                        Text("Finish Setup & Start")
                    }
                }
            )
        }

        val currentChoice = when (val last = backStack.lastOrNull()) {
            is Destination.Dashboard -> "dashboard"
            is Destination.CategoryExplorer -> "explorer_${last.category}"
            is Destination.MakerDirectory -> "makers"
            is Destination.Settings -> "settings"
            else -> null
        }

        val buttons = remember(categoriesSettings) {
            val list = mutableListOf<GcNavButton>()
            list.add(
                GcNavButton(label = "Stats", imageVector = Icons.Default.Dashboard, navChoice = "dashboard", onClick = {
                    backStack.clear()
                    backStack.add(Destination.Dashboard)
                })
            )
            categoriesSettings.forEach { setting ->
                val icon = when (setting.category) {
                    "slot" -> Icons.Default.DirectionsCar
                    "train" -> Icons.Default.Train
                    "static" -> Icons.Default.DirectionsCar
                    "kit" -> Icons.Default.Build
                    "misc" -> Icons.Default.Category
                    else -> Icons.Default.Category
                }
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
            list.add(
                GcNavButton(label = "Makers", imageVector = Icons.Default.Business, navChoice = "makers", onClick = {
                    backStack.clear()
                    backStack.add(Destination.MakerDirectory)
                })
            )
            list.add(
                GcNavButton(label = "Settings", imageVector = Icons.Default.Settings, navChoice = "settings", onClick = {
                    backStack.clear()
                    backStack.add(Destination.Settings)
                })
            )
            list
        }

        GcE2eBox {
            GcAdaptiveScaffold(
                modifier = modifier,
                navChoice = currentChoice,
                gcNavButtons = buttons,
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
                                    currentTheme = themeMode,
                                    onThemeChanged = { newTheme ->
                                        repository.setThemeSetting(newTheme)
                                        themeMode = newTheme
                                    },
                                    onCategoriesChanged = {
                                        categoriesSettings = repository.getCategorySettings()
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
