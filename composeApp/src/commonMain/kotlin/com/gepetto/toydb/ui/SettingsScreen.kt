package com.gepetto.toydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.SftpService
import com.gepetto.toydb.utils.ImageResolverConfig
import com.gepetto.toydb.utils.isWebPlatform
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

enum class SettingsTab(val id: String, val titleRes: StringResource) {
    GENERAL("general", Res.string.tab_general),
    BACKUP_RESTORE("backup_restore", Res.string.backup_title),
    SERVER_SYNC("server_sync", Res.string.info_tab_backup),
    CATEGORIES("categories", Res.string.categories)
}

@Composable
fun SettingsScreen(
    db: ToyDatabase,
    sftpService: SftpService,
    currentTheme: Int,
    onThemeChanged: (Int) -> Unit,
    currentLanguage: String = "",
    onLanguageChanged: (String) -> Unit = {},
    onCategoriesChanged: () -> Unit = {},
    onNavigate: (Destination) -> Unit = {},
    onAppTitleChanged: (String) -> Unit = {},
    onCollectionRestored: () -> Unit = {},
    initialTab: SettingsTab = SettingsTab.GENERAL,
    modifier: Modifier = Modifier
) {
    val repository = remember(db) { ToyRepository(db) }
    var appTitle by remember { mutableStateOf(repository.getAppTitleSetting()) }
    var dataPath by remember { mutableStateOf(repository.getDataPathSetting()) }
    var restoreKey by remember { mutableStateOf(0) }

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val tabs = SettingsTab.entries
    val selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        lazyListState.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Header: Title and Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GcSpacing.Standard, vertical = GcSpacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = sysTextColor()
                )
                IconButton(
                    onClick = {
                        val topicId = when (selectedTab) {
                            SettingsTab.GENERAL -> "general"
                            SettingsTab.BACKUP_RESTORE -> "backup_restore"
                            SettingsTab.SERVER_SYNC -> "server_sync"
                            SettingsTab.CATEGORIES -> "categories"
                        }
                        onNavigate(Destination.Info(topicId))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(Res.string.help_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = GcSpacing.Standard,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = sysForegroundColor(),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = stringResource(tab.titleRes),
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(GcSpacing.Standard)
                ) {
                    item {
                        key(restoreKey) {
                            when (selectedTab) {
                                SettingsTab.GENERAL -> {
                                    GeneralSettingsTab(
                                        currentTheme = currentTheme,
                                        onThemeChanged = onThemeChanged,
                                        currentLanguage = currentLanguage,
                                        onLanguageChanged = onLanguageChanged,
                                        appTitle = appTitle,
                                        onAppTitleChanged = { newTitle ->
                                            repository.setAppTitleSetting(newTitle)
                                            appTitle = newTitle
                                            onAppTitleChanged(newTitle)
                                        },
                                        dataPath = dataPath,
                                        onDataPathChanged = { newPath ->
                                            repository.setDataPathSetting(newPath)
                                            ImageResolverConfig.imagesPath = newPath
                                            dataPath = newPath
                                        }
                                    )
                                }

                                SettingsTab.BACKUP_RESTORE -> {
                                    if (!isWebPlatform()) {
                                        BackupRestoreCard(
                                            db = db,
                                            dataPath = dataPath,
                                            onCollectionRestored = {
                                                appTitle = repository.getAppTitleSetting()
                                                dataPath = repository.getDataPathSetting()
                                                restoreKey++
                                                onCollectionRestored()
                                            },
                                            onSetStatus = {}
                                        )
                                    } else {
                                        WebLocalDataNotice()
                                    }
                                }

                                SettingsTab.SERVER_SYNC -> {
                                    ServerSyncSettingsTab(
                                        db = db,
                                        sftpService = sftpService,
                                        dataPath = dataPath,
                                        onCategoriesChanged = onCategoriesChanged
                                    )
                                }

                                SettingsTab.CATEGORIES -> {
                                    CategoriesSettingsTab(
                                        db = db,
                                        onCategoriesChanged = onCategoriesChanged
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
                PlatformScrollbar(
                    state = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}
