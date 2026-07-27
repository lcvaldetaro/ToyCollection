package com.gepetto.toycollection.ui.addtoy

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.composeutils.navigation3.LocalGcAdaptiveInfo
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.intentprocessors.SystemBackEffect
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.ui.edittoy.EditToyBodyWrapper

@Composable
fun AddToyView(
    maker: Maker,
    collectionIp: CollectionIntentProcessor,
    modifier: Modifier = Modifier,
    onEffect: (CircumEffect) -> Unit = {},
) {
    val adaptiveInfo = LocalGcAdaptiveInfo.current
    BackHandler(true) { onEffect(SystemBackEffect(adaptiveInfo)) }

    EditToyBodyWrapper(
        modifier = modifier,
        maker = maker,
    ) {
        when (it.tapAction) {
            is ListDetailsTapAction.TapBack -> onEffect(SystemBackEffect(adaptiveInfo))
            is ListDetailsTapAction.TapSave -> Unit // TODO
            else -> {}
        }
    }
}