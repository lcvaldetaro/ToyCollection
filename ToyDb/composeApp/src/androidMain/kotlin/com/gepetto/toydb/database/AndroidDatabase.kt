package com.gepetto.toydb.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import club.gepetto.GcLog

class AndroidSqlCursor(private val cursor: Cursor) : SqlCursor {
    override fun next(): Boolean = cursor.moveToNext()
    override fun getString(columnName: String): String? {
        val idx = cursor.getColumnIndex(columnName)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
    }
    override fun getInt(columnName: String): Int? {
        val idx = cursor.getColumnIndex(columnName)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getInt(idx) else null
    }
    override fun getDouble(columnName: String): Double? {
        val idx = cursor.getColumnIndex(columnName)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getDouble(idx) else null
    }
    override fun close() {
        cursor.close()
    }
}

class AndroidToyDatabase(context: Context, dbName: String) : ToyDatabase {
    private val helper = object : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            val wrapper = object : ToyDatabase {
                override fun execute(sql: String, bindArgs: List<Any?>) {
                    if (bindArgs.isEmpty()) {
                        db.execSQL(sql)
                    } else {
                        val args = bindArgs.map { it?.toString() }.toTypedArray()
                        db.execSQL(sql, args)
                    }
                }
                override fun query(sql: String, bindArgs: List<String>): SqlCursor {
                    val args = if (bindArgs.isEmpty()) null else bindArgs.toTypedArray()
                    return AndroidSqlCursor(db.rawQuery(sql, args))
                }
                override fun close() {
                }
            }
            CREATE_SCHEMA_SQL_LIST.forEach { sql ->
                wrapper.execute(sql)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            val wrapper = object : ToyDatabase {
                override fun execute(sql: String, bindArgs: List<Any?>) {
                    if (bindArgs.isEmpty()) {
                        db.execSQL(sql)
                    } else {
                        val args = bindArgs.map { it?.toString() }.toTypedArray()
                        db.execSQL(sql, args)
                    }
                }
                override fun query(sql: String, bindArgs: List<String>): SqlCursor {
                    val args = if (bindArgs.isEmpty()) null else bindArgs.toTypedArray()
                    return AndroidSqlCursor(db.rawQuery(sql, args))
                }
                override fun close() {
                }
            }
            for (v in (oldVersion + 1)..newVersion) {
                runMigration(wrapper, v)
            }
        }
    }
    private val db = helper.writableDatabase

    init {
        checkUpgrade(this)
    }

    override fun execute(sql: String, bindArgs: List<Any?>) {
        if (bindArgs.isEmpty()) {
            db.execSQL(sql)
        } else {
            val args = bindArgs.map { it?.toString() }.toTypedArray()
            db.execSQL(sql, args)
        }
    }

    override fun query(sql: String, bindArgs: List<String>): SqlCursor {
        val args = if (bindArgs.isEmpty()) null else bindArgs.toTypedArray()
        val cursor = db.rawQuery(sql, args)
        return AndroidSqlCursor(cursor)
    }

    override fun close() {
        helper.close()
    }
}

actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase {
    val context = platformContext as? Context ?: throw IllegalArgumentException("Android context required")
    return AndroidToyDatabase(context, dbName)
}
