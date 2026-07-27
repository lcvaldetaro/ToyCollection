package com.gepetto.toycollection

import com.gepetto.toycollection.intentprocessors.*
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.models.CollectionList
import com.gepetto.toycollection.models.ToyCounts
import com.gepetto.toycollection.network.NetworkData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import club.gepetto.utils.ioDispatcher
import org.koin.dsl.module

val toyCollectionModule = module {
    single { Database() }
    factory { HomeIntentProcessor(get()) }
    factory { CollectionIntentProcessor(get()) }
    factory { MakerIntentProcessor() }
    factory { SearchIntentProcessor() }
    factory { ToyIntentProcessor() }
    factory { club.gepetto.composeutils.navigation3.SceneStrategyIntentProcessor() }
}

class Database {
    private lateinit var collectionList: CollectionList

    fun initializeCollectionList (collectionList: CollectionList) { this.collectionList = collectionList}

    fun getListOfCollections () : CollectionList { return collectionList }

    fun updateCollectionList(collection: CollectionData, typeQuery: String) {
        for (c in collectionList.toyCollections) {
            if (c.typeQuery == typeQuery) {
                c.collectionData = collection
                break
            }
        }
        val totalToyCounts = ToyCounts()
        for (col in collectionList.toyCollections) {
            if (col.collectionData != null) {
                totalToyCounts.add(ToyCounts.countToys(col.collectionData!!.makers))
            }
        }

        collectionList.totals = totalToyCounts.toString()
        collectionList.save()
    }

    fun getCollection (
        saveDb: Boolean = false,
        typeQuery: String,
        onError: (Int) -> Unit = {},
        onSuccess: (CollectionData) -> Unit = {},
    ) {
        println("getCollection() was called. typeQuery = '${typeQuery}'")

        CoroutineScope(ioDispatcher).launch {
            NetworkData.getCollection(
                saveDb = saveDb,
                typeQuery = typeQuery,
                onSuccess = { onSuccess(it) },
                onError = { onError(it) }
            )
        }
    }
}
