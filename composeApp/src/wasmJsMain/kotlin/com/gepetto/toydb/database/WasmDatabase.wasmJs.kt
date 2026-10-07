@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.gepetto.toydb.database

import club.gepetto.GcLog
import kotlin.io.encoding.Base64
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.toJsNumber
import kotlin.js.toJsString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import toydb.composeapp.generated.resources.Res

private const val SNAPSHOT_KEY = "database_snapshot"
private const val SAVE_DELAY_MS = 1000L
private const val TAG_WASM = "WasmToyDatabase"

/**
 * ISSUE-26 (Rev 6): sql.js errors reach Kotlin as JsException, which is a Throwable but not an Exception.
 * The shared code (ToyRepository, checkUpgrade) catches Exception only, the same as JDBC's SQLException on Desktop.
 */
class SqlJsException(message: String?, cause: Throwable?) : Exception(message, cause)

private inline fun <T> sqlCall(sql: String, block: () -> T): T = try {
    block()
} catch (e: Throwable) {
    throw if (e is Exception) e else SqlJsException("${e.message} [${sql.take(80)}]", e)
}

class WasmSqlCursor(private val columns: List<String>, private val rows: List<List<Any?>>) : SqlCursor {
    private var index = -1
    override fun next(): Boolean = ++index < rows.size

    private fun cell(name: String): Any? {
        val col = columns.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (col < 0) {
            if (columns.size == 1 && rows[index].isNotEmpty()) rows[index][0] else null
        } else rows[index][col]
    }

    override fun getString(columnName: String): String? = when (val v = cell(columnName)) {
        null -> null
        is Double -> if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    override fun getInt(columnName: String): Int? = when (val v = cell(columnName)) {
        null -> null
        is Double -> v.toInt()
        else -> v.toString().toIntOrNull()
    }

    override fun getDouble(columnName: String): Double? = when (val v = cell(columnName)) {
        null -> null
        is Double -> v
        else -> v.toString().toDoubleOrNull()
    }

    override fun close() {}
}

class WasmToyDatabase private constructor(private val db: SqlJsDatabase) : ToyDatabase {
    private val scope: CoroutineScope = MainScope()
    private var saveJob: Job? = null
    private var dirty = false

    private fun List<Any?>.toJsParams(): JsArray<JsAny?>? {
        if (isEmpty()) return null
        val out = JsArray<JsAny?>()
        forEachIndexed { i, v ->
            out[i] = when (v) {
                null -> null
                is Int -> v.toJsNumber()
                is Long -> v.toDouble().toJsNumber()
                is Double -> v.toJsNumber()
                is Boolean -> (if (v) 1 else 0).toJsNumber()
                else -> v.toString().toJsString()
            }
        }
        return out
    }

    override fun execute(sql: String, bindArgs: List<Any?>) {
        sqlCall(sql) { db.run(sql, bindArgs.toJsParams()) }
        markDirty()
    }

    override fun query(sql: String, bindArgs: List<String>): SqlCursor {
        val results = sqlCall(sql) { db.exec(sql, bindArgs.toJsParams()) }
        if (results.length == 0) return WasmSqlCursor(emptyList(), emptyList())
        val first = results[0]!!
        val columns = List(first.columns.length) { first.columns[it].toString() }
        val rows = List(first.values.length) { r ->
            val row = first.values[r]!!
            List(row.length) { c ->
                val v = row[c]
                when (cellKind(v)) {
                    0 -> null
                    1 -> v!!.unsafeCast<kotlin.js.JsNumber>().toDouble()
                    else -> v.toString()
                }
            }
        }
        return WasmSqlCursor(columns, rows)
    }

    private fun markDirty() {
        dirty = true
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            flush()
        }
    }

    suspend fun flush() {
        if (!dirty) return
        dirty = false
        try {
            idbPut(SNAPSHOT_KEY, db.export()).await<JsAny?>()
        } catch (e: Throwable) {
            dirty = true
            GcLog.e(e, "$TAG_WASM: failed to persist database snapshot: ${e.message}")
        }
    }

    override fun <T> transaction(block: () -> T): T {
        execute("BEGIN")
        return try {
            val result = block()
            execute("COMMIT")
            result
        } catch (e: Throwable) {
            try {
                execute("ROLLBACK")
            } catch (rbEx: Throwable) {
                GcLog.w("$TAG_WASM: Rollback failed: ${rbEx.message}")
            }
            throw e
        }
    }

    override fun close() {
        db.close()
    }

    fun toysCount(): Int {
        val c = query("SELECT COUNT(*) as total FROM toys")
        val n = if (c.next()) c.getInt("total") ?: 0 else 0
        c.close()
        return n
    }

    companion object {
        suspend fun open(): WasmToyDatabase {
            val sql = initSqlJs(sqlJsConfig()).await<SqlJsStatic>()
            var raw: SqlJsDatabase? = null
            // ISSUE-25: IndexedDB can be blocked (for example private mode). Continue without a snapshot.
            val snapshot = try {
                idbGet(SNAPSHOT_KEY).await<org.khronos.webgl.Uint8Array?>()
            } catch (e: Throwable) {
                GcLog.e(e, "$TAG_WASM: cannot read the stored snapshot, starting from default database: ${e.message}")
                null
            }
            if (snapshot != null) {
                try { raw = newSqlJsDatabase(sql, snapshot) } catch (e: Throwable) {
                    GcLog.e(e, "$TAG_WASM: stored snapshot is unreadable, starting from default database: ${e.message}")
                }
            }
            val isInitialInstall = raw == null
            if (raw == null) {
                val bytes = Res.readBytes("files/default_toydb.db")
                raw = newSqlJsDatabase(sql, base64ToUint8Array(Base64.encode(bytes)))
            }
            val database = WasmToyDatabase(raw)
            checkUpgrade(database)
            if (isInitialInstall) {
                database.execute("DELETE FROM toys")
                database.execute("DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'")
                database.flush()
            }
            onPageHidden { database.scope.launch { database.flush() } }
            return database
        }
    }
}

actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase =
    error("On web, call WasmToyDatabase.open() from main() instead of createDatabase().")
