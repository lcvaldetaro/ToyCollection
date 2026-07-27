package com.gepetto.toycollection.ui.collection.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import club.gepetto.composeutils.scaffold.GcDrawerItem
import com.gepetto.common.Common
import com.gepetto.toycollection.intentprocessors.CollectionTapAction
import com.gepetto.toycollection.intentprocessors.CollectionIntent
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import org.jetbrains.compose.resources.DrawableResource

fun getResourceFromFilename(filename: String): DrawableResource {
    val clean = filename.substringBeforeLast(".")
    return when (clean) {
        "slotcaricon" -> Res.drawable.gepetto_slotcaricon
        "train" -> Res.drawable.gepetto_train
        "staticmodel" -> Res.drawable.gepetto_staticicon
        "plastickits" -> Res.drawable.gepetto_plastickits
        "others" -> Res.drawable.gepetto_icon
        "gepetto" -> Res.drawable.gepetto
        "invertedgepetto" -> Res.drawable.gepetto_darkicon
        else -> Res.drawable.gepetto_icon
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDrawer(
    modifier: Modifier = Modifier,
    aboutTitle: String = "",
    collectionImage: String = "",
    onIntentCommand: (CollectionIntent) -> Unit,
) {
    val iconResource = getResourceFromFilename(collectionImage)
    val title = aboutTitle.substring(6)

    Column (modifier = modifier.systemBarsPadding()) {
        GcDrawerItem(
            text = aboutTitle,
            contentDescription = aboutTitle,
            iconResourceRes = iconResource
        ) {
            onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapAbout))
        }
        GcDrawerItem(
            text = "Refresh ${title}",
            contentDescription = "Refresh ${title}",
            iconResourceRes = Res.drawable.gepetto_refresh,
        ) {
            onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapRefresh))
        }
        GcDrawerItem (
            iconResourceRes = Res.drawable.gepetto_delete,
            text = "Clear $title cache"
        ) {
            onIntentCommand(CollectionIntent.Tapped(CollectionTapAction.TapClearCache))
        }
    }
}
