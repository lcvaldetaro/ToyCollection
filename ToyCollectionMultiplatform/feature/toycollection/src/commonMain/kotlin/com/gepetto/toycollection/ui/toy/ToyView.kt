package com.gepetto.toycollection.ui.toy

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.circumIntentProcessor
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.GoToEditToyEffect
import com.gepetto.toycollection.intentprocessors.GoToWebPageEffect
import com.gepetto.toycollection.intentprocessors.GoToWebSearchEffect
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.intentprocessors.SystemBackEffect
import com.gepetto.toycollection.intentprocessors.ToyIntentProcessor
import com.gepetto.toycollection.intentprocessors.ToyState
import com.gepetto.toycollection.models.Toy

@Composable
fun ToyView(
    toy: Toy,
    collectionIp: CollectionIntentProcessor,
    modifier: Modifier = Modifier,
    iproc: ToyIntentProcessor = circumIntentProcessor<ToyIntentProcessor>(),
    onEffect: (CircumEffect) -> Unit = {},
) {
    val state by iproc.collectState(initialState = ToyState.Loaded())

    val adaptiveInfo = LocalGcAdaptiveInfo.current
    BackHandler(true) { onEffect(SystemBackEffect(adaptiveInfo)) }

    when (state) {
        is ToyState.Loaded -> {
            ToyDetailsBodyWrapper (
                modifier = modifier,
                toy = toy,
                collection = collectionIp.getCurrentCollectionData()!!,
            ) {
                when (it.tapAction) {
                    is ListDetailsTapAction.TapBack -> onEffect(SystemBackEffect(adaptiveInfo))
                    is ListDetailsTapAction.TapEditToy -> onEffect(GoToEditToyEffect(toy))
                    is ListDetailsTapAction.TapWebPage -> onEffect(GoToWebPageEffect(toy))
                    is ListDetailsTapAction.TapWebSearch -> onEffect(GoToWebSearchEffect(toy))
                    else -> {}
                }
            }
        }
        else -> Unit
    }
}