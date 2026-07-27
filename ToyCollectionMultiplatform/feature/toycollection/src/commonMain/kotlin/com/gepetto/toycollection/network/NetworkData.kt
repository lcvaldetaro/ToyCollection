package com.gepetto.toycollection.network

import com.gepetto.toycollection.models.CollectionData

data object NetworkData {
    private var collectionSaved: CollectionData? = null

    fun getCollection (
        typeQuery: String,
        saveDb: Boolean = false,
        onError: (Int) -> Unit = {},
        onSuccess: (CollectionData) -> Unit = {},
    ): CollectionData?  {
        println("NetworkData: getCollection Called")

        Network.getToys(
            typeQuery = typeQuery,
            onError = onError
        ) { toysResponse ->
            if (saveDb)
                toysResponse.save(typeQuery)

            Network.getMakers(onError = onError) { collectionData ->
                if (saveDb)
                    collectionData.save(typeQuery)

                collectionData.mergeToysResultIntoMakersResult(toysResponse = toysResponse)
                collectionSaved = collectionData
                onSuccess(collectionData)
            }
        }

        return collectionSaved
    }
}