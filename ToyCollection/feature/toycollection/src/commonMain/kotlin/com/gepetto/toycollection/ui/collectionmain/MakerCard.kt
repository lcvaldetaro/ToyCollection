package com.gepetto.toycollection.ui.collection.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.models.ToyCounts
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.ui.common.images.SmallImage
import club.gepetto.composeutils.GcCard
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysTextColor
import club.gepetto.composeutils.textAsBitmap
import com.gepetto.common.Common
import club.gepetto.composeutils.gcCurrentTimeMillis

@Composable
fun MakerCard(
    maker: Maker,
    timeStamp: Long,
    modifier: Modifier = Modifier,
    onUpdate: (Long) -> Unit = {},
    onClick: () -> Unit,
) {
    val textColor = sysTextColor()
    GcCard(
        modifier = modifier.clickable { onClick() },
    ) {
        Row {
            if (maker.picture.isEmpty()) {
                SmallImage(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    imageBitmap = textAsBitmap(text = maker.name, textColor = textColor.toArgb()),
                    fullImageOnClick = false,
                    onClick = onClick
                )
            }
            else {
                SmallImage(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    imageFile = maker.picture,
                    fullImageOnClick = false,
                    onUpdate = { onUpdate(gcCurrentTimeMillis()) },
                    onClick = onClick
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = maker.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
                Text(
                    text = maker.country,
                    fontSize = 12.sp,
                    color = textColor,
                )
                val toyCounts = ToyCounts.countSingleMakerToys(maker)
                Text(
                    text = "${toyCounts.total} models",
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MakerCardPreview() {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            MakerCard(
                maker = MakerDataProvider.cox,
                timeStamp = 0L,
            ) {}
        }
    }
}



