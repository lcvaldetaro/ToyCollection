package com.gepetto.toycollection.ui.privacypolicy

import club.gepetto.composeutils.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.circum.CircumEffect
import club.gepetto.composeutils.scaffold.GcScaffold
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import org.jetbrains.compose.resources.painterResource
import com.gepetto.common.Common
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import club.gepetto.composeutils.GcMarkdown
import com.gepetto.toycollection.intentprocessors.GoBackEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyView(
    modifier: Modifier = Modifier,
    onEffect: (CircumEffect) -> Unit = {},
) {
    BackHandler(true) { onEffect(GoBackEffect) }

    val tint = sysTextColor()
    val background = TopAppBarDefaults.topAppBarColors().containerColor

    GcScaffold(
        modifier = modifier.background(sysBackgroundColor()),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = background),
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.backarrow),
                            contentDescription = "Back",
                            tint = tint,
                            modifier = Modifier.size(24.dp).clickable { onEffect(GoBackEffect) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Privacy Policy for ${Common.appName}",
                            color = tint,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
            )
        },
        sheetContent = {},
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                GcMarkdown(
                    content = Common.privacyPolicyMarkdown,
                    textColor = sysForegroundColor()
                )
            }
        }
    )
}