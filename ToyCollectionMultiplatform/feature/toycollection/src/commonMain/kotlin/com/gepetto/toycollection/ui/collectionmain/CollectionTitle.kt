package com.gepetto.toycollection.ui.collectionmain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.image.GcImage
import club.gepetto.composeutils.isSystemInLandscape
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.common.Common
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import com.gepetto.toycollection.intentprocessors.CollectionTapAction

@Composable
fun CollectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    onIntentCommand: (CollectionIntent) -> Unit,
) {
    val tint = sysTextColor()
    val iconSize = 36.dp
    val fontSize = 14.sp
    val startPadding = if (isSystemInLandscape()) 24.dp else 30.dp

    Row (modifier
        .background(sysBackgroundColor())
        .padding(start = startPadding, end = 16.dp)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /*if (isAndroidPlatform)
            GcImage(
                imageResourceRes = Res.drawable.poweroff,
                contentDescription = "Exit",
                size = iconSize,
                onClick = { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapBack)) },
            )

         */

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f)
                .clickable { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapAbout)) }
                .padding(end = 8.dp),
        )
        Icon(
            painter = painterResource(Res.drawable.gepetto_refresh),
            contentDescription = "Refresh",
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .padding(6.dp)
                .clickable { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapRefresh)) },
        )
        Icon(
            painter = painterResource(Res.drawable.search_24px),
            contentDescription = "Search",
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .clickable { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapSearch)) },
        )
        Icon(
            painter = painterResource(Res.drawable.gepetto_menu),
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .padding(4.dp)
                .clickable { onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapMenu)) },
            contentDescription = "Menu"
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    Common.testInit()
    MakerDataProvider.init()

    GcTheme {
        Surface {
            CollectionTitle("title") { }
        }
    }
}
