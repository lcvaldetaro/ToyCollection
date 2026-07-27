package com.gepetto.toycollection.ui.toy

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import club.gepetto.composeutils.scaffold.GcTitleWithActionIcon
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Toy

@Composable
fun ToyTitleBar(
    toy: Toy,
    modifier: Modifier = Modifier,
    closeUsed: Boolean = true,
    onClickAction: (ListDetailsIntent.Tapped) -> Unit,
) {
    GcTitleWithActionIcon(
        modifier = modifier.background(sysBackgroundColor()),
        title = "${toy.description} (${toy.refNum})",
        backUsed = false,
        thinMode = true,
        closeUsed = if (LocalGcAdaptiveInfo.current.backButtonVisibility) closeUsed else false,
        closeClicked = { if (closeUsed) onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapBack)) }
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ToyTitlePreview() {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme{
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                ToyTitleBar(toy = com.gepetto.toycollection.dataproviders.ToyDataProvider.toy1) {}
            }
        }
    }
}