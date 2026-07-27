package com.gepetto.toycollection.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
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
import club.gepetto.composeutils.sysForegroundColor
import com.gepetto.common.Common
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import com.gepetto.toycollection.dataproviders.MakerDataProvider
import com.gepetto.toycollection.dataproviders.defaultProcessorCollectionList
import com.gepetto.toycollection.intentprocessors.HomeIntent
import com.gepetto.toycollection.intentprocessors.HomeTapAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTitle(
    modifier: Modifier = Modifier,
    title: String = "",
    onIntentCommand: (HomeIntent) -> Unit,
) {
    val tint = sysForegroundColor()
    val iconSize = 36.dp
    val startPadding = if (isSystemInLandscape()) 24.dp else 30.dp

    Row (modifier = modifier
        .height(56.dp)
        .background(sysBackgroundColor())
        .padding(start = startPadding, end = 16.dp)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isAndroidPlatform) {
            GcImage(
                imageResourceRes = Res.drawable.poweroff,
                contentDescription = "Exit",
                size = iconSize,
                onClick = { onIntentCommand(HomeIntent.Tapped(HomeTapAction.TapBack))  },
            )
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.size(iconSize))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = tint,
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .clickable { onIntentCommand(HomeIntent.Tapped(HomeTapAction.TapTitle)) }
                .padding(end = 8.dp)
        )

        Icon(
            painter = painterResource(Res.drawable.gepetto_menu),
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .padding(4.dp)
                .clickable { onIntentCommand(HomeIntent.Tapped(HomeTapAction.TapMenu)) },
            contentDescription = ""
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
            HomeTitle(
                title = defaultProcessorCollectionList.title,
            ) {}
        }
    }
}


