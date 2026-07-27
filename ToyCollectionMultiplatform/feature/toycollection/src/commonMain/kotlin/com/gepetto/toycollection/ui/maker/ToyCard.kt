package com.gepetto.toycollection.ui.maker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import club.gepetto.composeutils.textAsBitmap
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.ToyDataProvider
import com.gepetto.toycollection.dataproviders.ToyProvider
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.ui.common.images.SmallImage

@Composable
fun ToyCard(
    toy: Toy,
    collection: CollectionData,
    modifier: Modifier = Modifier,
    timeStamp: Long = 0L,
    onClick: () -> Unit,
) {
    val textColor = sysTextColor()

    Column(
        modifier = modifier.height(IntrinsicSize.Min).background(sysBackgroundColor())) {

        HorizontalDivider()

        Row(modifier = Modifier.clickable { onClick() }) {
            val maker = toy.getMaker(collection)
            var imageFile = toy.picture

            if (imageFile.isEmpty()) {
                if (maker != null)
                    imageFile = maker.picture
            }

            if (imageFile.isEmpty()) {
                SmallImage(
                    modifier = Modifier.align(Alignment.Top),
                    imageBitmap = textAsBitmap(text = toy.refNum, textColor = textColor.toArgb()),
                    fullImageOnClick = false,
                    onClick = onClick
                )
            } else {
                SmallImage(
                    modifier = Modifier.align(Alignment.Top),
                    imageFile = imageFile,
                    fullImageOnClick = false,
                    onClick = onClick
                )
            }
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = toy.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
                Text(
                    text = "${toy.scale} (${toy.refNum})",
                    fontSize = 12.sp,
                    color = textColor,
                )
                if (toy.traded.isNotEmpty()) {
                    Text(
                        text = "* gone *",
                        fontSize = 12.sp,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CardToyPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(ToyDataProvider::class) toyProvider: ToyProvider
) {
    Common.testInit()
    MakerDataProvider.init()
    GcTheme {
        Surface {
            Column {
                ToyCard(
                    modifier = Modifier.padding(8.dp),
                    toy = toyProvider.toy,
                    collection = MakerDataProvider.collectionDataDb,
                ) {}
            }
        }
    }
}
