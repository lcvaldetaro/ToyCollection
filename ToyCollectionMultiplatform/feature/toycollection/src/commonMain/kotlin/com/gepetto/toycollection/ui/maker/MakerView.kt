package com.gepetto.toycollection.ui.maker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.circumIntentProcessor
import club.gepetto.composeutils.navigation3.GcPaneType
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.GoBackEffect
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.MakerIntentProcessor
import com.gepetto.toycollection.intentprocessors.ListDetailsState
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Maker

@Composable
fun MakerView(
    maker: Maker,
    collectionIp: CollectionIntentProcessor,
    modifier: Modifier = Modifier,
    iproc: MakerIntentProcessor = circumIntentProcessor<MakerIntentProcessor>(),
    onEffect: (CircumEffect) -> Unit = {},
) {
    val state by iproc.collectState(initialState = ListDetailsState.Loaded(maker = maker))
    val adaptiveInfo = LocalGcAdaptiveInfo.current
    var launchFirstToy by remember { mutableStateOf(adaptiveInfo.type == GcPaneType.LIST && maker.toysList.isNotEmpty())}

    iproc.onEffectIssued { effect -> onEffect(effect) }

    if (launchFirstToy) {
        iproc.onIntentCommand(ListDetailsIntent.Tapped(ListDetailsTapAction.TapToy(maker.toysList.first())), state)
        launchFirstToy = false
    }

    when (state) {
        is ListDetailsState.Loaded -> {
            val loadedState = state as ListDetailsState.Loaded

            MakerContent(
                modifier = modifier,
                maker = maker,
                collection = collectionIp.getCurrentCollectionData()!!,
                currentSelectedToy = loadedState.toy,
                goBack = { onEffect(GoBackEffect) },
                onIntentCommand = { intent -> iproc.onIntentCommand(intent, state) }
            )
        }
        else -> Unit
    }
}