package com.gepetto.toycollection.models

import com.gepetto.common.Common
import com.gepetto.common.SAVED_FILENAME
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import club.gepetto.GcLog
import com.gepetto.common.GcFile

@Serializable
data class CollectionData (
    var date: String,
    var buildNumber: String,
    var makers: List<Maker>,
    var prefix: String = "car",
    var merged: Boolean = false,
)  {
    val size: Int
        get() = makers.size

    operator fun get(position: Int): Maker = makers[position]

    @Transient var totalToys        = 0
    @Transient var totalFactoryToys = 0
    @Transient var valueCollection  = 0.0F
    @Transient var costCollection   = 0.0F
    @Transient var collectionTitle = ""

    fun isTheSame (dbIn: CollectionData) : Boolean {
        var result = false

        if (
            (this.date == dbIn.date) &&
            (this.makers.size == dbIn.makers.size) &&
            (this.buildNumber == dbIn.buildNumber) &&
            (this.prefix == dbIn.prefix) &&
            (this.totalToys == dbIn.totalToys) &&
            (this.valueCollection == dbIn.valueCollection) &&
            (this.costCollection == dbIn.costCollection) &&
            (this.totalFactoryToys == dbIn.totalFactoryToys)
        ) {
            var i = 0
            while (i < this.makers.size) {
                if (this.makers[i] != dbIn.makers[i])
                    break
                i++
            }
            result = i == this.makers.size - 1
        }

        return result
    }

    fun findToy (refNum: String) : Toy? {
        for (maker in this.makers)
            for (toy in maker.toysList)
                if (refNum == toy.refNum)
                    return toy
        return null
    }

    fun deleteMaker (maker: Maker) : Boolean {
        val toysResponse = toToysResponse()
        val makersResponse = toMakersResponse()
        var i = 0

        for (m in makersResponse.makers) {
            if (m.name == maker.name) {
                val newMakersList = makersResponse.makers.toMutableList()
                newMakersList.removeAt(i)
                makersResponse.makers = newMakersList.toImmutableList()

                makersResponse.mergeToysResultIntoMakersResult(toysResponse)
                this.makeEqual(makersResponse)
                return true
            }
            i++
        }
        return false
    }

    fun deleteToy (toy: Toy) : Boolean {
        val maker = toy.getMaker(this)
        if (maker != null)
            return maker.delete(toy, this)
        return false
    }

    fun makeEqual(makersResponse: CollectionData) {
        this.makers = makersResponse.makers
        this.date = makersResponse.date
        this.buildNumber = makersResponse.buildNumber
        this.costCollection = makersResponse.costCollection
        this.totalToys = makersResponse.totalToys
        this.totalFactoryToys = makersResponse.totalFactoryToys
        this.valueCollection = makersResponse.valueCollection
        this.merged = makersResponse.merged
        this.prefix = makersResponse.prefix
    }

    fun contains (searchString: String) : List<Toy> {
        val returnList = ArrayList<Toy>(0)

        if (searchString.isNotEmpty()) {
            for (maker in makers) {
                val toyList = maker.containsToys(searchString)

                for (toy in toyList)
                    returnList.add(toy)
            }
        }

        return returnList
    }

    fun findMaker(makerName : String) : Maker? {
        var resultMaker : Maker? = null

        for (maker in this.makers) {
            if (maker.name == makerName) {
                resultMaker = maker
                break
            }
        }

        return resultMaker
    }

    fun findMakerIndex(makerName : String) : Int {
        var result = 0

        for (maker in this.makers) {
            if (maker.name == makerName) {
                return result
            }
            else
                result++
        }

        return 0
    }

    fun add(maker: Maker) {
        val newMakers = makers.toMutableList()

        val counts = ToyCounts.countSingleMakerToys(maker)
        maker.totalFactoryToys = counts.totalFactory

        newMakers.add(maker)
        makers = newMakers.toImmutableList()

        normalizeData()
        adjustTotals()
    }

    fun getNextRefnum() : String {
        var highestRefNum = 0

        for (maker in makers) {
            for (toy in maker.toysList) {
               if (toy.refNum.toInt() > highestRefNum)
                   highestRefNum = toy.refNum.toInt() + 1
            }
        }

        return "$highestRefNum"
    }

    fun normalizeData() : CollectionData {
        this.makers = this.makers.sortedBy { it.name.lowercase() }

        for (maker in this.makers) {
            maker.splitBitmaps()
            maker.cleanHtmlFromComments()
        }
        return this
    }

    fun adjustTotals() {
        val counts = ToyCounts.countToys(makers)
        totalToys = counts.total
        totalFactoryToys = counts.totalFactory
        valueCollection = counts.value
        costCollection = counts.cost
    }

    fun mergeToysResultIntoMakersResult(toysResponse: ToysResponse) {
        val makersList = ArrayList<Maker>()
        var m = 0
        var previousMaker = ""
        var makersToysList = ArrayList<Toy>()
        var totalToys = 0
        var totalFactoryToys = 0
        var value = 0.0F
        var cost = 0.0F

        GcLog.d("Merging toys response into makers response")

        for (maker in this.makers) { maker.toysList = emptyList() }

        for (toy in toysResponse.toys) {
            if (toy.traded.isEmpty()) {
                try { value += toy.value.toFloat() } catch (_: Exception) { }
                try { cost += toy.amountPaid.toFloat() } catch (_: Exception) { }
                totalToys++
            }

            if (toy.hasPicture.isYes()) {
                this.prefix = toy.picture.substring(0, 3)
            }

            if (toy.bodyMaker != previousMaker) {
                if (m > 0) {
                    val newMaker = Maker(name = previousMaker, toysList = makersToysList)
                    newMaker.totalFactoryToys = totalFactoryToys
                    makersList.add(newMaker)
                    newMaker.setHasToys()
                }

                previousMaker = toy.bodyMaker
                makersToysList = ArrayList()
                m++
                totalFactoryToys = 0
            }
            makersToysList.add(toy)

            if (toy.traded.isEmpty() && !toy.makerCombo.contains("/"))
                totalFactoryToys++
        }

        toysResponse.valueCollection = value
        toysResponse.costCollection = cost

        val newMaker = Maker(name = previousMaker, toysList =  makersToysList)
        newMaker.totalFactoryToys = totalFactoryToys
        newMaker.setHasToys()
        newMaker.initPicture()

        makersList.add(newMaker)

        totalFactoryToys = 0

        for (maker in makersList) {
            var i = 0
            var match = false
            while (i < this.makers.size) {
                if (maker.name == this.makers[i].name) {
                    maker.bitmaps = this.makers[i].bitmaps
                    maker.bitmapsTimeStamp = this.makers[i].bitmapsTimeStamp
                    maker.bitmapsSize = this.makers[i].bitmapsSize
                    maker.splitBitmaps()
                    maker.comments = this.makers[i].comments
                    maker.cleanHtmlFromComments()
                    maker.country = this.makers[i].country
                    maker.bitmapFiles = this.makers[i].bitmapFiles
                    maker.initPicture()
                    match = true
                }
                i++
            }
            if (!match) {
                maker.bitmapFiles = null
            }
            totalFactoryToys += maker.totalFactoryToys
        }

        this.makers = makersList
        this.totalToys = totalToys
        this.totalFactoryToys = totalFactoryToys
        this.valueCollection = toysResponse.valueCollection
        this.costCollection = toysResponse.costCollection
        this.merged = true
    }

    fun toMakersResponse() : CollectionData {
        val makersResponse = this.copy()
        for (maker in makersResponse.makers) {
            maker.toysList = emptyList()
        }
        return makersResponse
    }

    fun toToysResponse() : ToysResponse {
        val toyssList = emptyList<Toy>().toMutableList()

        for (maker in makers) {
            for (toy in maker.toysList) {
                toyssList.add(toy)
            }
        }

        val toysResponse = ToysResponse(
            date = this.date,
            buildNumber = this.buildNumber,
            toys = toyssList.toImmutableList()
        )

        return toysResponse
    }

    fun countAllToys () {
        val toyCounts = ToyCounts.countToys(this.makers)
        this.valueCollection = toyCounts.value
        this.costCollection  = toyCounts.cost
    }

    fun toToysResponseJson() : String {
        val toysResponse = this.toToysResponse()
        return Json.encodeToString(toysResponse)
    }

    fun toMakersResponseJson () : String {
        return Json.encodeToString(toMakersResponse())
    }

    fun toJson() : String {
        return Json.encodeToString(this)
    }

    fun save(
        typeQuery: String,
        filename: String = SAVED_FILENAME,
    ) {
        try {
            val filenameComplete = "${filename}-${typeQuery}"
            val savedDataFile = GcFile(Common.packageFolder, filenameComplete)
            val jsonStr = this.toJson()
            savedDataFile.writeText(jsonStr)
            GcLog.d("data saved on ${Common.packageFolder}/${filenameComplete}")
        }
        catch (e: Exception) {
            GcLog.e("save Exception ${e}")
        }
    }

    companion object {
        fun fromJson (jsonStr: String) : CollectionData? {
            var result : CollectionData? = null
            try {
                val newCollectionSaved = Json.decodeFromString<CollectionData>(jsonStr)
                val toyCounts = ToyCounts.countToys(newCollectionSaved.makers)

                newCollectionSaved.valueCollection = toyCounts.value
                newCollectionSaved.costCollection  = toyCounts.cost

                for (maker in newCollectionSaved.makers) {
                    maker.initPicture()
                }

                result = newCollectionSaved
            }
            catch (e: Exception) {
                GcLog.e("Exception ${e}")
            }
            return result
        }
        fun restore(
            typeQuery: String,
            filename: String = SAVED_FILENAME,
        ) : CollectionData? {
            val filenameComplete = "${filename}-${typeQuery}"
            var resultCollection: CollectionData? = null
            val savedCollectionFile = GcFile( Common.packageFolder, filenameComplete)
            val toysResponse = ToysResponse.restore(typeQuery)

            if (savedCollectionFile.exists() && toysResponse != null) {
                GcLog.d("Saved collection data exists on '${Common.packageFolder}${filenameComplete}'")
                try {
                    resultCollection = fromJson(savedCollectionFile.readText())
                    resultCollection!!.mergeToysResultIntoMakersResult(toysResponse)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    resultCollection = null
                }
            }

            if (resultCollection == null)
                GcLog.d("Saved collection data do not exist on '${Common.packageFolder}${filenameComplete}'. returning $resultCollection")
            return resultCollection
        }
    }
}