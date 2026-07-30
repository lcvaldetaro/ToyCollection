package com.gepetto.toydb.service

import club.gepetto.GcLog
import com.gepetto.toydb.database.Maker
import com.gepetto.toydb.database.Toy
import com.gepetto.toydb.database.ToyDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.gepetto.toydb.CommonConfig
import okio.FileSystem
import okio.Path.Companion.toPath

private const val TAG = "ImportExportService"

expect fun getCurrentDateString(): String

@Serializable
data class JsonMaker(
    val name: String,
    val country: String = "",
    val bitmaps: String = "",
    val bitmapsSize: String = "",
    val bitmapsTimeStamp: String = "",
    val comments: String = ""
)

@Serializable
data class JsonToy(
    val refNum: String,
    val makerCombo: String = "",
    val condition: String = "",
    val scale: String = "",
    val description: String,
    val comments: String = "",
    val bodyMaker: String = "",
    val chassisMaker: String = "",
    val chassisType: String = "",
    val motorMaker: String = "",
    val motorDetails: String = "",
    val repro: String = "",
    val boxed: String = "",
    val color: String = "",
    val catalogNumber: String = "",
    val majorWork: String = "",
    val minorWork: String = "",
    val maintenance: String = "",
    val detail: String = "",
    val toMake: String = "",
    val buy: String = "",
    val traded: String = "",
    val value: String = "",
    val acquired: String = "",
    val amountPaid: String = "",
    val amountSold: String = "",
    val bitmaps: String = "",
    val bitmapsTimeStamp: String = "",
    val bitmapsSize: String = "",
    val picture: String = "",
    val pictureTimeStamp: String = "",
    val pictureSize: String = "",
    val hasPicture: String = "",
    val factoryCar: String = "",
    val yearMade: String = "",
    val number: String = "",
    val myComments: String = ""
)

@Serializable
data class JsonMakersFile(
    val date: String = getCurrentDateString(),
    val buildNumber: String = CommonConfig.versionCodeString,
    val makers: List<JsonMaker>
)

@Serializable
data class JsonToysFile(
    val date: String = getCurrentDateString(),
    val buildNumber: String = CommonConfig.versionCodeString,
    val cars: List<JsonToy>
)

@Serializable
data class JsonCategorySetting(
    val category: String,
    val imagePrefix: String,
    val label: String
)

@Serializable
data class JsonCategorySettingsFile(
    val date: String = getCurrentDateString(),
    val buildNumber: String = CommonConfig.versionCodeString,
    val settings: List<JsonCategorySetting>
)

@Serializable
data class JsonAppSetting(
    val key: String,
    val value: String
)

@Serializable
data class JsonAppSettingsFile(
    val date: String = getCurrentDateString(),
    val buildNumber: String = CommonConfig.versionCodeString,
    val settings: List<JsonAppSetting>
)

