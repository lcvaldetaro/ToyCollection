package com.gepetto.toycollection.ui.collection.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcTheme
import com.gepetto.common.CELL_SIZE
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.dataproviders.MakerDataProvider.Companion.cox
import com.gepetto.toycollection.dataproviders.MakerDataProvider.Companion.estrela
import com.gepetto.toycollection.dataproviders.MakerDataProvider.Companion.monogram
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import com.gepetto.toycollection.intentprocessors.CollectionTapAction
import com.gepetto.toycollection.models.Maker

@Composable
fun MakersList(
    makers: List<Maker>,
    timeStamp: Long,
    modifier : Modifier = Modifier,
    index: Int = 0,
    offset: Int = 0,
    onIntentCommand: (CollectionIntent) -> Unit,
) {
    val listMakers = ArrayList<Maker>(0)
    val lazyListState = rememberLazyGridState(index, offset)

    for (maker in makers) {
        if (maker.hasToys)
            listMakers.add(maker)
    }

    BoxWithConstraints {
        val numOfColumns = (this.maxWidth / CELL_SIZE).value.toInt()
        if (numOfColumns > 0) {
            val columns = GridCells.Fixed(numOfColumns)
            val localDensity = LocalDensity.current
            var columnHeight by remember { mutableStateOf(0) }
            var columnHeightDp by remember { mutableStateOf(0.dp) }

            LazyVerticalGrid(
                state = lazyListState,
                modifier = modifier,
                columns = columns,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                content = {
                    items(listMakers.size) { index ->
                        val maker = listMakers[index]
                        val localModifier =
                            if (columnHeight > 0) Modifier.height(columnHeightDp) else Modifier
                        if (maker.hasToys) {
                            MakerCard(
                                modifier = localModifier
                                    .onGloballyPositioned {
                                        if (it.size.height > columnHeight) {
                                            columnHeight = it.size.height
                                            columnHeightDp = with(localDensity) { columnHeight.toDp() }
                                        }
                                    },
                                maker = maker,
                                timeStamp = timeStamp,
                                onClick = { onIntentCommand(CollectionIntent.Tapped(tapAction = CollectionTapAction.TapMaker(maker = maker))) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            MakersList(
                makers = listOf(cox, monogram, estrela),
                timeStamp = 0L
            ) {}
        }
    }
}
