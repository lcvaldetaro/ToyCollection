package com.gepetto.toycollection.ui.edittoy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.tooling.preview.Preview

import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.ToyDataProvider
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction


@Composable
fun EditToyBodyWrapper(
    maker: Maker,
    modifier: Modifier = Modifier,
    toyInput: Toy? = null,
    initialSaveEnabledState: Boolean = false,
    secretData: Boolean = true,
    onToySaved: (Toy) -> Unit = {},
    onClickAction: (ListDetailsIntent.Tapped) -> Unit,
) {
    var toy by remember { mutableStateOf(if (toyInput != null) toyInput else Toy()) }
    var enableSave by remember { mutableStateOf(initialSaveEnabledState)}
    val oldToy = Toy()
    oldToy.makeEqual(toy)

    toy.normalizeData()
    toy.bodyMaker = maker.name

    Column (modifier.background(sysBackgroundColor())) {
        EditToyTitle(
            onClickAction = {
                if (it.tapAction is ListDetailsTapAction.TapBack) {
                    toy.makeEqual(oldToy)
                    toy.normalizeData()
                }
                if (it.tapAction is ListDetailsTapAction.TapSave) {
                    toy.normalizeData()
                    onToySaved(toy)
                }
                onClickAction(it)
            },
            saveEnabled = if (initialSaveEnabledState) true else enableSave,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            title = if (toyInput == null) "Add new toy" else "Edit toy"
        )

        EditToyBody(toy = toy, secretData = secretData, evaluate = { updatedToy ->
            toy = updatedToy
            enableSave = toy.shouldSaveBeEnabled()
        })

        // enableSave = toy.shouldSaveBeEnabled() // TODO may not be needed
    }
}

private fun Toy.shouldSaveBeEnabled () : Boolean {
    return description.isNotEmpty() && makerCombo.isNotEmpty() && scale.isNotEmpty()
}

@Preview
@Composable
private fun Preview() {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            EditToyBodyWrapper(maker = MakerDataProvider.monogram, toyInput = ToyDataProvider.toy1) {}
        }
    }
}