object ImportExportService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun getImagesPath(db: ToyDatabase): String? {
        var path: String? = null
        try {
            val cursor = db.query("SELECT value FROM app_settings WHERE key = 'images_path'")
            if (cursor.next()) {
                path = cursor.getString("value")
            }
            cursor.close()
        } catch (e: Exception) {
            GcLog.e(TAG, "Error reading images_path: ${e.message}", e)
        }
        return path
    }

    private fun processBitmaps(
        db: ToyDatabase,
        incomingBitmaps: String,
        incomingSizes: String,
        incomingTimestamps: String
    ): Triple<String, String, String> {
        val customPath = getImagesPath(db)
        val targetDir = if (!customPath.isNullOrEmpty()) {
            customPath.toPath()
        } else {
            val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
            possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
        }

        val filenames = incomingBitmaps.split(" ").filter { it.trim().isNotEmpty() }
        val sizes = incomingSizes.split(" ").filter { it.trim().isNotEmpty() }
        val timestamps = incomingTimestamps.split(" ").filter { it.trim().isNotEmpty() }

        val finalBitmaps = mutableListOf<String>()
        val finalSizes = mutableListOf<String>()
        val finalTimestamps = mutableListOf<String>()

        filenames.forEachIndexed { index, filename ->
            val trimmedFilename = filename.trim()
            val file = targetDir.div(trimmedFilename)
            val metadata = FileSystem.SYSTEM.metadataOrNull(file)
            if (metadata != null && metadata.isRegularFile) {
                val actualSize = metadata.size ?: 0L
                val actualTimestamp = metadata.lastModifiedAtMillis ?: 0L
                finalBitmaps.add(trimmedFilename)
                finalSizes.add(actualSize.toString())
                finalTimestamps.add(actualTimestamp.toString())
            } else {
                val sizeVal = sizes.getOrNull(index)?.trim() ?: ""
                val timeVal = timestamps.getOrNull(index)?.trim() ?: ""
                if (sizeVal.isNotEmpty() && timeVal.isNotEmpty()) {
                    finalBitmaps.add(trimmedFilename)
                    finalSizes.add(sizeVal)
                    finalTimestamps.add(timeVal)
                }
            }
        }

        return Triple(
            finalBitmaps.joinToString(" "),
            finalSizes.joinToString(" "),
            finalTimestamps.joinToString(" ")
        )
    }

    /**
     * Imports makers from JSON string content into the database.
     */
    fun importMakers(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing makers...")
        val parsed = json.decodeFromString<JsonMakersFile>(jsonContent)
        var count = 0
        parsed.makers.forEach { m ->
            val (bitmaps, bitmapsSize, bitmapsTimeStamp) = processBitmaps(
                db,
                m.bitmaps,
                m.bitmapsSize,
                m.bitmapsTimeStamp
            )
            db.execute(
                """
                INSERT OR REPLACE INTO makers 
                (name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments) 
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(m.name, m.country, bitmaps, bitmapsSize, bitmapsTimeStamp, m.comments)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count makers.")
        return count
    }

    /**
     * Imports toys of a specific category from JSON string content into the database.
     */
    fun importToys(db: ToyDatabase, toyType: String, jsonContent: String): Int {
        GcLog.d(TAG, "Importing toys for type '$toyType'...")
        val parsed = json.decodeFromString<JsonToysFile>(jsonContent)
        var count = 0
        parsed.cars.forEach { c ->
            val ref = c.refNum.toIntOrNull() ?: 0
            val valDouble = c.value.trim().toDoubleOrNull() ?: 0.0
            val paidDouble = c.amountPaid.trim().toDoubleOrNull() ?: 0.0

            val customPath = getImagesPath(db)
            val targetDir = if (!customPath.isNullOrEmpty()) {
                customPath.toPath()
            } else {
                val possibleDirs = listOf("images", "../images", "ToyDb/images", "../ToyDb/images")
                possibleDirs.map { it.toPath() }.find { FileSystem.SYSTEM.exists(it) } ?: "images".toPath()
            }

            var finalPicture = ""
            var finalPicSize = 0
            var finalPicTime = 0L
            var finalHasPicture = "n"

            val incomingPicture = c.picture.trim()
            if (incomingPicture.isNotEmpty()) {
                val file = targetDir.div(incomingPicture)
                val metadata = FileSystem.SYSTEM.metadataOrNull(file)
                if (metadata != null && metadata.isRegularFile) {
                    finalPicture = incomingPicture
                    finalPicSize = (metadata.size ?: 0L).toInt()
                    finalPicTime = metadata.lastModifiedAtMillis ?: 0L
                    finalHasPicture = "y"
                } else {
                    val incomingSize = c.pictureSize.trim().toIntOrNull() ?: 0
                    val incomingTime = c.pictureTimeStamp.trim().toLongOrNull() ?: 0L
                    if (incomingSize > 0 && incomingTime > 0L) {
                        finalPicture = incomingPicture
                        finalPicSize = incomingSize
                        finalPicTime = incomingTime
                        finalHasPicture = if (c.hasPicture.trim().isNotEmpty()) c.hasPicture.trim() else "y"
                    }
                }
            }
            
            val (bitmaps, bitmapsSize, bitmapsTimeStamp) = processBitmaps(
                db,
                c.bitmaps,
                c.bitmapsSize,
                c.bitmapsTimeStamp
            )

            val body = c.bodyMaker.trim()
            val chassis = c.chassisMaker.trim()
            val calculatedMakerCombo = if (body == chassis) {
                body
            } else if (body.isEmpty()) {
                chassis
            } else if (chassis.isEmpty()) {
                body
            } else {
                "$chassis/$body"
            }

            db.execute(
                """
                INSERT OR REPLACE INTO toys (
                    ref_num, toy_type, description, maker_combo, scale, factory_car, 
                    body_maker, acquired, chassis_type, chassis_maker, condition, color, 
                    motor_maker, motor_details, catalog_number, comments, major_work, 
                    minor_work, repro, value, amount_paid, amount_sold, traded, buy, 
                    maintenance, to_make, detail, boxed, picture, picture_size, 
                    picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp,
                    year_made, number, my_comments
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(
                    ref, toyType, c.description, calculatedMakerCombo, c.scale, c.factoryCar,
                    c.bodyMaker, c.acquired, c.chassisType, c.chassisMaker, c.condition, c.color,
                    c.motorMaker, c.motorDetails, c.catalogNumber, c.comments, c.majorWork,
                    c.minorWork, c.repro, valDouble, paidDouble, c.amountSold, c.traded, c.buy,
                    c.maintenance, c.toMake, c.detail, c.boxed, finalPicture, finalPicSize,
                    finalPicTime, finalHasPicture, bitmaps, bitmapsSize, bitmapsTimeStamp,
                    c.yearMade, c.number, c.myComments
                )
            )
            count++
        }
        GcLog.d(TAG, "Imported $count toys for type '$toyType'.")
        return count
    }

    /**
     * Exports all makers from the database into JSON format.
     */
    fun exportMakers(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting makers...")
        val cursor = db.query("SELECT * FROM makers ORDER BY name COLLATE NOCASE")
        val list = mutableListOf<JsonMaker>()
        while (cursor.next()) {
            list.add(
                JsonMaker(
                    name = cursor.getString("name") ?: "",
                    country = cursor.getString("country") ?: "",
                    bitmaps = cursor.getString("bitmaps") ?: "",
                    bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                    bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                    comments = cursor.getString("comments") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonMakersFile(makers = list)
        return json.encodeToString(JsonMakersFile.serializer(), fileObj)
    }

    /**
     * Exports toys of a specific category from the database into JSON format.
     */
    fun exportToys(db: ToyDatabase, toyType: String): String {
        GcLog.d(TAG, "Exporting toys for type '$toyType'...")
        val cursor = db.query("SELECT * FROM toys WHERE toy_type = ? ORDER BY body_maker COLLATE NOCASE, description COLLATE NOCASE", listOf(toyType))
        val list = mutableListOf<JsonToy>()
        while (cursor.next()) {
            val ref = cursor.getInt("ref_num") ?: 0
            val valDouble = cursor.getDouble("value") ?: 0.0
            val paidDouble = cursor.getDouble("amount_paid") ?: 0.0
            val picSize = cursor.getInt("picture_size") ?: 0
            val picTime = cursor.getDouble("picture_timestamp")?.toLong() ?: 0L // JDBC double to long fallback helper

            list.add(
                JsonToy(
                    refNum = ref.toString(),
                    makerCombo = cursor.getString("maker_combo") ?: "",
                    condition = cursor.getString("condition") ?: "",
                    scale = cursor.getString("scale") ?: "",
                    description = cursor.getString("description") ?: "",
                    comments = cursor.getString("comments") ?: "",
                    bodyMaker = cursor.getString("body_maker") ?: "",
                    chassisMaker = cursor.getString("chassis_maker") ?: "",
                    chassisType = cursor.getString("chassis_type") ?: "",
                    motorMaker = cursor.getString("motor_maker") ?: "",
                    motorDetails = cursor.getString("motor_details") ?: "",
                    repro = cursor.getString("repro") ?: "",
                    boxed = cursor.getString("boxed") ?: "",
                    color = cursor.getString("color") ?: "",
                    catalogNumber = cursor.getString("catalog_number") ?: "",
                    majorWork = cursor.getString("major_work") ?: "",
                    minorWork = cursor.getString("minor_work") ?: "",
                    maintenance = cursor.getString("maintenance") ?: "",
                    detail = cursor.getString("detail") ?: "",
                    toMake = cursor.getString("to_make") ?: "",
                    buy = cursor.getString("buy") ?: "",
                    traded = cursor.getString("traded") ?: "",
                    value = formatDouble(valDouble),
                    acquired = cursor.getString("acquired") ?: "",
                    amountPaid = formatDouble(paidDouble),
                    amountSold = cursor.getString("amount_sold") ?: "",
                    bitmaps = cursor.getString("bitmaps") ?: "",
                    bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                    bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                    picture = cursor.getString("picture") ?: "",
                    pictureTimeStamp = if (picTime > 0) picTime.toString() else "",
                    pictureSize = if (picSize > 0) picSize.toString() else "",
                    hasPicture = cursor.getString("has_picture") ?: "",
                    factoryCar = cursor.getString("factory_car") ?: "",
                    yearMade = cursor.getString("year_made") ?: "",
                    number = cursor.getString("number") ?: "",
                    myComments = cursor.getString("my_comments") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonToysFile(cars = list)
        return json.encodeToString(JsonToysFile.serializer(), fileObj)
    }

    fun importCategorySettings(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing category settings...")
        val parsed = json.decodeFromString<JsonCategorySettingsFile>(jsonContent)
        var count = 0
        parsed.settings.forEach { s ->
            db.execute(
                """
                INSERT OR REPLACE INTO category_settings (category, image_prefix, label)
                VALUES (?, ?, ?)
                """.trimIndent(),
                listOf(s.category, s.imagePrefix, s.label)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count category settings.")
        return count
    }

    fun exportCategorySettings(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting category settings...")
        val cursor = db.query("SELECT * FROM category_settings ORDER BY category ASC")
        val list = mutableListOf<JsonCategorySetting>()
        while (cursor.next()) {
            list.add(
                JsonCategorySetting(
                    category = cursor.getString("category") ?: "",
                    imagePrefix = cursor.getString("image_prefix") ?: "",
                    label = cursor.getString("label") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonCategorySettingsFile(settings = list)
        return json.encodeToString(JsonCategorySettingsFile.serializer(), fileObj)
    }

    fun importAppSettings(db: ToyDatabase, jsonContent: String): Int {
        GcLog.d(TAG, "Importing app settings...")
        val parsed = json.decodeFromString<JsonAppSettingsFile>(jsonContent)
        var count = 0
        parsed.settings.forEach { s ->
            db.execute(
                """
                INSERT OR REPLACE INTO app_settings (key, value)
                VALUES (?, ?)
                """.trimIndent(),
                listOf(s.key, s.value)
            )
            count++
        }
        GcLog.d(TAG, "Imported $count app settings.")
        return count
    }

    fun exportAppSettings(db: ToyDatabase): String {
        GcLog.d(TAG, "Exporting app settings...")
        val cursor = db.query("SELECT * FROM app_settings ORDER BY key ASC")
        val list = mutableListOf<JsonAppSetting>()
        while (cursor.next()) {
            list.add(
                JsonAppSetting(
                    key = cursor.getString("key") ?: "",
                    value = cursor.getString("value") ?: ""
                )
            )
        }
        cursor.close()
        val fileObj = JsonAppSettingsFile(settings = list)
        return json.encodeToString(JsonAppSettingsFile.serializer(), fileObj)
    }

    private fun formatDouble(value: Double): String {
        if (value <= 0.0) return ""
        val rounded = (value * 100 + 0.5).toLong() / 100.0
        val parts = rounded.toString().split(".")
        val integerPart = parts[0]
        val decimalPart = parts.getOrNull(1) ?: "00"
        val paddedDecimal = decimalPart.padEnd(2, '0').take(2)
        return "$integerPart.$paddedDecimal"
    }

    fun String.toIso8859_1Bytes(): ByteArray {
        val bytes = ByteArray(this.length)
        for (i in 0 until this.length) {
            val code = this[i].code
            bytes[i] = if (code < 256) code.toByte() else '?'.code.toByte()
        }
        return bytes
    }

    data class CategoryHtmlConfig(
        val toyType: String,
        val imagePrefix: String,
        val mainTitle: String
    )
    
    val categoryConfigs = listOf(
        CategoryHtmlConfig("slot", "car", "Luiz Claudio Valdetaro's Slot car collection"),
        CategoryHtmlConfig("train", "tra", "Luiz Claudio Valdetaro's Train collection"),
        CategoryHtmlConfig("static", "sta", "Luiz Claudio Valdetaro's Static Models collection"),
        CategoryHtmlConfig("kit", "pla", "Luiz Claudio Valdetaro's Models Kit collection"),
        CategoryHtmlConfig("misc", "mis", "Luiz Claudio Valdetaro's Miscelaneous toys")
    )

    fun backfillFromLstFiles(db: ToyDatabase, exportDirectory: String): Int {
        val categoryFiles = mapOf(
            "slot" to "CARINDEX.LST",
            "train" to "TRAINDEX.LST",
            "static" to "STAINDEX.LST",
            "kit" to "PLAINDEX.LST",
            "misc" to "MISINDEX.LST"
        )
        
        var totalUpdated = 0
        val fs = FileSystem.SYSTEM
        
        for ((category, filename) in categoryFiles) {
            val path = exportDirectory.toPath().div(filename)
            if (!fs.exists(path)) continue
            
            try {
                val content = fs.read(path) { readUtf8() }
                val lines = content.split(Regex("\\r?\\n"))
                val dashLineIndex = lines.indexOfFirst { it.startsWith("----") }
                if (dashLineIndex < 1) continue
                
                val dashLine = lines[dashLineIndex]
                val headerLine = lines[dashLineIndex - 1]
                
                // Parse dash groups
                val dashGroups = mutableListOf<IntRange>()
                var start = -1
                for (i in 0 until dashLine.length) {
                    if (dashLine[i] == '-') {
                        if (start == -1) start = i
                    } else {
                        if (start != -1) {
                            dashGroups.add(start until i)
                            start = -1
                        }
                    }
                }
                if (start != -1) {
                    dashGroups.add(start until dashLine.length)
                }
                
                var refNumCol: IntRange? = null
                var yearMadeCol: IntRange? = null
                var numberCol: IntRange? = null
                var myCommentsCol: IntRange? = null
                
                for (range in dashGroups) {
                    if (range.last >= headerLine.length) continue
                    val headerText = headerLine.substring(range.first, range.last + 1).trim()
                    when {
                        headerText.equals("Reg.", ignoreCase = true) || headerText.equals("Reg", ignoreCase = true) || headerText.equals("Car Number", ignoreCase = true) -> {
                            refNumCol = range
                        }
                        headerText.equals("Made", ignoreCase = true) || headerText.equals("Year Made", ignoreCase = true) -> {
                            yearMadeCol = range
                        }
                        headerText.equals("#", ignoreCase = true) || headerText.equals("Racing Number", ignoreCase = true) -> {
                            numberCol = range
                        }
                        headerText.equals("My comments", ignoreCase = true) || headerText.equals("Internal Comments", ignoreCase = true) -> {
                            myCommentsCol = range
                        }
                    }
                }
                
                if (refNumCol == null) continue
                
                for (i in (dashLineIndex + 1) until lines.size) {
                    val line = lines[i]
                    if (line.length <= refNumCol.last) continue
                    val refStr = line.substring(refNumCol.first, refNumCol.last + 1).trim()
                    val refNum = refStr.toIntOrNull() ?: continue
                    
                    val yearMade = if (yearMadeCol != null && line.length > yearMadeCol.first) {
                        val end = minOf(yearMadeCol.last, line.length - 1)
                        line.substring(yearMadeCol.first, end + 1).trim()
                    } else ""
                    
                    val number = if (numberCol != null && line.length > numberCol.first) {
                        val end = minOf(numberCol.last, line.length - 1)
                        line.substring(numberCol.first, end + 1).trim()
                    } else ""
                    
                    val myComments = if (myCommentsCol != null && line.length > myCommentsCol.first) {
                        val end = minOf(myCommentsCol.last, line.length - 1)
                        line.substring(myCommentsCol.first, end + 1).trim()
                    } else ""
                    
                    db.execute(
                        "UPDATE toys SET year_made = ?, number = ?, my_comments = ? WHERE ref_num = ? AND toy_type = ?",
                        listOf(yearMade, number, myComments, refNum, category)
                    )
                    totalUpdated++
                }
            } catch (e: Exception) {
                GcLog.e(TAG, "Error backfilling $category from $filename: ${e.message}", e)
            }
        }
        GcLog.d(TAG, "Backfill completed. Updated $totalUpdated toys.")
        return totalUpdated
    }

    fun exportHtml(db: ToyDatabase, exportDirectory: String): Int {
        GcLog.d(TAG, "Starting HTML export...")
        
        // 1. Run one-time backfill from LST files if they exist in the target folder
        backfillFromLstFiles(db, exportDirectory)
        
        val dateStr = getCurrentDateString()
        var totalFilesGenerated = 0
        val fs = FileSystem.SYSTEM
        val targetDir = exportDirectory.toPath()
        
        // Query all makers from database
        val makersList = mutableListOf<Maker>()
        val makersCursor = db.query("SELECT * FROM makers ORDER BY name COLLATE NOCASE")
        while (makersCursor.next()) {
            makersList.add(
                Maker(
                    name = makersCursor.getString("name") ?: "",
                    country = makersCursor.getString("country") ?: "",
                    bitmaps = makersCursor.getString("bitmaps") ?: "",
                    bitmapsSize = makersCursor.getString("bitmaps_size") ?: "",
                    bitmapsTimeStamp = makersCursor.getString("bitmaps_timestamp") ?: "",
                    comments = makersCursor.getString("comments") ?: ""
                )
            )
        }
        makersCursor.close()

        for (config in categoryConfigs) {
            // Query active toys for this category
            val toys = mutableListOf<Toy>()
            val sql = "SELECT * FROM toys WHERE toy_type = ? AND (traded IS NULL OR traded = '') ORDER BY body_maker COLLATE NOCASE, description COLLATE NOCASE, maker_combo COLLATE NOCASE"
            val cursor = db.query(sql, listOf(config.toyType))
            while (cursor.next()) {
                toys.add(
                    Toy(
                        refNum = cursor.getInt("ref_num") ?: 0,
                        toyType = config.toyType,
                        description = cursor.getString("description") ?: "",
                        makerCombo = cursor.getString("maker_combo") ?: "",
                        scale = cursor.getString("scale") ?: "",
                        factoryCar = cursor.getString("factory_car") ?: "n",
                        bodyMaker = cursor.getString("body_maker") ?: "",
                        acquired = cursor.getString("acquired") ?: "",
                        chassisType = cursor.getString("chassis_type") ?: "",
                        chassisMaker = cursor.getString("chassis_maker") ?: "",
                        condition = cursor.getString("condition") ?: "",
                        color = cursor.getString("color") ?: "",
                        motorMaker = cursor.getString("motor_maker") ?: "",
                        motorDetails = cursor.getString("motor_details") ?: "",
                        catalogNumber = cursor.getString("catalog_number") ?: "",
                        comments = cursor.getString("comments") ?: "",
                        majorWork = cursor.getString("major_work") ?: "",
                        minorWork = cursor.getString("minor_work") ?: "",
                        repro = cursor.getString("repro") ?: "",
                        value = cursor.getDouble("value") ?: 0.0,
                        amountPaid = cursor.getDouble("amount_paid") ?: 0.0,
                        amountSold = cursor.getString("amount_sold") ?: "",
                        traded = cursor.getString("traded") ?: "",
                        buy = cursor.getString("buy") ?: "",
                        maintenance = cursor.getString("maintenance") ?: "",
                        toMake = cursor.getString("to_make") ?: "",
                        detail = cursor.getString("detail") ?: "",
                        boxed = cursor.getString("boxed") ?: "n",
                        picture = cursor.getString("picture") ?: "",
                        pictureSize = cursor.getInt("picture_size") ?: 0,
                        pictureTimeStamp = cursor.getDouble("picture_timestamp")?.toLong() ?: 0L,
                        hasPicture = cursor.getString("has_picture") ?: "n",
                        bitmaps = cursor.getString("bitmaps") ?: "",
                        bitmapsSize = cursor.getString("bitmaps_size") ?: "",
                        bitmapsTimeStamp = cursor.getString("bitmaps_timestamp") ?: "",
                        yearMade = cursor.getString("year_made") ?: "",
                        number = cursor.getString("number") ?: "",
                        myComments = cursor.getString("my_comments") ?: ""
                    )
                )
            }
            cursor.close()

            if (toys.isEmpty()) continue

            // Group toys by brand/bodyMaker
            val toysByBrand = toys.groupBy { it.bodyMaker.trim() }
            val sortedBrands = toysByBrand.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)

            // A. Generate Category Maker list page: [imagePrefix]maker.html
            val makerFilename = "${config.imagePrefix}maker.html"
            val makerPath = targetDir.div(makerFilename)
            val makerHtml = StringBuilder()
            
            makerHtml.append("<html><head><title>${config.mainTitle} by brand</title></head><body>")
            makerHtml.append("<body bgcolor=\"#FFFFFF\">")
            makerHtml.append("<center><strong>${config.mainTitle} by brand</strong></center>")
            makerHtml.append("<p><center>Collection organized by brand. Click on brand name to get list</center>")
            makerHtml.append("<p><center><img src=\"working.gif\">Last updated on <font color=red>$dateStr</font>")
            makerHtml.append("<p><TABLE>\n")

            // B. Generate Brand pages and append links to maker index page
            var secondBodyBrandCount = 0
            var factoryCount = 0
            var reproCount = 0
            var countHO = 0
            var count24 = 0
            var count44 = 0
            var countN = 0

            sortedBrands.forEachIndexed { brandIndex, brandName ->
                val brandToys = toysByBrand[brandName] ?: emptyList()
                val brandHtmlFilename = "${config.imagePrefix}m_${brandIndex}.html"
                
                // Add to maker list
                makerHtml.append("<td><a href=\"$brandHtmlFilename\">$brandName (${brandToys.size}) </a>")
                secondBodyBrandCount++
                if (secondBodyBrandCount > 7) {
                    makerHtml.append("<tr>\n")
                    secondBodyBrandCount = 0
                }

                // Compile stats and generate detail page for each toy
                var brandFacCount = 0
                var brandRepCount = 0

                brandToys.forEach { toy ->
                    val hasSlash = toy.makerCombo.contains("/")
                    val startsWithScratch = toy.makerCombo.startsWith("scratch", ignoreCase = true)
                    val startsWithValdetaro = toy.bodyMaker.startsWith("Valdetaro", ignoreCase = true)
                    val isRepro = toy.repro.startsWith("y", ignoreCase = true)

                    if (!hasSlash && !startsWithScratch && !startsWithValdetaro && !isRepro) {
                        factoryCount++
                        brandFacCount++
                    }
                    if (isRepro) {
                        reproCount++
                        brandRepCount++
                    }

                    val s = toy.scale
                    if (s.startsWith("1/6") || s.startsWith("1/7") || s.startsWith("1/8") || s.startsWith("1/9")) {
                        countHO++
                    } else if (s.equals("1/12") || s.equals("1/18") || s.startsWith("1/2")) {
                        count24++
                    } else if (s.startsWith("1/4") || s.startsWith("1/5")) {
                        count44++
                    } else if (s.startsWith("1/144") || s.startsWith("1/160")) {
                        countN++
                    }

                    // Generate Single Toy page: [imagePrefix]_[refNum].html
                    val toyFilename = "${config.imagePrefix}_${toy.refNum}.html"
                    val toyPath = targetDir.div(toyFilename)
                    val toyHtml = StringBuilder()

                    toyHtml.append("<html><head><title>${toy.description}</title></head><body>\n")
                    toyHtml.append("<body bgcolor=\"#FFFFFF\">\n")
                    val reproductionText = if (isRepro) " * REPRODUCTION * " else ""
                    toyHtml.append("<center><strong>${toy.description} $reproductionText ${toy.condition}</strong><br>${toy.bodyMaker}\n")
                    toyHtml.append("<p><img src=\"working.gif\">Last updated on <font color=red>$dateStr</font>\n")
                    toyHtml.append("<center><table><td><center><table><td>\n")

                    val picFileLower = "${config.imagePrefix}${toy.refNum}.jpg".lowercase()
                    if (toy.picture.isNotEmpty()) {
                        toyHtml.append("<center><a href=\"$picFileLower\"><img src=$picFileLower width=320></a></center>\n")
                        if (toy.hasPicture.startsWith("y", ignoreCase = true)) {
                            toyHtml.append("<br><center>* Actual model *<p></center>\n")
                        } else {
                            toyHtml.append("<br><center>* Similar model *<p></center>\n")
                        }
                    } else {
                        toyHtml.append("<p><center>No picture of this model is available yet.<center>\n<p>")
                    }

                    val bitmapsList = toy.bitmaps.split(" ").filter { it.trim().isNotEmpty() }
                    if (bitmapsList.isNotEmpty()) {
                        toyHtml.append("<center>\n")
                        bitmapsList.forEach { bm ->
                            val bmLower = bm.lowercase()
                            toyHtml.append(" <a href=\"$bmLower\"> <img src=$bmLower width=\"100\"></a>\n")
                        }
                        toyHtml.append("</center>\n")
                    }

                    toyHtml.append("<p><TABLE>\n")
                    toyHtml.append("<td>${toy.scale}<td><td>${toy.catalogNumber}<tr>\n")
                    toyHtml.append("<td>Brand:<td><td>${toy.bodyMaker}<tr>\n")
                    toyHtml.append("<td>Chassis:<td><td>${toy.chassisMaker} ${toy.chassisType} (*)<tr>\n")
                    toyHtml.append("<td>Motor:<td><td>${toy.motorDetails}<tr>\n")
                    toyHtml.append("<td>Color:<td>${toy.color}<td><td>Number:${toy.number}<td><td>Year made: ${toy.yearMade}<tr>\n")
                    if (toy.boxed.startsWith("y", ignoreCase = true) || toy.boxed.equals("1")) {
                        toyHtml.append("<td>Boxed<td><td><tr>\n")
                    }
                    toyHtml.append("</TABLE>\n")
                    toyHtml.append("<tr></table>\n")
                    toyHtml.append("<p>${toy.comments}\n")
                    toyHtml.append("<p>(*) Chassis convention:<br>P = Plastic, A = Aluminum, CA = Cast Aluminum,\n")
                    toyHtml.append("<br>B = Brass, M = Magnesium, NP = Nickel Plated , R = Resin, W = Wood, \n")
                    toyHtml.append("<br>T = Tin, S = Steel, AA = Anodised Aluminum, C - Circuit board\n")
                    toyHtml.append("<br>TJ = 'Tjet' vertical motor shaft, IF = ISO-Fulcrum, V = Vibrator\n")
                    toyHtml.append("<p>Back to the list of <a href=\"$brandHtmlFilename\">brand '${toy.bodyMaker}'</a><HR>Build ${CommonConfig.versionCodeString}</body></HTML>\n")

                    fs.write(toyPath) {
                        write(toyHtml.toString().toIso8859_1Bytes())
                    }
                    totalFilesGenerated++
                }

                // Generate Brand Page: [makerPrefix]m_[brandIndex].html
                val brandPath = targetDir.div(brandHtmlFilename)
                val brandHtml = StringBuilder()
                brandHtml.append("<html><head><title>${config.mainTitle} of brand '$brandName'</title></head><body>")
                brandHtml.append("<body bgcolor=\"#FFFFFF\">")
                brandHtml.append("<center><strong>${config.mainTitle} of brand '$brandName'</strong></center>")

                val makerObj = makersList.find { it.name.equals(brandName, ignoreCase = true) }
                val brandCountry = makerObj?.country ?: ""
                val brandComments = makerObj?.comments ?: ""
                val brandBitmaps = makerObj?.bitmaps ?: ""

                brandHtml.append("<p><center>Manufacturer from $brandCountry</center>")
                brandHtml.append("<p><center><img src=\"working.gif\">Last updated on <font color=red>$dateStr</font><p>")
                brandHtml.append("<TABLE><td><td><td><td><tr>")

                var brandPicCount = 8
                brandToys.forEach { toy ->
                    brandPicCount++
                    if (brandPicCount >= 8) {
                        brandHtml.append("<tr>")
                        brandPicCount = 0
                    }
                    brandHtml.append("<td><center>")
                    val picFileLower = "${config.imagePrefix}${toy.refNum}.jpg".lowercase()
                    if (toy.picture.isNotEmpty()) {
                        brandHtml.append("<a href=\"${config.imagePrefix}_${toy.refNum}.html\"><img src=$picFileLower alt=\"${toy.description}\" width=\"100\">")
                    } else {
                        brandHtml.append("<a href=\"${config.imagePrefix}_${toy.refNum}.html\">(No Picture)<p>")
                    }
                    brandHtml.append("<br><font size=1>${toy.description}")
                    if (toy.repro.startsWith("y", ignoreCase = true)) {
                        brandHtml.append(" - Reproduction")
                    }
                    brandHtml.append("</font></a></center><td>")
                }

                brandHtml.append("</table><p>Total of brand '$brandName': $brandFacCount factory models, $brandRepCount reproductions, ${brandToys.size} total.</center>")
                if (brandComments.isNotEmpty()) {
                    brandHtml.append("<p>$brandComments")
                }

                val makerBitmapsList = brandBitmaps.split(" ").filter { it.trim().isNotEmpty() }
                if (makerBitmapsList.isNotEmpty()) {
                    brandHtml.append("<p><center>")
                    makerBitmapsList.forEach { bm ->
                        brandHtml.append("  <img src=${bm.lowercase()} width=\"300\">")
                    }
                    brandHtml.append("</center>")
                }

                brandHtml.append("<p>Back to the <a href=\"$makerFilename\">Brand list</a></center>")
                brandHtml.append("<HR>Build ${CommonConfig.versionCodeString}</body></HTML>")

                fs.write(brandPath) {
                    write(brandHtml.toString().toIso8859_1Bytes())
                }
                totalFilesGenerated++
            }

            // Close index and write maker page
            val count32 = toys.size - count24 - count44 - countHO - countN
            makerHtml.append("</table><p>Total in list: ${toys.size} models from ${sortedBrands.size} different brands, $factoryCount of them being factory models, ")
            makerHtml.append("$reproCount are reproductions.<br>$count24 are 1/28 scale or bigger, $count32 are 1/32 scale, $count44 are O scale, $countHO are HO scale and $countN are N scale or smaller.<br>")
            
            if (config.imagePrefix.startsWith("car", ignoreCase = true)) {
                makerHtml.append("</center><HR><center><img src='pistagif.gif'></center><HR><br><img SRC=\"http://www.truegem.net/cgi-bin/gifcounter/valdetaro/collection${config.imagePrefix}\"--><br>Build ${CommonConfig.versionCodeString}</body></HTML>")
            } else {
                makerHtml.append("</center><HR><center><img src='pistagif.gif'><center><HR><br><img SRC=\"http://www.truegem.net/cgi-bin/gifcounter/valdetaro/trdlist${config.imagePrefix}\"--><br>Build ${CommonConfig.versionCodeString}</body></HTML>")
            }

            fs.write(makerPath) {
                write(makerHtml.toString().toIso8859_1Bytes())
            }
            totalFilesGenerated++
        }
        
        GcLog.d(TAG, "HTML export completed. Generated $totalFilesGenerated files.")
        return totalFilesGenerated
    }
}
