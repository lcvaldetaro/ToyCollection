package com.gepetto.toycollection.ui.toy

import club.gepetto.composeutils.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.isLandscape
import club.gepetto.composeutils.navigation3.GcPaneType
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData

@Composable
fun ToyDetailsBodyWrapper (
    toy: Toy,
    collection: CollectionData,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    enableBack: Boolean = true,
    onClickAction: (ListDetailsIntent.Tapped) -> Unit,
) {
    val adaptiveInfo = LocalGcAdaptiveInfo.current
    BackHandler(enableBack) { onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapBack)) }

    BoxWithConstraints(modifier) {
        val landscape = this.isLandscape()
        Column {
            if (showTitle)
                ToyTitleBar(
                    closeUsed = if (adaptiveInfo.type != GcPaneType.BOTTOMSHEET) true else false,
                    toy = toy,
                    onClickAction = onClickAction,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (landscape)
                    ToyDetailsBodyLandscape(toy)
                else
                    ToyDetailsBodyPortrait(toy)

                ToyDetailsCarousel(toy = toy, collection = collection)
                ToyNavBar(toy = toy, collection = collection, onClickAction = onClickAction)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ToyDetailsPreview(
) {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme{
        androidx.compose.material3.Surface {
            Column {
                ToyDetailsBodyWrapper(
                    toy = com.gepetto.toycollection.dataproviders.ToyDataProvider.toy1,
                    collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                ) {}
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ToyDetailsPreviewLandscape(
) {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme{
        androidx.compose.material3.Surface {
            Column {
                ToyDetailsBodyWrapper(
                    toy = com.gepetto.toycollection.dataproviders.ToyDataProvider.toy1,
                    collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                ) {}
            }
        }
    }
}
