package com.gepetto.toycollection.ui.web

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.composeutils.navigation3.GcPaneType
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import club.gepetto.composeutils.webpage.GcWebPageScreen
import com.gepetto.toycollection.intentprocessors.CloseCurrentExtraPaneEffect
import com.gepetto.toycollection.intentprocessors.SystemBackEffect

@Composable
fun WebPageView(
    url: String,
    modifier: Modifier = Modifier,
    title: String = "",
    onEffect: (CircumEffect) -> Unit,
) {
    val adaptiveInfo = LocalGcAdaptiveInfo.current
    BackHandler(true) { onEffect(SystemBackEffect(adaptiveInfo)) }

    GcWebPageScreen(
        closeIcon = if (adaptiveInfo.type != GcPaneType.BOTTOMSHEET && (adaptiveInfo.backButtonVisibility || adaptiveInfo.pane == 3)) true else false,
        modifier = modifier,
        url = url,
        title = title,
        backIcon = false,
        onBackClick = { onEffect(CloseCurrentExtraPaneEffect) }
    )
}