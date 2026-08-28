package com.gepetto.toycollection.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.isDark
import com.gepetto.toycollection.intentprocessors.HomeIntent
import com.gepetto.toycollection.intentprocessors.HomeTapAction
import club.gepetto.composeutils.scaffold.GcDrawerItem
import com.gepetto.common.Common
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*

@Composable
fun HomeDrawer(
    onAboutClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onIntentCommand: (HomeIntent) -> Unit,
) {
    val iconResource = if (isDark()) Res.drawable.invertedgepetto else Res.drawable.gepetto

    Column(modifier = modifier) {
        GcDrawerItem(
            text = "About ${Common.appName}",
            contentDescription = "About ${Common.appName}",
            iconResourceRes = iconResource,
        ) {
            onAboutClicked()
        }
        GcDrawerItem(
            text = "Privacy Policy",
            iconResourceRes = iconResource,
        ) {
            onIntentCommand(HomeIntent.Tapped(HomeTapAction.TapPrivacyPolicy))
        }
    }
}
