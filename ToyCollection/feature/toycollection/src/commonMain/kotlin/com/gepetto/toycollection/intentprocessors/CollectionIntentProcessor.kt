package com.gepetto.toycollection.intentprocessors

import androidx.lifecycle.viewModelScope
import com.gepetto.toycollection.Database
import com.gepetto.toycollection.dataproviders.defaultProcessorCollectionList
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.ToyCounts
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.ToyCollection
import com.gepetto.toycollection.utils.ImageCache
import club.gepetto.circum.CircumEffect
import club.gepetto.circum.CircumIntentProcessor
import club.gepetto.utils.ioCoroutine
import com.gepetto.common.TOYS_QUERY_STRING
import com.gepetto.common.Common
import com.gepetto.toycollection.dataproviders.updateLoadedStatus
import kotlinx.coroutines.CoroutineScope
import club.gepetto.utils.ioDispatcher
import club.gepetto.composeutils.gcCurrentTimeMillis
import kotlinx.coroutines.launch
import club.gepetto.GcLog

class CollectionIntentProcessor(private val dbInjected: Database) : CircumIntentProcessor<CollectionState, CollectionIntent, CircumEffect>() {
    private lateinit var toyCounts: ToyCounts
    private var title = "title"
    private var aboutString = ""
    private var aboutTitle = ""
    private var timeStamp = 0L
    private var currentCollectionData : CollectionData? = null
    private var collectionTitle = ""
    private var currentToyCollection = ToyCollection("", "", "")

    fun getCurrentCollection() = currentToyCollection
    fun getCurrentCollectionData() = currentCollectionData

    override fun onIntentCommand(intent: CollectionIntent, state: CollectionState?) {
        when (intent) {
            is CollectionIntent.LoadCollection -> {
                val loaded = defaultProcessorCollectionList.findCollection(intent.toyCollection.title)!!.loaded
                if (loaded) {
                    getCollection(intent.toyCollection, forceCache = true)
                }
                else {
                    setState { CollectionState.Loading(intent.toyCollection) }
                    getCollection(intent.toyCollection)
                }
            }

            is CollectionIntent.GoHome -> sendEffect { GoToHomeEffect }

            is CollectionIntent.Tapped -> onActionClicked(intent, state)

            is CollectionIntent.ToyEdited -> {
                intent.maker.replace(intent.toy, currentCollectionData)
            }
            is CollectionIntent.ToyAdded -> {
                intent.maker.add(intent.toy, currentCollectionData)
            }
        }
    }

    private fun onActionClicked(intent: CollectionIntent.Tapped, state: CollectionState?) {
        when (intent.tapAction) {
            is CollectionTapAction.TapBack -> {
                if (state is CollectionState.Loaded)
                    sendEffect { GoBackEffect }
                else
                    popState()
            }

            is CollectionTapAction.TapSearch -> sendEffect { GoToSearchEffect }

            is CollectionTapAction.TapMaker -> sendEffect(GoToMakerEffect(intent.tapAction.maker))

            is CollectionTapAction.TapRefresh -> {
                setState { CollectionState.Loading(currentToyCollection) }
                getCollection(currentToyCollection, useCache = false)
            }

            is CollectionTapAction.TapClearCache -> {
                CoroutineScope(ioDispatcher).launch {
                    GcLog.d("Pictures of $title being deleted...")
                    Common.clearGcImageCache()
                    currentCollectionData!!.makers.forEach { it.clearCache() }
                    GcLog.d("Cache cleared")
                }
            }

            else -> {
                GcLog.e("tapAction = ${intent.tapAction}")
                TODO()
            }
        }
    }

