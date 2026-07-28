package com.gepetto.toydb.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import club.gepetto.GcLog

class AndroidSqlCursor(private val cursor: Cursor) : SqlCursor {
    override fun next(): Boolean = cursor.moveToNext()
    
    override fun getString(columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        if (index == -1 || cursor.isNull(index)) return null
        return cursor.getString(index)
    }
    
    override fun getInt(columnName: String): Int? {
        val index = cursor.getColumnIndex(columnName)
        if (index == -1 || cursor.isNull(index)) return null
        return cursor.getInt(index)
    }
    
    override fun getDouble(columnName: String): Double? {
        val index = cursor.getColumnIndex(columnName)
        if (index == -1 || cursor.isNull(index)) return null
        return cursor.getDouble(index)
    }
    
    override fun close() {
        cursor.close()
    }
}

class AndroidToyDatabase(context: Context, dbName: String) : ToyDatabase {
    private val db: SQLiteDatabase

    init {
        // Open or create the database in private mode
        db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
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
        db.close()
    }
}

actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase {
    val context = platformContext as? Context ?: throw IllegalArgumentException("platformContext must be an Android Context")
    return AndroidToyDatabase(context, dbName)
}
