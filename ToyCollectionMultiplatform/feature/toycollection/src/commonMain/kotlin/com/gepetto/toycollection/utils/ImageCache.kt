package com.gepetto.toycollection.utils

import com.gepetto.common.Common
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.network.Network
import kotlinx.coroutines.CoroutineScope
import club.gepetto.utils.ioDispatcher
import kotlinx.coroutines.launch
import club.gepetto.GcLog
import com.gepetto.common.GcFile

object ImageCache {
    private val cacheDirectory = Common.directoryFile
    private val cacheMap: MutableMap<String, ImageDetails> = mutableMapOf()

    fun checkImagesInCache (collection: CollectionData?) {
        val collectionUsed = collection
        if (collectionUsed != null) {
            val newCollection = collectionUsed
            var timeStamp: Long
            var size: Long
            var timeStampOnCollection: Long
            var sizeOnCollection: Long
            var inCache = 0
            var notInCache = 0

            newCollection.makers.forEach { maker ->
                // Check maker image files
                GcLog.i("Processing maker ${maker}")
                maker.bitmapFiles?.forEachIndexed { index, image ->
                    timeStamp = fileTimeStamp(image)
                    size = fileSize(image)
                    timeStampOnCollection = maker.bitmapFilesTimeStamp!![index].toTimeStamp()
                    sizeOnCollection = maker.bitmapFilesSize!![index].toSize()

                    if (image.hasData()) {
                        if (checkCache(imageFile = image, timeStampOnCache = timeStamp, timeStampOnDb = timeStampOnCollection, sizeOnCache = size, sizeOnDb = sizeOnCollection,)) {
                            inCache++
                            cacheMap[image] = ImageDetails(image, timeStamp, timeStampOnCollection, size, sizeOnCollection)
                        } else {
                            notInCache++
                            downloadFile(image)
                        }
                    }
                }

                // Check first toy picture
                timeStamp = fileTimeStamp(maker.toysList[0].picture)
                size = fileSize(maker.toysList[0].picture)
                timeStampOnCollection = maker.toysList[0].pictureTimeStamp.toTimeStamp()
                sizeOnCollection = maker.toysList[0].pictureSize.toTimeStamp()

                if (maker.toysList[0].picture.hasData()) {
                    if (checkCache(imageFile = maker.toysList[0].picture, timeStampOnCache = timeStamp, timeStampOnDb = timeStampOnCollection, sizeOnCache = size, sizeOnDb = sizeOnCollection)) {
                        inCache++
                        cacheMap[maker.toysList[0].picture] = ImageDetails(maker.toysList[0].picture, timeStamp, timeStampOnCollection, size, sizeOnCollection)
                    } else {
                        notInCache++
                        downloadFile(maker.toysList[0].picture)
                    }
                }
            }

            // Check all toy pictures
            newCollection.makers.forEach { maker ->
                maker.toysList.forEach { toy ->
                    // Download picture if needed
                    timeStamp = fileTimeStamp(toy.picture)
                    size = fileSize(toy.picture)
                    timeStampOnCollection = toy.pictureTimeStamp.toTimeStamp()
                    sizeOnCollection = toy.pictureSize.toSize()

                    if (toy.picture != "" && toy.picture != " ") {
                        if (checkCache(imageFile = toy.picture, timeStampOnCache = timeStamp, timeStampOnDb = timeStampOnCollection, sizeOnCache = size, sizeOnDb = sizeOnCollection,)) {
                            inCache++
                            cacheMap[toy.picture] = ImageDetails(toy.picture, timeStamp, timeStampOnCollection, size, sizeOnCollection)
                        } else {
                            notInCache++
                            downloadFile(toy.picture)
                        }
                    }

                    // Check all other bitmaps for this toy
                    toy.bitmapFiles?.forEachIndexed { index, image ->
                        timeStamp = fileTimeStamp(image)
                        size = fileSize(image)
                        timeStampOnCollection = toy.bitmapFilesTimeStamp!![index].toTimeStamp()
                        sizeOnCollection = toy.bitmapFilesSize!![index].toSize()

                        if (image.hasData()) {
                            if (checkCache(imageFile = image, timeStampOnCache = timeStamp, timeStampOnDb = timeStampOnCollection, sizeOnCache = size, sizeOnDb = sizeOnCollection)) {
                                inCache++
                                cacheMap[image] = ImageDetails(image, timeStamp, timeStampOnCollection, size, sizeOnCollection)
                            } else {
                                notInCache++
                                downloadFile(image)
                            }
                        }

                    }
                }
            }

            GcLog.i("${notInCache} images not in cache, ${inCache} in cache")
        }
    }

