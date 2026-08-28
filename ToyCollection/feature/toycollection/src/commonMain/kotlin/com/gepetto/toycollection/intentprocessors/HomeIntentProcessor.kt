package com.gepetto.toycollection.intentprocessors

import club.gepetto.circum.CircumEffect
import club.gepetto.circum.CircumIntentProcessor
import com.gepetto.toycollection.Database
import com.gepetto.toycollection.dataproviders.defaultProcessorCollectionList
import com.gepetto.toycollection.models.CollectionList
import com.gepetto.toycollection.models.ToyCollection
import club.gepetto.GcLog
import com.gepetto.common.platformExitApp

class HomeIntentProcessor(private val dbInjected: Database) : CircumIntentProcessor<HomeState, HomeIntent, CircumEffect>() {

    init {
        getCollectionList()
    }

    override fun onIntentCommand(intent: HomeIntent, state: HomeState?) {
        when (intent) {
            is HomeIntent.Tapped -> onActionClicked(intent, state)
            is HomeIntent.Home -> setState { HomeState.Loaded(dbInjected.getListOfCollections(), fromCache = false) }
        }
    }

    private fun onActionClicked(intent: HomeIntent.Tapped, state: HomeState?) {
        when (intent.tapAction) {
            is HomeTapAction.TapAbout -> {
                when (state) {
                    is HomeState.Loaded -> { setState { HomeState.Loaded(dbInjected.getListOfCollections(), fromCache = false) } }
                    else -> TODO()
                }
            }
            is HomeTapAction.TapBack -> {
                when (state) {
                    is HomeState.Loaded -> platformExitApp()
                    else -> popState()
                }
            }
            is HomeTapAction.TapCollection -> Unit // sendEffect { GoToCollectionEffect(intent.tapAction.collection) }

            is HomeTapAction.TapPrivacyPolicy -> sendEffect(GoToPrivacyPolicyEffect)

            else -> {
                GcLog.e("CollectionChoiceTapAction = ${intent.tapAction}")
                TODO()
            }
        }
    }

    private fun getCollectionList() {
        // TODO - should come from server, then save it. 4 states: loading, cached, loaded, error
        val colList = CollectionList.restore()
        if (colList != null)
            dbInjected.initializeCollectionList(colList)
        else
            dbInjected.initializeCollectionList(defaultProcessorCollectionList)

        setState { HomeState.Loaded(dbInjected.getListOfCollections(), fromCache = false) }
    }
}

sealed interface HomeState {
    data object Initial : HomeState
    data class Loaded(val collectionList: CollectionList, val timeStamp: Long = 0L, val fromCache: Boolean) : HomeState
}

sealed interface HomeIntent {
    data object Home: HomeIntent
    data class Tapped(val tapAction: HomeTapAction): HomeIntent
}

sealed interface HomeTapAction {
    data class TapCollection(val collection: ToyCollection): HomeTapAction
    data object TapBack: HomeTapAction
    data object TapAbout: HomeTapAction
    data object TapPrivacyPolicy: HomeTapAction
    data object TapMenu: HomeTapAction
    data object TapTitle: HomeTapAction
}
