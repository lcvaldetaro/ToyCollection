package com.gepetto.toydb.ui

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
import club.gepetto.GcLog
import club.gepetto.composeutils.GcMarkdown
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.CommonConfig
import toydb.composeapp.generated.resources.Res

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    var aboutMarkdown by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            aboutMarkdown = Res.readBytes("files/about.md").decodeToString()
        } catch (e: Exception) {
            GcLog.e("InfoScreen", "Failed to load about.md: ${e.message}", e)
            aboutMarkdown = "Failed to load application information."
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = sysBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gepetto Toy Database Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = sysTextColor(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version ${CommonConfig.versionName} (${CommonConfig.versionCode})",
                style = MaterialTheme.typography.bodyMedium,
                color = sysTextColor().copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Copyright © 2026 Valdetaro Consulting, LLC DBA Gepetto Club\nAll rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = sysTextColor().copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            if (aboutMarkdown.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    GcMarkdown(
                        content = aboutMarkdown,
                        textColor = sysTextColor()
                    )
                }
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
