package com.gepetto.toycollection.ui.maker

import club.gepetto.composeutils.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Maker

@Composable
fun MakerToysListBody(
    maker: Maker,
    collection: CollectionData,
    offset: Int,
    index: Int,
    timeStamp: Long,
    onIntentCommand: (ListDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
    multipleColumns: Boolean = true,
) {
    BackHandler(true) { onIntentCommand(ListDetailsIntent.Tapped(ListDetailsTapAction.TapBack)) }
    Column(modifier.fillMaxHeight()) {
        if (maker.scales.getListOfScales().size == 1) {
            ToysList(
                toys = maker.toysList,
                collection = collection,
                index = index,
                offset = offset,
                timeStamp = timeStamp,
                multipleColumns = multipleColumns,
                onIntentCommand = onIntentCommand
            )
        } else {
            MakerFilterWrapper(
                maker = maker,
                makersDb = collection,
                onIntentCommand = onIntentCommand,
                index = index,
                offset = offset,
                multipleColumns = multipleColumns
            )
        }
    }
}