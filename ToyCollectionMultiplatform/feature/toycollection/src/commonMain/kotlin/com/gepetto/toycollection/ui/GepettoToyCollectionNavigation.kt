package com.gepetto.toycollection.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.savedstate.serialization.SavedStateConfiguration
import club.gepetto.circum.circumIntentProcessor
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.isLandscape
import club.gepetto.composeutils.navigation3.GcNavDisplay
import club.gepetto.composeutils.navigation3.GcSceneStrategy
import club.gepetto.composeutils.navigation3.removeUpToExclusiveAndAdd
import club.gepetto.composeutils.navigation3.rememberGcSceneStrategy
import club.gepetto.composeutils.navigation3.removeUpToInclusive
import club.gepetto.composeutils.navigation3.removeUpToInclusivePanes
import com.gepetto.common.platformExitApp
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.defaultProcessorCollectionList
import com.gepetto.toycollection.intentprocessors.CloseCurrentDetailPaneEffect
import com.gepetto.toycollection.intentprocessors.CloseCurrentExtraPaneEffect
import com.gepetto.toycollection.intentprocessors.CollectionIntentProcessor
import com.gepetto.toycollection.intentprocessors.GoBackEffect
import com.gepetto.toycollection.intentprocessors.GoToAddToyEffect
import com.gepetto.toycollection.intentprocessors.GoToEditToyEffect
import com.gepetto.toycollection.intentprocessors.GoToMakerEffect
import com.gepetto.toycollection.intentprocessors.GoToPrivacyPolicyEffect
import com.gepetto.toycollection.intentprocessors.GoToSearchEffect
import com.gepetto.toycollection.intentprocessors.GoToViewToyEffect
import com.gepetto.toycollection.intentprocessors.GoToWebSearchEffect
import com.gepetto.toycollection.intentprocessors.SystemBackEffect
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.ToyCollection
import kotlinx.serialization.Serializable
import com.gepetto.toycollection.ui.Destination.Home
import com.gepetto.toycollection.ui.Destination.CollectionScreen
import com.gepetto.toycollection.ui.Destination.MakerScreen
import com.gepetto.toycollection.ui.Destination.PrivacyPolicy
import com.gepetto.toycollection.ui.Destination.Search
import com.gepetto.toycollection.ui.addtoy.AddToyView
import com.gepetto.toycollection.ui.collection.main.CollectionView
import com.gepetto.toycollection.ui.common.AboutSheet
import com.gepetto.toycollection.ui.common.PlaceHolderView
import com.gepetto.toycollection.ui.web.WebPageView
import com.gepetto.toycollection.ui.edittoy.EditToyView
import com.gepetto.toycollection.ui.toy.ToyView
import com.gepetto.toycollection.ui.privacypolicy.PrivacyPolicyView
import com.gepetto.toycollection.ui.maker.MakerView
import com.gepetto.toycollection.ui.search.SearchView
import com.gepetto.toycollection.ui.settings.SettingsView
import com.gepetto.toycollection.ui.home.HomeView

sealed interface Destination : NavKey {
    @Serializable data object Home : Destination
    @Serializable data class CollectionScreen(val collectionChoice: ToyCollection) : Destination
    @Serializable data class MakerScreen(val maker: Maker) : Destination
    @Serializable data object Search : Destination
    @Serializable data object Info : Destination
    @Serializable data object Settings : Destination
    @Serializable data object PrivacyPolicy : Destination
    @Serializable data class ViewToy(val toy: Toy) : Destination
    @Serializable data class ViewToyFromSearch(val toy: Toy) : Destination
    @Serializable data class EditToyLandscape(val toy: Toy) : Destination
    @Serializable data class EditToyPortrait(val toy: Toy) : Destination
    @Serializable data class AddToy(val maker: Maker) : Destination
    @Serializable data class WebSearchLandscape(val toy: Toy) : Destination
    @Serializable data class WebSearchPortrait(val toy: Toy) : Destination
}

val collection : CollectionData? = null

