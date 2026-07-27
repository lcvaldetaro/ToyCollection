package com.gepetto.toycollection.ui.maker


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.CELL_SIZE
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.ToyListDataProvider
import com.gepetto.toycollection.dataproviders.ToyListProvider
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData

@Composable
fun ToysList(
    toys: List<Toy>,
    collection: CollectionData,
    modifier: Modifier = Modifier,
    timeStamp: Long = 0L,
    index: Int = 0,
    offset: Int = 0,
    showTradedToys: Boolean = false,
    multipleColumns: Boolean = true,
    onIntentCommand: (ListDetailsIntent) -> Unit,
) {
    val toysList = listOf<Toy>().toMutableList()
    val state = rememberLazyGridState(index, offset)



    LaunchedEffect(key1 = state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            /*onIntentCommand(
                CollectionIntent.SaveToyListScrollPosition(
                    index = state.firstVisibleItemIndex,
                    offset = state.firstVisibleItemScrollOffset
                )
            )*/
        }
    }

    for (toy in toys) {
        if (showTradedToys || toy.traded.isEmpty())
            toysList.add(toy)
    }

    BoxWithConstraints {
        val numOfColumns = if (multipleColumns) (this.maxWidth / CELL_SIZE).value.toInt() else 1
        if (numOfColumns > 0) {
            val columns = if (multipleColumns) GridCells.Fixed(numOfColumns) else GridCells.Fixed(numOfColumns)

            LazyVerticalGrid(
                state = state,
                modifier = modifier.background(sysBackgroundColor()),
                columns = columns,
                contentPadding = PaddingValues(8.dp),
                content = {
                    items(toysList.size) { index ->
                        ToyCard(
                            modifier = Modifier,
                            toy = toysList[index],
                            collection = collection,
                            onClick = { onIntentCommand(ListDetailsIntent.Tapped(ListDetailsTapAction.TapToy(toy = toysList[index]))) }
                        )
                    }
                    if ((toysList.size % numOfColumns) != 0) {
                        var i = toysList.size
                        while ((i % numOfColumns) != 0) {
                            item { Column { HorizontalDivider() } }
                            i++
                        }
                    }
                }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CardListPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(ToyListDataProvider::class) toyListProvider: ToyListProvider
) {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            ToysList(
                toys = toyListProvider.toyList,
                timeStamp = 0L,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CardListPreview1Column(
    @androidx.compose.ui.tooling.preview.PreviewParameter(ToyListDataProvider::class) toyListProvider: ToyListProvider
) {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            ToysList(
                toys = toyListProvider.toyList,
                timeStamp = 0L,
                multipleColumns = false,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CardListPreviewTablet(
    @androidx.compose.ui.tooling.preview.PreviewParameter(ToyListDataProvider::class) toyListProvider: ToyListProvider
) {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            ToysList(
                toys = toyListProvider.toyList,
                timeStamp = 0L,
                collection = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}
