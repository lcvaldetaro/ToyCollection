package com.gepetto.toydb.database

import club.gepetto.GcLog
import kotlinx.serialization.Serializable

const val DATABASE_VERSION = 7
const val TAG = "ToyDbDatabase"

interface SqlCursor {
    fun next(): Boolean
    fun getString(columnName: String): String?
    fun getInt(columnName: String): Int?
    fun getDouble(columnName: String): Double?
    fun close()
}

interface ToyDatabase {
    fun execute(sql: String, bindArgs: List<Any?> = emptyList())
    fun query(sql: String, bindArgs: List<String> = emptyList()): SqlCursor
    fun close()
}

@Serializable
data class Toy(
    val refNum: Int,
    val toyType: String, // 'slot', 'train', 'static', 'kit', 'misc'
    val description: String,
    val makerCombo: String = "",
    val scale: String = "",
    val factoryCar: String = "n", // 'y' or 'n'
    val bodyMaker: String = "",
    val acquired: String = "",
    val chassisType: String = "",
    val chassisMaker: String = "",
    val condition: String = "",
    val color: String = "",
    val motorMaker: String = "",
    val motorDetails: String = "",
    val catalogNumber: String = "",
    val comments: String = "",
    val majorWork: String = "",
    val minorWork: String = "",
    val repro: String = "", // 'y', 'r', ''
    val value: Double = 0.0,
    val amountPaid: Double = 0.0,
    val amountSold: String = "", // text due to 'Traded'
    val traded: String = "",
    val buy: String = "",
    val maintenance: String = "",
    val toMake: String = "",
    val detail: String = "",
    val boxed: String = "n", // 'y' or 'n'
    // Image details
    val picture: String = "",
    val pictureSize: Int = 0,
    val pictureTimeStamp: Long = 0L,
    val hasPicture: String = "n", // 'y' or 'n'
    // List of secondary images parsed
    val bitmaps: String = "",
    val bitmapsSize: String = "",
    val bitmapsTimeStamp: String = "",
    val yearMade: String = "",
    val number: String = "",
    val myComments: String = ""
) {
    /**
     * Resolves the primary image filename based on category prefix rules.
     */
    fun resolvePrimaryImageFilename(prefix: String): String? {
        if (hasPicture != "y" && picture.isEmpty()) return null
        if (picture.isNotEmpty()) return picture
        // Fallback or dynamic lookup
        return "${prefix}${refNum}" // Extension will be resolved dynamically by Coil or file exists check
    }

    /**
     * Parse space-separated secondary images.
     */
    fun getSecondaryImages(): List<ToyImage> {
        val names = bitmaps.split(" ").filter { it.trim().isNotEmpty() }
        val sizes = bitmapsSize.split(" ").filter { it.trim().isNotEmpty() }
        val times = bitmapsTimeStamp.split(" ").filter { it.trim().isNotEmpty() }
        
        return names.mapIndexed { index, name ->
            ToyImage(
                filename = name,
                size = sizes.getOrNull(index)?.toIntOrNull() ?: 0,
                timestamp = times.getOrNull(index)?.toLongOrNull() ?: 0L
            )
        }
    }
}

data class ToyImage(
    val filename: String,
    val size: Int,
    val timestamp: Long
)

@Serializable
data class Maker(
    val name: String,
    val country: String = "",
    val bitmaps: String = "",
    val bitmapsSize: String = "",
    val bitmapsTimeStamp: String = "",
    val comments: String = ""
)

@Serializable
data class CategorySetting(
    val category: String, // slots, trains, static, kits, misc
    val imagePrefix: String,
    val label: String,
    val title: String = "",
    val icon: String = "category"
)

