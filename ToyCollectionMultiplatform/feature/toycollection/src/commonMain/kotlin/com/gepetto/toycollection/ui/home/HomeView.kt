package com.gepetto.toycollection.ui.home

import club.gepetto.composeutils.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.circumIntentProcessor
import com.gepetto.toycollection.intentprocessors.HomeIntent
import com.gepetto.toycollection.intentprocessors.HomeIntentProcessor
import com.gepetto.toycollection.intentprocessors.HomeState
import com.gepetto.toycollection.intentprocessors.HomeTapAction

@Composable
fun HomeView(
    modifier: Modifier = Modifier,
    homeIp: HomeIntentProcessor = circumIntentProcessor<HomeIntentProcessor>(),
    onEffect: (CircumEffect) -> Unit
) {
    val state by homeIp.collectState(initialState = HomeState.Initial)
    homeIp.onEffectIssued { effect -> onEffect(effect) }

    BackHandler(true) { homeIp.onIntentCommand(HomeIntent.Tapped(HomeTapAction.TapBack), state) }

    when (state) {
        is HomeState.Loaded -> {
            HomeContent(
                modifier = modifier,
                timeStamp = (state as HomeState.Loaded).timeStamp,
                collectionList = (state as HomeState.Loaded).collectionList,
                onIntentCommand = { homeIp.sendIntentCommand(it) }
            )
        }

        else -> Unit
    }
}

