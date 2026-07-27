package com.gepetto.toycollection.ui.search

import club.gepetto.composeutils.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.ui.maker.ToysList

@Composable
fun SearchScreenBody(
    collection: CollectionData,
    searchString: String,
    modifier: Modifier = Modifier,
    multipleColumns: Boolean = true,
    timeStamp: Long = 0L,
    index: Int = 0,
    offset: Int = 0,
    onIntentCommand: (ListDetailsIntent) -> Unit,
) {
    val searchStr = remember { mutableStateOf(searchString) }
    BackHandler(true) { onIntentCommand(ListDetailsIntent.Tapped(ListDetailsTapAction.TapBack)) }

    Column(modifier) {
        SearchInput(
            searchString = searchStr.value,
            searchStringUpdated = {
                searchStr.value = it
                onIntentCommand(ListDetailsIntent.Tapped(ListDetailsTapAction.UpdateSearchString(searchStr.value)))
            },
        )
        val searchList = collection.contains(searchStr.value)
        val total = searchList.size
        if (total > 0) {
            Text(
                text = "Search results for \"${searchStr.value}\" - ${total} models.",
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            ToysList(
                toys = searchList,
                showTradedToys = true,
                collection = collection,
                multipleColumns = multipleColumns,
                timeStamp = timeStamp,
                index = index,
                offset = offset,
                onIntentCommand = onIntentCommand
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme {
        androidx.compose.material3.Surface {
            SearchScreenBody(
                collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                searchString = "196",
            ) {}
        }
    }
}