// SQL CREATE SCHEMA
val CREATE_SCHEMA_SQL_LIST = listOf(
    """
    CREATE TABLE IF NOT EXISTS category_settings (
        category TEXT PRIMARY KEY,
        image_prefix TEXT NOT NULL,
        label TEXT NOT NULL,
        title TEXT NOT NULL DEFAULT '',
        icon TEXT NOT NULL DEFAULT 'category'
    );
    """.trimIndent(),
    
    """
    INSERT OR REPLACE INTO category_settings (category, image_prefix, label, title, icon) VALUES
    ('slot', 'car', 'Slot Cars', 'Slot car', 'car'),
    ('train', 'tra', 'Model Trains', 'Train', 'train'),
    ('static', 'sta', 'Static Models', 'Static Models', 'car'),
    ('kit', 'pla', 'Model Kits', 'Models Kit', 'build'),
    ('misc', 'mis', 'Others', 'Miscelaneous toys', 'category');
    """.trimIndent(),
    
    """
    CREATE TABLE IF NOT EXISTS makers (
        name TEXT PRIMARY KEY,
        country TEXT,
        bitmaps TEXT,
        bitmaps_size TEXT,
        bitmaps_timestamp TEXT,
        comments TEXT
    );
    """.trimIndent(),
    
    """
    CREATE TABLE IF NOT EXISTS toys (
        ref_num INTEGER NOT NULL,
        toy_type TEXT NOT NULL REFERENCES category_settings(category),
        description TEXT NOT NULL,
        maker_combo TEXT,
        scale TEXT,
        factory_car TEXT DEFAULT 'n',
        body_maker TEXT,
        acquired TEXT,
        chassis_type TEXT,
        chassis_maker TEXT,
        condition TEXT,
        color TEXT,
        motor_maker TEXT,
        motor_details TEXT,
        catalog_number TEXT,
        comments TEXT,
        major_work TEXT,
        minor_work TEXT,
        repro TEXT,
        value REAL DEFAULT 0.0,
        amount_paid REAL DEFAULT 0.0,
        amount_sold TEXT,
        traded TEXT,
        buy TEXT,
        maintenance TEXT,
        to_make TEXT,
        detail TEXT,
        boxed TEXT DEFAULT 'n',
        picture TEXT,
        picture_size INTEGER DEFAULT 0,
        picture_timestamp INTEGER DEFAULT 0,
        has_picture TEXT DEFAULT 'n',
        bitmaps TEXT,
        bitmaps_size TEXT,
        bitmaps_timestamp TEXT,
        year_made TEXT DEFAULT '',
        number TEXT DEFAULT '',
        my_comments TEXT DEFAULT '',
        PRIMARY KEY (toy_type, ref_num)
    );
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
    );
    """.trimIndent(),

    """
    INSERT OR IGNORE INTO app_settings (key, value) VALUES ('theme', '0');
    """.trimIndent()
)

fun checkUpgrade(db: ToyDatabase, currentVersion: Int = DATABASE_VERSION) {
    GcLog.d(TAG, "Checking database upgrade. Target version = $currentVersion")
    var tableExists = false
    try {
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='category_settings'")
        if (cursor.next()) {
            tableExists = true
        }
        cursor.close()
    } catch (e: Exception) {
        GcLog.e(TAG, "Error checking table existence: ${e.message}", e)
    }

    var oldVersion = 0
    try {
        val cursor = db.query("PRAGMA user_version")
        if (cursor.next()) {
            oldVersion = cursor.getInt("user_version") ?: 0
        }
        cursor.close()
    } catch (e: Exception) {
        GcLog.e(TAG, "Error checking user_version: ${e.message}", e)
    }

    GcLog.d(TAG, "Current database version = $oldVersion, tableExists = $tableExists")

    if (!tableExists || oldVersion == 0) {
        GcLog.d(TAG, "Database is uninitialized or missing tables. Creating tables...")
        CREATE_SCHEMA_SQL_LIST.forEach { sql ->
            db.execute(sql)
        }
        db.execute("PRAGMA user_version = $currentVersion")
        GcLog.d(TAG, "Database initialized successfully to version $currentVersion")
    } else if (oldVersion < currentVersion) {
        GcLog.d(TAG, "Upgrading database from version $oldVersion to $currentVersion")
        for (v in (oldVersion + 1)..currentVersion) {
            runMigration(db, v)
        }
        db.execute("PRAGMA user_version = $currentVersion")
        GcLog.d(TAG, "Database upgraded successfully to version $currentVersion")
    }
    
    // Run self-healing updates to fix any standard category icons that were reset to 'category'
    try {
        db.execute("UPDATE category_settings SET icon = 'car' WHERE category = 'slot' AND icon = 'category'")
        db.execute("UPDATE category_settings SET icon = 'train' WHERE category = 'train' AND icon = 'category'")
        db.execute("UPDATE category_settings SET icon = 'car' WHERE category = 'static' AND icon = 'category'")
        db.execute("UPDATE category_settings SET icon = 'build' WHERE category = 'kit' AND icon = 'category'")
    } catch (e: Exception) {
        GcLog.e(TAG, "Error executing self-healing icon migrations: ${e.message}", e)
    }
}

