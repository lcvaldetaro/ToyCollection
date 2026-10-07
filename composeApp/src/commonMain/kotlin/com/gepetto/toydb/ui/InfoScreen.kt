package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.GcLog
import club.gepetto.composeutils.GcMarkdown
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import com.gepetto.toydb.CommonConfig
import com.gepetto.toydb.utils.isDesktopPlatform
import com.gepetto.toydb.utils.isWebPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import toydb.composeapp.generated.resources.*

enum class InfoTopic(
    val id: String,
    val titleRes: StringResource
) {
    ABOUT("about", Res.string.info_tab_about),
    GENERAL("general", Res.string.tab_general),
    BACKUP_RESTORE("backup_restore", Res.string.backup_title),
    SERVER_SYNC("server_sync", Res.string.info_tab_backup),
    CATEGORIES("categories", Res.string.categories),
    PRIVACY("privacy", Res.string.info_tab_privacy),
    TERMS("terms", Res.string.info_tab_terms)
}

@Composable
fun InfoScreen(
    onNavigateToSftpSetup: () -> Unit = {},
    initialTopicId: String? = null,
    selectedLanguage: String = "",
    modifier: Modifier = Modifier
) {
    val topics = InfoTopic.entries
    val initialIndex = remember(initialTopicId) {
        if (initialTopicId != null) {
            val idx = topics.indexOfFirst {
                it.id == initialTopicId || (initialTopicId == "backup" && it.id == "server_sync")
            }
            if (idx != -1) idx else 0
        } else 0
    }
    var selectedTopicIndex by remember(initialIndex) { mutableStateOf(initialIndex) }
    LaunchedEffect(initialTopicId) {
        if (initialTopicId != null) {
            val idx = topics.indexOfFirst {
                it.id == initialTopicId || (initialTopicId == "backup" && it.id == "server_sync")
            }
            if (idx != -1) {
                selectedTopicIndex = idx
            }
        }
    }
    val currentTopic = topics.getOrElse(selectedTopicIndex) { topics[0] }

    val effectiveLang = if (selectedLanguage.isNotEmpty()) {
        selectedLanguage
    } else {
        try {
            com.gepetto.toydb.platform.LocaleHelper.getSystemLanguageCode()
        } catch (_: Throwable) {
            "en"
        }
    }

    val currentLang = remember(effectiveLang) {
        when (effectiveLang.lowercase().take(2)) {
            "pt", "es", "it", "de", "fr" -> effectiveLang.lowercase().take(2)
            else -> "en"
        }
    }

    var topicContent by remember(currentTopic.id, currentLang) { mutableStateOf("") }
    var isLoading by remember(currentTopic.id, currentLang) { mutableStateOf(true) }

    LaunchedEffect(currentTopic.id, currentLang) {
        isLoading = true
        val baseName = when (currentTopic) {
            InfoTopic.ABOUT -> "about"
            InfoTopic.GENERAL -> "general"
            InfoTopic.BACKUP_RESTORE -> "backup_restore"
            InfoTopic.SERVER_SYNC -> "server_sync"
            InfoTopic.CATEGORIES -> "categories"
            InfoTopic.PRIVACY -> "privacypolicy"
            InfoTopic.TERMS -> "terms"
        }
        try {
            val rawContent = withContext(Dispatchers.Default) {
                try {
                    Res.readBytes("files/${currentLang}_${baseName}.md").decodeToString()
                } catch (_: Exception) {
                    Res.readBytes("files/en_${baseName}.md").decodeToString()
                }
            }
            topicContent = filterPlatformContent(rawContent)
        } catch (e: Exception) {
            GcLog.e("InfoScreen", "Failed to load ${baseName}.md: ${e.message}", e)
            topicContent = when (currentTopic) {
                InfoTopic.ABOUT -> getString(Res.string.failed_load_about)
                InfoTopic.GENERAL,
                InfoTopic.BACKUP_RESTORE,
                InfoTopic.CATEGORIES -> getString(Res.string.failed_load_help)
                InfoTopic.SERVER_SYNC -> getString(Res.string.failed_load_sftp_guide)
                InfoTopic.PRIVACY -> getString(Res.string.failed_load_privacy)
                InfoTopic.TERMS -> getString(Res.string.failed_load_terms)
            }
        } finally {
            isLoading = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = sysBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTopicIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = sysForegroundColor()
            ) {
                topics.forEachIndexed { index, topic ->
                    Tab(
                        selected = selectedTopicIndex == index,
                        onClick = { selectedTopicIndex = index },
                        text = {
                            Text(
                                text = stringResource(topic.titleRes),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sysForegroundColor().copy(alpha = 0.15f))
            ) {
                key(currentTopic.id) {
                    when (currentTopic) {
                        InfoTopic.ABOUT -> AboutTabContent(
                            aboutText = topicContent,
                            isLoading = isLoading
                        )
                        InfoTopic.GENERAL -> MarkdownTabContent(
                            title = stringResource(Res.string.tab_general),
                            content = topicContent,
                            isLoading = isLoading
                        )
                        InfoTopic.BACKUP_RESTORE -> MarkdownTabContent(
                            title = stringResource(Res.string.backup_title),
                            content = topicContent,
                            isLoading = isLoading
                        )
                        InfoTopic.SERVER_SYNC -> ServerSyncTabContent(
                            guideText = topicContent,
                            isLoading = isLoading,
                            onNavigateToSftpSetup = onNavigateToSftpSetup
                        )
                        InfoTopic.CATEGORIES -> MarkdownTabContent(
                            title = stringResource(Res.string.categories),
                            content = topicContent,
                            isLoading = isLoading
                        )
                        InfoTopic.PRIVACY -> MarkdownTabContent(
                            title = stringResource(Res.string.info_tab_privacy),
                            content = topicContent,
                            isLoading = isLoading
                        )
                        InfoTopic.TERMS -> MarkdownTabContent(
                            title = stringResource(Res.string.info_tab_terms),
                            content = topicContent,
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutTabContent(
    aboutText: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = sysForegroundColor(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.version_format, CommonConfig.versionName, CommonConfig.versionCode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = sysForegroundColor().copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.copyright) + stringResource(Res.string.rights_reserved),
            style = MaterialTheme.typography.bodySmall,
            color = sysForegroundColor().copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = sysForegroundColor().copy(alpha = 0.15f))
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            if (aboutText.isNotEmpty()) {
                key(aboutText) {
                    GcMarkdown(
                        content = aboutText,
                        textColor = sysForegroundColor()
                    )
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ServerSyncTabContent(
    guideText: String,
    isLoading: Boolean,
    onNavigateToSftpSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(Res.string.info_tab_backup),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = sysForegroundColor()
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = sysForegroundColor().copy(alpha = 0.15f))
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            if (guideText.isNotEmpty()) {
                key(guideText) {
                    Column {
                        GcMarkdown(
                            content = guideText,
                            textColor = sysForegroundColor()
                        )
                        if (!isWebPlatform()) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateToSftpSetup,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(Res.string.sftp_setup_guide_btn),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MarkdownTabContent(
    title: String,
    content: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = sysForegroundColor()
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = sysForegroundColor().copy(alpha = 0.15f))
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            if (content.isNotEmpty()) {
                key(content) {
                    GcMarkdown(
                        content = content,
                        textColor = sysForegroundColor()
                    )
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun InfoScreenPreview() {
    GcTheme {
        InfoScreen()
    }
}

internal fun filterPlatformContent(
    content: String,
    isDesktop: Boolean = isDesktopPlatform(),
    isWeb: Boolean = isWebPlatform()
): String {
    val isAndroid = !isDesktop && !isWeb
    val platformMap = mapOf(
        "desktop" to isDesktop,
        "!desktop" to !isDesktop,
        "web" to isWeb,
        "!web" to !isWeb,
        "android" to isAndroid,
        "!android" to !isAndroid
    )

    val pattern = Regex("""(?:^[ \t]*)?<!--\s*(!?[a-zA-Z]+)\s*-->[ \t]*(?:\r?\n)?([\s\S]*?)(?:^[ \t]*)?<!--\s*/\1\s*-->[ \t]*(?:\r?\n)?""", RegexOption.MULTILINE)
    var prev: String? = null
    var curr = content
    while (prev != curr) {
        prev = curr
        curr = pattern.replace(curr) { matchResult ->
            val tag = matchResult.groupValues[1].lowercase()
            val inner = matchResult.groupValues[2]
            val active = platformMap[tag] ?: true
            if (active) inner else ""
        }
    }
    return curr.replace(Regex("""\n{3,}"""), "\n\n").trim()
}

