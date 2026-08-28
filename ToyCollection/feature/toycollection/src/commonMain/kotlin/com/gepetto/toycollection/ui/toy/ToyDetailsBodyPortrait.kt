package com.gepetto.toycollection.ui.toy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.isYes

@Composable
fun ToyDetailsBodyPortrait (
    toy: Toy,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(sysBackgroundColor())) {
        ToyImage(toy = toy)

        if (toy.isSimilarPicture())
            ToyProperty(value = "Similar car picture")

        if (toy.repro.isYes())
            ToyProperty(value = "Reproduction")

        if (toy.boxed.isYes())
            ToyProperty(value = "Boxed")

        ToyProperty(value = toy.comments)
        ToyProperty(label = "chassis", value = "${toy.chassisMaker} ${toy.chassisType}")
        ToyProperty(label = "motor", value = "${toy.motorMaker} ${toy.motorDetails}")
        ToyProperty(label = "catalog", value = toy.catalogNumber)
    }
}

@Composable
private fun ToyProperty(
    value: String,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    val tint = sysForegroundColor()
    if (!value.equals("")) {
        Column(modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
            if (!label.equals("")) {
                Text(
                    text = label,
                    color = tint,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = value,
                color = tint,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Preview
@Composable
private fun ToyDetailsPreview(
) {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme{
        androidx.compose.material3.Surface {
            Column { ToyDetailsBodyPortrait(toy = com.gepetto.toycollection.dataproviders.ToyDataProvider.toy1) }
        }
    }
}
