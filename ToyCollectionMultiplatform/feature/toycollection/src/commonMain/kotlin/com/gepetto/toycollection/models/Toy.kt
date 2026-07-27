package com.gepetto.toycollection.models

import com.gepetto.common.Common
import com.gepetto.common.WEBSITE_BASE_URL
import com.gepetto.toycollection.utils.stripExcessSpaces
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import club.gepetto.GcLog
import com.gepetto.common.GcFile

@Serializable
data class Toy (
    var refNum: String = "",
    var makerCombo: String = "",
    var condition: String = "",
    var scale: String = "",
    var description: String = "",
    var comments: String = "",
    var bodyMaker: String = "",
    var chassisMaker: String = "",
    var chassisType: String = "",
    var motorMaker: String = "",
    var motorDetails: String = "",
    var repro: String = "",
    var boxed: String = "",
    var color: String = "",
    var catalogNumber: String = "",
    var majorWork: String = "",
    var minorWork: String = "",
    @SerialName("maintenance") var soundFile: String = "",
    var detail: String = "",
    var toMake: String = "",
    var buy: String = "",
    var traded: String = "",
    var value: String = "",
    var amountPaid: String = "",
    var amountSold: String = "",
    var bitmaps: String = "",
    var bitmapsTimeStamp: String = "",
    var bitmapsSize: String = "",
    var picture: String = "",
    var pictureTimeStamp: String = "",
    var pictureSize: String = "",
    var hasPicture: String = "",
    @SerialName("factoryCar") var factoryToy: String = "",
    var acquired: String = "",
) {
    @Transient var bitmapFiles          : List<String>? = null
    @Transient var bitmapFilesTimeStamp : List<String>? = null
    @Transient var bitmapFilesSize : List<String>? = null
    @Transient var normalizedScale = ""

    fun getMaker(makersDb: CollectionData) : Maker? {
        var result : Maker? = null

        for (maker in makersDb.makers) {
            if (this.bodyMaker.equals(maker.name)) {
                result = maker
                break
            }
        }

        return result
    }

    fun normalizeData () {
        bitmapFiles = bitmaps.stripExcessSpaces().split(" ")
        bitmapFilesTimeStamp = bitmapsTimeStamp.stripExcessSpaces().split(" ")
        bitmapFilesSize = bitmapsSize.stripExcessSpaces().split(" ")

        normalizedScale = normalizeScale(scale)
        initFields()
    }

    fun initFields () {
        if (makerCombo.contains("/")) {
            factoryToy = "n"
            val word = makerCombo.split("/")
            chassisMaker = word[0]
            bodyMaker = word[1]
        }
        else {
            chassisMaker = makerCombo
            bodyMaker = makerCombo
        }
    }

    fun clearCache (directory: GcFile? = Common.directoryFile) {
        Common.clearGcImageCache(picture)
        GcLog.d("deleted ${picture}")
        bitmapFiles?.forEach { pic ->
            GcLog.d("deleted ${pic}")
            Common.clearGcImageCache(pic)
        }
        /*bitmapFiles?.let {
            for (pic in it) {
                Timber.i("deleted ${pic}")
                Common.clearCache(pic)
            }
        }*/
    }

    fun isSimilarPicture () : Boolean {
        if (this.hasPicture.isNo() && this.picture.isNotEmpty())
            return true
        return false
    }

    fun contains (searchString: String) : Boolean {
        var result = true
        val words: List<String> = searchString.split(" ")

        if (words.size > 1) {
            if (words[0].lowercase() == "refnum" && words[1] == refNum)
                return result
        }
        else
        if (words.size == 1) {
            if (words[0].lowercase() == "repro" && repro.isYes())
                return result

            if (words[0].lowercase() == "boxed" && boxed.isYes())
                return result

            if (words[0].lowercase() == "traded" && traded.isNotEmpty())
                return result

            if (words[0].lowercase() == "gone" && traded.isNotEmpty())
                return result

            if (words[0] == refNum)
                return result

            val nScale = normalizeScale(words[0])
            if (nScale.isNotEmpty() && nScale == normalizedScale)
                return result
        }

        for (word in words) {
            result = containsWord(word)
            if (!result)
                break
        }

        return result
    }

    private fun containsWord (word: String) : Boolean {
        if (word.isNotEmpty()) {
            if (description.contains(word, true)) return true
            if (chassisMaker.contains(word, true)) return true
            if (chassisType.contains(word, true)) return true
            if (motorMaker.contains(word, true)) return true
            if (motorDetails.contains(word, true)) return true
            if (soundFile.contains(word, true)) return true
            if (detail.contains(word, true)) return true
            if (color.contains(word, true)) return true
            if (condition.contains(word, true)) return true
            if (catalogNumber.contains(word, true)) return true
            if (bodyMaker.contains(word, true)) return true
            if (comments.contains(word, true)) return true
            if (acquired.contains(word, true)) return true
            if (buy.contains(word, true)) return true
            if (bitmaps.contains(word, true)) return true
            if (majorWork.contains(word, true)) return true
            if (minorWork.contains(word, true)) return true
            if (traded.contains(word, true)) return true
            if (scale.contains(word, true)) return true
            if (soundFile.contains(word, true)) return true
            if (refNum.contains(word, true)) return true
        }
        return false
    }

    fun isFactoryToy() : Boolean {
        return !makerCombo.contains("/")
    }

    fun isTraded() : Boolean {
        return traded.isNotEmpty()
    }

    fun normalizeScale (scale: String) : String {
        var result = ""

        if (scale == "1/27" || scale == "1/28" || scale == "1/30" || scale == "1/32" || scale == "1/34" || scale == "1/36" )
           result = "1/32"

        if (scale == "1/24" || scale == "1/25")
           result = "1/24"

        if (scale == "1/12" || scale == "1/18" || scale == "1/22")
           result = "1/18"

        if (scale == "1/38" || scale == "1/43" || scale == "1/44" || scale == "1/45" || scale == "1/48" || scale == "1/52")
           result = "O"

        if (scale == "1/64" || scale == "1/65" || scale == "1/88")
           result = "HO"

        if (scale == "1/99" || scale == "1/144" || scale == "1/160")
            result = "N"

        return result
    }

    fun makeEqual(toy: Toy) {
        this.refNum = toy.refNum
        this.makerCombo = toy.makerCombo
        this.condition = toy.condition
        this.scale = toy.scale
        this.description = toy.description
        this.comments = toy.comments
        this.bodyMaker = toy.bodyMaker
        this.chassisMaker = toy.chassisMaker
        this.chassisType = toy.chassisType
        this.motorMaker = toy.motorMaker
        this.motorDetails = toy.motorDetails
        this.repro = toy.repro
        this.boxed = toy.boxed
        this.color = toy.color
        this.catalogNumber = toy.catalogNumber
        this.majorWork = toy.majorWork
        this.minorWork = toy.minorWork
        this.soundFile = toy.soundFile
        this.detail = toy.detail
        this.toMake = toy.toMake
        this.buy = toy.buy
        this.traded = toy.traded
        this.value = toy.value
        this.amountPaid = toy.amountPaid
        this.amountSold = toy.amountPaid
        this.bitmaps = toy.bitmaps
        this.bitmapsTimeStamp = toy.bitmapsTimeStamp
        this.bitmapsSize = toy.bitmapsSize
        this.picture = toy.picture
        this.pictureTimeStamp = toy.pictureTimeStamp
        this.pictureSize = toy.pictureSize
        this.hasPicture = toy.hasPicture
        this.factoryToy = toy.factoryToy
        this.acquired = toy.acquired
        this.bitmapFiles = toy.bitmapFiles
        this.bitmapFilesTimeStamp = toy.bitmapFilesTimeStamp
        this.bitmapFilesSize = toy.bitmapFilesSize
        this.normalizedScale = toy.normalizedScale

    }

    fun getUrl(collection: CollectionData) : String {
        val fileName = collection.prefix + "${refNum.toInt()}" + ".htm"
        return "${Common.getActiveBaseUrl()}$fileName"
    }

    fun getSearchUrl() : String {
        return "https://www.google.com/search?q=" + description.replace(" ", "+") + "&safe=off&source=lnms&tbm=isch&sa=X"
    }
}

fun String.isYes () : Boolean {
    if (this == "y" || this == "Y") return true
    return false
}

fun String.isNo () : Boolean {
    return !this.isYes()
}