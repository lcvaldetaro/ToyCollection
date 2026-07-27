package com.gepetto.toycollection.ui.maker


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.scaffold.GcFlipModalSheet
import club.gepetto.composeutils.scaffold.GcScaffold
import club.gepetto.composeutils.scaffold.GcTitleWithActionIcon
import club.gepetto.composeutils.scaffold.gcModalBottomSheetState
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.Common
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent.*
import com.gepetto.toycollection.intentprocessors.ListDetailsState
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Maker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)@Composable
fun MakerContent (
    maker: Maker,
    collection: CollectionData,
    modifier: Modifier = Modifier,
    timeStamp: Long = 0L,
    index: Int = 0,
    offset: Int = 0,
    currentSelectedToy: Toy? = null,
    goBack: () -> Unit = {},
    onIntentCommand: (ListDetailsIntent) -> Unit,
) {
    val modalBottomSheetState = gcModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val title = "${maker.name} from ${maker.country}"
    var selectedToy: Toy? by remember { mutableStateOf(currentSelectedToy) }
    var refresh: Long by remember { mutableStateOf(0L)}

    fun showBottomSheet() { coroutineScope.launch { GcFlipModalSheet(modalBottomSheetState) } }

    maker.normalizeData()



    GcScaffold(
        modifier = modifier,
        topBar = {
            Row(modifier = Modifier.clickable { showBottomSheet() }) {
                GcTitleWithActionIcon(
                    title = title,
                    actionIcon = Res.drawable.add,
                    thinMode = true,
                    actionIconClicked = { onIntentCommand(Tapped(ListDetailsTapAction.TapAddToy(maker = maker))) },
                    backClicked = { goBack() }
                )
            }
        },
        containerColor = sysBackgroundColor(),
        modalBottomSheetState = modalBottomSheetState,
        sheetContent = {
            MakerDetailsSheet(
                modifier = Modifier.background(sysBackgroundColor()),
                maker = maker,
            )
        },
        content = { paddingValues ->
            MakerToysListBody(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                maker = maker,
                collection = collection,
                index = index,
                offset = offset,
                multipleColumns = true,
                timeStamp = timeStamp + refresh,
                onIntentCommand = {
                    if (it is Tapped && it.tapAction is ListDetailsTapAction.TapToy) {
                        val toy = it.tapAction.toy
                        selectedToy = toy
                        onIntentCommand(SaveListDetailsState(toy, ListDetailsState.ShowToy))
                        onIntentCommand(Tapped(ListDetailsTapAction.TapToy(selectedToy!!)))
                    }
                    else
                    if (it is Tapped && it.tapAction is ListDetailsTapAction.TapBack) {
                        goBack()
                    }
                    else
                        onIntentCommand(it)
                }
            )
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerToysScreenPreview() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            MakerContent(
                maker = MakerDataProvider.cox,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerToysScreenPreviewLandscape() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            MakerContent(
                maker = MakerDataProvider.cox,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerToysScreenPreviewTablet() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            MakerContent(
                maker = MakerDataProvider.cox,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}