    private fun getCollection(toyCollection: ToyCollection, useCache: Boolean = true, forceCache: Boolean = false) {
        viewModelScope.launch(ioDispatcher) {
            currentToyCollection = toyCollection
            var fetchedFromCahe = false

            if (useCache) {
                val collection = reloadCollection()
                if (collection != null) {
                    currentCollectionData = collection
                    buildHeadings(collection)

                    dbInjected.updateCollectionList(collection, toyCollection.typeQuery)

                    setState {
                        CollectionState.Loaded(
                            toyCollection = currentToyCollection,
                            collection = collection,
                            about = aboutTitle,
                            aboutString = aboutString,
                            title = title,
                            fromCache = true,
                        )
                    }
                    fetchedFromCahe = true
                } else {
                    setState { CollectionState.Loading(currentToyCollection) }
                }
            }

            if (!forceCache)
                getCollectionFromNetwork(
                    typeQuery = toyCollection.typeQuery,
                    onError = { if (!fetchedFromCahe) setState { CollectionState.Error } },
                    onSuccess = {
                        currentCollectionData = it
                        val collection = it

                        updateLoadedStatus(collection.collectionTitle, true)

                        setState {
                            CollectionState.Loaded(
                                toyCollection = toyCollection,
                                collection = collection,
                                about = aboutTitle,
                                aboutString = aboutString,
                                title = title,
                                fromCache = false,
                            )
                        }

                        if (Common.caching)
                            ioCoroutine { ImageCache.checkImagesInCache(currentCollectionData) }
                    }
                )
        }
    }

    private fun getCollectionFromNetwork(
        typeQuery: String = TOYS_QUERY_STRING,
        onError: (Int) -> Unit,
        onSuccess: (CollectionData) -> Unit,
    ) {
        dbInjected.getCollection(
            saveDb = true,
            typeQuery = typeQuery,
            onError = onError,
            onSuccess = {
                GcLog.d("downloaded from network")
                buildHeadings(it)
                onSuccess(it)
            }
        )
    }

    private fun reloadCollection() : CollectionData? {
        val tempDb = CollectionData.restore(typeQuery = currentToyCollection.typeQuery)
        if (tempDb != null)
            buildHeadings(tempDb)
        return tempDb
    }

    private fun buildHeadings(dbUsed: CollectionData) {
        GcLog.d( "buildHeadings called with a collection - prefix = '${dbUsed.prefix}'")
        currentCollectionData = dbUsed
        collectionTitle = currentToyCollection.title
        currentCollectionData!!.collectionTitle = collectionTitle

        aboutTitle = "About ${currentToyCollection.title}"
        aboutString = "${collectionTitle}\n\nServer updated on ${currentCollectionData!!.date}\nServer build ${currentCollectionData!!.buildNumber}"
        title = "${collectionTitle} - ${currentCollectionData!!.totalToys} models"

        val makers = currentCollectionData!!.makers
        var totMakers = 0
        for (maker in makers) { if (maker.hasToys) totMakers++ }

        title = title + ", from ${totMakers} manufacturers, on ${currentCollectionData!!.date}."
        title = collectionTitle
        toyCounts = ToyCounts.countToys(makers)
        aboutString = aboutString + toyCounts.toString()

        timeStamp = gcCurrentTimeMillis()
        GcLog.d("buildHeadings timestamp = $timeStamp, title=$title")
    }

    fun getCollectionDataForMaker(maker: Maker): CollectionData? {
        return dbInjected.getCollectionDataForMaker(maker)
    }

    fun getCollectionDataForToy(toy: Toy): CollectionData? {
        return dbInjected.getCollectionDataForToy(toy)
    }

    fun getActiveCollectionData(): CollectionData? {
        return dbInjected.getActiveCollectionData()
    }
}

sealed interface CollectionState {
    data object Initial : CollectionState
    data class Loading(val toyCollection: ToyCollection, val timeStamp: Long = gcCurrentTimeMillis()) : CollectionState
    data object Error : CollectionState

    data class Loaded(
        val toyCollection: ToyCollection,
        val collection: CollectionData,
        val about: String,
        val aboutString: String,
        val title: String,
        val timeStamp: Long = gcCurrentTimeMillis(),
        val fromCache: Boolean,
    ) : CollectionState
}

sealed interface CollectionIntent {
    data class Tapped(val tapAction: CollectionTapAction, ): CollectionIntent
    data class ToyEdited(val toy: Toy, val maker: Maker) : CollectionIntent
    data class ToyAdded(val toy: Toy, val maker: Maker) : CollectionIntent
    data class LoadCollection(val toyCollection: ToyCollection) : CollectionIntent
    data object GoHome : CollectionIntent
}

sealed interface CollectionTapAction {
    data class TapMaker(val maker: Maker): CollectionTapAction
    data object TapBack: CollectionTapAction
    data object TapRefresh: CollectionTapAction
    data object TapClearCache: CollectionTapAction
    data object TapSearch: CollectionTapAction
    data object TapMenu: CollectionTapAction
    data object TapAbout: CollectionTapAction
}