@Composable
fun ToyCollectionNavigation(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<Destination>(Home) }
    val gcSceneStrategy = rememberGcSceneStrategy<NavKey>()
    val collectionIp: CollectionIntentProcessor = circumIntentProcessor<CollectionIntentProcessor>()

    var forceDarkMode by remember { mutableStateOf(Common.forceDarkMode) }
    var forceLightMode by remember { mutableStateOf(Common.forceLightMode) }
    val darkTheme = when {
        forceDarkMode -> true
        forceLightMode -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        Common.initializePrivacyPolicy()
    }

    fun cleanBackStack() { backStack.clear() }
    fun safePop() {
        backStack.removeLastOrNull()
        if (backStack.isEmpty()) {
            backStack.add(Home)
        }
    }

    TopView(
        modifier = modifier,
        darkTheme = darkTheme,
        onIconCliked = {
            cleanBackStack()
            when (it) {
                "Home" -> backStack.add(Home)
                "Info" -> backStack.add(Destination.Info)
                "Settings" -> backStack.add(Destination.Settings)
                else -> {
                    val collection = defaultProcessorCollectionList.findCollection(it)!!
                    backStack.add(CollectionScreen(collection))
                }
            }
        }
    ) {
        GcNavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(gcSceneStrategy),
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            onBack = { safePop() },
            entryProvider = entryProvider {
                entry<Home> {
                    GcTheme(darkTheme = darkTheme) {
                        HomeView { effect ->
                            when (effect) {
                                is GoToPrivacyPolicyEffect -> backStack.add((PrivacyPolicy))
                            }
                        }
                    }
                }
                entry<Destination.Info> {
                    GcTheme(darkTheme = darkTheme) {
                        Box(Modifier.fillMaxSize()) {
                            AboutSheet(
                                modifier = Modifier.align(Alignment.Center),
                                showVersion = true,
                                showPpLink = true,
                                collectionList = defaultProcessorCollectionList,
                                onPpClick = { backStack.add(PrivacyPolicy) },
                                onCloseClick = { safePop() }
                            )
                        }
                    }
                }
                entry<Destination.Settings> {
                    GcTheme(darkTheme = darkTheme) {
                        SettingsView(
                            onThemeChanged = { forceLight, forceDark ->
                                forceLightMode = forceLight
                                forceDarkMode = forceDark
                            },
                            onExit = {
                                safePop()
                            }
                        )
                    }
                }
                entry<PrivacyPolicy> {
                    GcTheme(darkTheme = darkTheme) {
                        PrivacyPolicyView { effect ->
                            when (effect) {
                                is GoBackEffect -> safePop()
                            }
                        }
                    }
                }
                entry<CollectionScreen> { key ->
                    GcTheme(darkTheme = darkTheme) {
                        CollectionView(key.collectionChoice, collectionIp) { effect ->
                            when (effect) {
                                is GoBackEffect -> platformExitApp()
                                is GoToMakerEffect -> backStack.add(MakerScreen(effect.maker))
                                is GoToSearchEffect -> backStack.add(Search)
                            }
                        }
                    }
                }
                entry<MakerScreen>(
                    metadata = GcSceneStrategy.listPane(
                        resizeable = true,
                        useThreePanes = true,
                        placeholder = { PlaceHolderView() })
                ) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        MakerView(key.maker, collectionIp) { effect ->
                            when (effect) {
                                is GoBackEffect -> backStack.removeUpToInclusive(key)
                                is GoToViewToyEffect -> backStack.removeUpToExclusiveAndAdd(
                                    key,
                                    Destination.ViewToy(effect.toy)
                                )

                                is GoToAddToyEffect -> backStack.add(Destination.AddToy(effect.maker))
                            }
                        }
                    }
                }
                entry<Search>(
                    metadata = GcSceneStrategy.listPane(
                        resizeable = true,
                        useThreePanes = false,
                        placeholder = { PlaceHolderView() })
                ) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        SearchView(collectionIp) { effect ->
                            when (effect) {
                                is GoBackEffect -> backStack.removeUpToInclusive(key)
                                is GoToViewToyEffect -> backStack.removeUpToExclusiveAndAdd(
                                    key,
                                    Destination.ViewToyFromSearch(effect.toy)
                                )
                            }
                        }
                    }
                }
                entry<Destination.ViewToy>(metadata = GcSceneStrategy.detailPane(resizeable = true)) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        BoxWithConstraints {
                            val landscape = this.isLandscape()
                            ToyView(key.toy, collectionIp) { effect ->
                                when (effect) {
                                    is SystemBackEffect -> {
                                        backStack.removeUpToInclusive(key)
                                        if (backStack.last() is MakerScreen)
                                            safePop()
                                    }

                                    is GoToEditToyEffect -> {
                                        if (landscape)
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.EditToyLandscape(effect.toy))
                                        else
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.EditToyPortrait(effect.toy))
                                    }

                                    is GoToWebSearchEffect -> {
                                        if (landscape)
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.WebSearchLandscape(effect.toy))
                                        else
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.WebSearchPortrait(effect.toy))
                                    }
                                }
                            }
                        }
                    }
                }
                entry<Destination.ViewToyFromSearch>(metadata = GcSceneStrategy.detailPane(resizeable = true, percent = .3f)) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        BoxWithConstraints {
                            val landscape = this.isLandscape()
                            ToyView(key.toy, collectionIp) { effect ->
                                when (effect) {
                                    is SystemBackEffect -> {
                                        backStack.removeUpToInclusive(key)
                                        if (backStack.last() is MakerScreen)
                                            safePop()
                                    }

                                    is GoToEditToyEffect -> {
                                        if (landscape)
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.EditToyLandscape(effect.toy))
                                        else
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.EditToyPortrait(effect.toy))
                                    }

                                    is GoToWebSearchEffect -> {
                                        if (landscape)
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.WebSearchLandscape(effect.toy))
                                        else
                                            backStack.removeUpToExclusiveAndAdd(key, Destination.WebSearchPortrait(effect.toy))
                                    }
                                }
                            }
                        }
                    }
                }
                entry<Destination.EditToyPortrait>(metadata = GcSceneStrategy.bottomSheetPane()) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        EditToyView(key.toy, collectionIp) { effect ->
                            when (effect) {
                                is CloseCurrentExtraPaneEffect -> backStack.removeUpToInclusive(key)
                                is SystemBackEffect -> backStack.removeUpToInclusive(key) //, effect.adaptiveInfo)
                            }
                        }
                    }
                }
                entry<Destination.EditToyLandscape>(metadata = GcSceneStrategy.extraPane(resizeable = true)) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        EditToyView(key.toy, collectionIp) { effect ->
                            when (effect) {
                                is CloseCurrentExtraPaneEffect -> safePop()
                                is SystemBackEffect -> backStack.removeUpToInclusivePanes(
                                    key,
                                    effect.adaptiveInfo
                                )
                            }
                        }
                    }
                }
                entry<Destination.AddToy>(metadata = GcSceneStrategy.detailPane(resizeable = true)) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        AddToyView(key.maker, collectionIp) { effect ->
                            when (effect) {
                                is CloseCurrentDetailPaneEffect -> safePop()
                                is SystemBackEffect -> backStack.removeUpToInclusivePanes(
                                    key,
                                    effect.adaptiveInfo
                                )
                            }
                        }
                    }
                }
                entry<Destination.WebSearchLandscape>(metadata = GcSceneStrategy.extraPane(resizeable = true)) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        WebPageView(
                            url = key.toy.getSearchUrl(),
                            title = "Search for ${key.toy.description}"
                        ) { effect ->
                            when (effect) {
                                is CloseCurrentExtraPaneEffect -> safePop()
                                is SystemBackEffect -> backStack.removeUpToInclusivePanes(
                                    key,
                                    effect.adaptiveInfo
                                )
                            }
                        }
                    }
                }
                entry<Destination.WebSearchPortrait>(metadata = GcSceneStrategy.bottomSheetPane()) { key ->
                    GcTheme(darkTheme = darkTheme) {
                        WebPageView(
                            url = key.toy.getSearchUrl(),
                            title = "Search for ${key.toy.description}"
                        ) { effect ->
                            when (effect) {
                                is CloseCurrentExtraPaneEffect -> safePop()
                                is SystemBackEffect -> backStack.removeUpToInclusivePanes(
                                    key,
                                    effect.adaptiveInfo
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
