package com.gepetto.toycollection.ui.edittoy

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import club.gepetto.composeutils.isDark
import club.gepetto.composeutils.navigation3.GcPaneType
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import club.gepetto.composeutils.scaffold.GcTitleWithActionIcon
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*

@Composable
fun EditToyTitle(
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    title: String = "",
    onClickAction: (ListDetailsIntent.Tapped) -> Unit,
) {
    GcTitleWithActionIcon(
        modifier = modifier.background(sysBackgroundColor()),
        title = title,
        backUsed = false,
        closeUsed = LocalGcAdaptiveInfo.current.type != GcPaneType.BOTTOMSHEET && LocalGcAdaptiveInfo.current.backButtonVisibility || LocalGcAdaptiveInfo.current.pane == 3,
        actionIcon = if (isDark()) Res.drawable.saveicondark else Res.drawable.saveicon,
        disabledActionIcon = Res.drawable.saveicondisabled,
        actionIconEnabled = saveEnabled,
        actionIconClicked = { onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapSave)) },
        closeClicked = { onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapBack)) }
    )
}

@Preview
@Composable
private fun PreviewEnabled() {
    GcTheme {
        Surface {
            EditToyTitle(title = "title", saveEnabled = true) {}
        }
    }
}

@Preview
@Composable
private fun PreviewDisabled() {
    GcTheme {
        Surface {
            EditToyTitle(title = "title", saveEnabled = false) {}
        }
    }
}