fun runMigration(db: ToyDatabase, version: Int) {
    GcLog.d(TAG, "Executing migration to version $version")
    when (version) {
        2 -> {
            db.execute("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            db.execute("INSERT OR IGNORE INTO app_settings (key, value) VALUES ('theme', '0')")
        }
        3 -> {
            db.execute("UPDATE category_settings SET label = 'Others' WHERE category = 'misc'")
        }
        4 -> {
            db.execute("ALTER TABLE toys ADD COLUMN year_made TEXT DEFAULT ''")
            db.execute("ALTER TABLE toys ADD COLUMN number TEXT DEFAULT ''")
            db.execute("ALTER TABLE toys ADD COLUMN my_comments TEXT DEFAULT ''")
        }
        5 -> {
            db.execute("ALTER TABLE category_settings ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            db.execute("UPDATE category_settings SET title = 'Slot car' WHERE category = 'slot'")
            db.execute("UPDATE category_settings SET title = 'Train' WHERE category = 'train'")
            db.execute("UPDATE category_settings SET title = 'Static Models' WHERE category = 'static'")
            db.execute("UPDATE category_settings SET title = 'Models Kit' WHERE category = 'kit'")
            db.execute("UPDATE category_settings SET title = 'Miscelaneous toys' WHERE category = 'misc'")
        }
        6 -> {
            db.execute("UPDATE category_settings SET title = 'Slot car' WHERE category = 'slot' AND (title IS NULL OR title = '')")
            db.execute("UPDATE category_settings SET title = 'Train' WHERE category = 'train' AND (title IS NULL OR title = '')")
            db.execute("UPDATE category_settings SET title = 'Static Models' WHERE category = 'static' AND (title IS NULL OR title = '')")
            db.execute("UPDATE category_settings SET title = 'Models Kit' WHERE category = 'kit' AND (title IS NULL OR title = '')")
            db.execute("UPDATE category_settings SET title = 'Miscelaneous toys' WHERE category = 'misc' AND (title IS NULL OR title = '')")
        }
        7 -> {
            db.execute("ALTER TABLE category_settings ADD COLUMN icon TEXT NOT NULL DEFAULT 'category'")
            db.execute("UPDATE category_settings SET icon = 'car' WHERE category = 'slot'")
            db.execute("UPDATE category_settings SET icon = 'train' WHERE category = 'train'")
            db.execute("UPDATE category_settings SET icon = 'car' WHERE category = 'static'")
            db.execute("UPDATE category_settings SET icon = 'build' WHERE category = 'kit'")
            db.execute("UPDATE category_settings SET icon = 'category' WHERE category = 'misc'")
        }
    }
}

expect fun createDatabase(platformContext: Any? = null, dbName: String = "toydb.db"): ToyDatabase