    fun isImageOnCacheClean(fileName: String?) : Boolean {
        if (fileName == null) return false
        val image = fileName.lowercase()
        val imageDetails = cacheMap[image]

        return imageDetails?.isIdentical(image) ?: false
    }

    fun downloadFile(fileName: String?, onSuccess: () -> Unit = {} ) {
        if (!Common.caching || fileName == null) return
        CoroutineScope(ioDispatcher).launch {
            Network.getFile(
                filename = fileName,
                onError = { GcLog.e("failed to download ${fileName}") },
                onSuccess = { rc, bytArray ->
                    saveDownloadedFile(fileName, bytArray)
                    onSuccess()
                }
            )
        }
    }

    fun isImageOnCache (cacheImageIn: String) : Boolean {
        val cacheImage = cacheImageIn.lowercase()
        val imageFile = GcFile(cacheDirectory, cacheImage)

        return imageFile.exists()
    }

    private fun removeImageFromCache (cacheImageIn: String)  {
        val cacheImage = cacheImageIn.lowercase()
        val imageFile = GcFile(cacheDirectory, cacheImage)

        imageFile.delete()
    }

    private fun fileTimeStamp (cacheImageIn: String) : Long {
        val cacheImage = cacheImageIn.lowercase()
        val imageFile = GcFile(cacheDirectory, cacheImage)

        return imageFile.lastModified()
    }

    private fun fileSize (cacheImageIn: String) : Long {
        val cacheImage = cacheImageIn.lowercase()
        val imageFile = GcFile(cacheDirectory, cacheImage)
        return imageFile.length()
    }

    private fun saveDownloadedFile(fileName: String, byteArray: ByteArray, directory: GcFile? = Common.directoryFile) {
        val file = GcFile(directory, fileName)
        file.writeBytes(byteArray)
        val timeStamp = fileTimeStamp(fileName)
        val fileSize = fileSize(fileName)
        val imageDetails = ImageDetails(
                fileName = fileName,
                timeStampOnCache = timeStamp,
                timeStampOnDb = timeStamp,
                sizeOnCache = fileSize,
                sizeOnDb = fileSize
        )
        cacheMap[fileName] = imageDetails
    }

    private fun checkCache (
        imageFile: String,
        timeStampOnCache : Long,
        timeStampOnDb : Long,
        sizeOnCache: Long,
        sizeOnDb: Long,
    ) : Boolean {
        if (sizeOnDb != sizeOnCache) {
            GcLog.d("image ${imageFile} is different (cache/db) ${sizeOnCache}/${sizeOnDb} and need to be re-downloaded")
            removeImageFromCache(imageFile)
            return false
        }
        else
        if (timeStampOnDb > 0 && timeStampOnCache > 0 && timeStampOnDb > timeStampOnCache ) {
            GcLog.d("image ${imageFile} is old (cache/db) ${timeStampOnCache}/${timeStampOnDb} and need to be re-downloaded")
            removeImageFromCache(imageFile)
            return false
        }

        return isImageOnCache(imageFile)
    }

    data class ImageDetails(
        val fileName: String,
        val timeStampOnCache : Long,
        val timeStampOnDb : Long,
        val sizeOnCache: Long,
        val sizeOnDb: Long,
    ) {
        fun isIdentical(image: String) : Boolean {
            val size = fileSize(image)
            val timeStamp = fileTimeStamp(image)
            return size == sizeOnDb && size == sizeOnCache && timeStamp == timeStampOnDb && timeStamp == timeStampOnCache && image == fileName
        }
    }
}

