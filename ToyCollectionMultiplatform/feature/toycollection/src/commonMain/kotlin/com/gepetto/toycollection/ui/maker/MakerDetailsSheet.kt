package com.gepetto.toycollection.ui.maker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.models.ToyCounts
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.ui.common.images.ImageListItem
import com.gepetto.toycollection.ui.common.images.MediumImage
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.common.Common


@Composable
fun MakerDetailsSheet (
    maker: Maker,
    modifier: Modifier = Modifier,
    //backCLicked: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column (modifier = modifier.background(sysBackgroundColor())) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                MakerTitle(maker = maker)
                MakerImage(maker = maker)
                MakerProperty(label = "Country: ", value = maker.country)
                MakerProperty(value = maker.comments)
                MakerProperty(label = "Totals:", value = ToyCounts.countSingleMakerToys(maker).toString() )

                if (maker.bitmapFiles != null && maker.bitmapFiles!!.size > 1) {
                    val files  = Array(maker.bitmapFiles!!.size) { ""}
                    var i = maker.bitmapFiles!!.size - 1
                    var j = 0
                    while (i >= 0) {
                        files.set(j++, maker.bitmapFiles!![i--])
                    }

                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        items(
                            items = files,
                            itemContent = {
                                ImageListItem(imageFile = it, files = files)
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun MakerImage(
    maker: Maker,
    modifier: Modifier = Modifier,
) {
    MediumImage(modifier = modifier, imageFile = maker.picture)
}

@Composable
private fun MakerProperty(
    value: String,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    if (!value.equals("")) {
        Column(modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
            if (!label.equals("")) {
                Text(
                    text = label,
                    color = sysTextColor(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                text = value,
                color = sysTextColor(),
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun MakerTitle(
    maker: Maker,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)) {
        Text(
            text = maker.name,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = sysTextColor(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerDetailsSheetPreview() {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            MakerDetailsSheet(maker = MakerDataProvider.estrela )
        }
    }
}



