package com.gepetto.toycollection.intentprocessors

import club.gepetto.circum.CircumEffect
import club.gepetto.composeutils.navigation3.GcAdaptiveInfo
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.Toy

data object GoToHomeEffect : CircumEffect()

data class GoToMakerEffect(val maker: Maker) : CircumEffect()

data class GoToViewToyEffect(val toy: Toy) : CircumEffect()

data class GoToEditToyEffect(val toy: Toy) : CircumEffect()

data class GoToAddToyEffect(val maker: Maker) : CircumEffect()

data class GoToWebPageEffect(val toy: Toy) : CircumEffect()

data class GoToWebSearchEffect(val toy: Toy) : CircumEffect()

data object GoToPrivacyPolicyEffect : CircumEffect()

data object GoToSearchEffect : CircumEffect()

data object GoBackEffect : CircumEffect()

data class SystemBackEffect(val adaptiveInfo: GcAdaptiveInfo) : CircumEffect()

data object CloseCurrentExtraPaneEffect : CircumEffect()

data object CloseCurrentDetailPaneEffect : CircumEffect()
