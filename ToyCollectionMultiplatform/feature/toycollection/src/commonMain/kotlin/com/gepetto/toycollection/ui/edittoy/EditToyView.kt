package com.gepetto.toycollection.ui.edittoy

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.circumIntentProcessor
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import com.gepetto.toycollection.intentprocessors.CloseCurrentExtraPaneEffect
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.intentprocessors.SystemBackEffect
import com.gepetto.toycollection.intentprocessors.ToyIntentProcessor
import com.gepetto.toycollection.intentprocessors.ToyState
import com.gepetto.toycollection.models.Toy

@Composable
fun EditToyView(
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
            EditToyBodyWrapper(
                modifier = modifier,
                maker = toy.getMaker(
                    collectionIp.getCurrentCollectionData()
                        ?: collectionIp.getCollectionDataForToy(toy)!!
                )!!,
                toyInput = toy
            ) {
                when (it.tapAction) {
                    is ListDetailsTapAction.TapBack -> onEffect(CloseCurrentExtraPaneEffect)
                    is ListDetailsTapAction.TapSave -> Unit // TODO
                    else -> {}
                }
            }
        }
        else -> Unit
    }
}