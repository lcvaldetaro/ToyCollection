package com.gepetto.toycollection.ui.common.images

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import club.gepetto.composeutils.navbar.GcNavButton
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*

class NavigationButtons(
    val currentChoice: String = "",
    val onClick: (GcNavButton) -> Unit
) {
    var choice = currentChoice
    val buttonList = listOf(
        GcNavButton(imageVector = Icons.Default.Info, label = "Info", navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(imageVector = Icons.Default.Settings, label = "Settings", navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
       // GcNavButton(resourceResIcon = Res.drawable.home, label = "Home", outline = true, navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(resourceResIcon = Res.drawable.slotcaricon, vector = false, label = "Slot Cars", outline = true, navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(resourceResIcon = Res.drawable.train, vector = false, label = "Trains", outline = true, navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(resourceResIcon = Res.drawable.staticmodel, vector = false, label = "Static Models", outline = true, navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(resourceResIcon = Res.drawable.plastickits, vector = false, label = "Model Kits", outline = true, navChoice = choice, onClick = { choice = it.label.orEmpty(); onClick(it) }),
        GcNavButton(resourceResIcon = Res.drawable.others, vector = false, label = "Others", navChoice = choice, outline = true, onClick = { choice = it.label.orEmpty(); onClick(it) }),
    )
}