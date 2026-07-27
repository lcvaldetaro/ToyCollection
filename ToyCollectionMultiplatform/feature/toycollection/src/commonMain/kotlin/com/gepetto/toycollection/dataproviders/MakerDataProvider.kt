package com.gepetto.toycollection.dataproviders

import com.gepetto.toycollection.models.Maker
import com.gepetto.toycollection.models.CollectionData

data class MakerProvider(val maker: Maker)

class MakerDataProvider {

    companion object {
        val monogram = Maker(
            name = "Monogram",
            comments = "",
            bitmaps = "",
            bitmapsTimeStamp = "",
            country = "United States of America",
            toysList = ToyDataProvider.monogramToys,
        )

        val cox = Maker(
            name = "Cox",
            comments = "",
            bitmaps = "",
            bitmapsTimeStamp = "",
            country = "United States of America",
            toysList = ToyDataProvider.coxToys,
        )

        val estrela = Maker(
            name = "Estrela",
            comments = "",
            bitmaps = "",
            bitmapsTimeStamp = "",
            country = "Brazil",
            toysList = ToyDataProvider.estrelaToys,
        )

        val collectionDataDb = CollectionData(
            date  = "November, 6, 1958",
            buildNumber = "1000",
            makers = listOf(cox, monogram, estrela)
        )

        fun init () {
            cox.normalizeData()
            for (toy in cox.toysList) toy.normalizeData()
            cox.normalizeData()

            monogram.normalizeData()
            for (toy in monogram.toysList) toy.normalizeData()
            monogram.normalizeData()

            estrela.normalizeData()
            for (toy in estrela.toysList) toy.normalizeData()
            estrela.normalizeData()
        }
    }
}