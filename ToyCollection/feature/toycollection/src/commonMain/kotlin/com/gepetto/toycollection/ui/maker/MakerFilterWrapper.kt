package com.gepetto.toycollection.ui.maker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcFilterButton
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Maker
import club.gepetto.GcLog

@Composable
fun MakerFilterWrapper(
    maker: Maker,
    makersDb: CollectionData,
    modifier: Modifier = Modifier,
    index: Int = 0,
    offset: Int = 0,
    multipleColumns: Boolean = false,
    onIntentCommand: (ListDetailsIntent) -> Unit,
) {
    val selection = remember { mutableStateOf("All")}
    val scales = maker.scales

    GcLog.d("scales = ${maker.scales.getListOfScales().size}")
    maker.scales.getListOfScales().forEach {
        GcLog.d("scale = ${it}")
    }
    Column (modifier = modifier.fillMaxWidth().background((sysBackgroundColor()))) {
        Row (modifier
            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp)
            .align(Alignment.CenterHorizontally)
        ) {
            GcFilterButton("All", selection.value) { selection.value = it }
            scales.getListOfScales().forEach {
                if (it.isNotEmpty()) //band aid
                    GcFilterButton(it, selection.value, modifier = Modifier.padding(start = 2.dp)) { selection.value = it}
            }
        }
        Column {
            ToysList(
                toys = scales.getScaleList(selection.value),
                collection = makersDb,
                timeStamp = 0L,
                index = index,
                offset = offset,
                multipleColumns = multipleColumns,
                onIntentCommand = onIntentCommand,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerFilterPreview() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            MakerFilterWrapper(
                maker = MakerDataProvider.monogram,
                makersDb = MakerDataProvider.collectionDataDb,
            ) {}
        }
    }
}



