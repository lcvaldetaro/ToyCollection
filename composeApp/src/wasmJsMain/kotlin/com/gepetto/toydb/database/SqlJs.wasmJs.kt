@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.database

import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

external interface SqlJsQueryResult : JsAny {
    val columns: JsArray<JsString>
    val values: JsArray<JsArray<JsAny?>>
}

external interface SqlJsDatabase : JsAny {
    fun run(sql: String, params: JsArray<JsAny?>?)
    fun exec(sql: String, params: JsArray<JsAny?>?): JsArray<SqlJsQueryResult>
    fun export(): Uint8Array
    fun close()
}

external interface SqlJsStatic : JsAny

@JsModule("sql.js")
external fun initSqlJs(config: JsAny): Promise<SqlJsStatic>

@JsFun("() => ({ locateFile: (file) => file })")
external fun sqlJsConfig(): JsAny

@JsFun("(sql, data) => new sql.Database(data)")
external fun newSqlJsDatabase(sql: SqlJsStatic, data: Uint8Array?): SqlJsDatabase

/** 0 = null, 1 = number, 2 = string, 3 = other */
@JsFun("(v) => v === null || v === undefined ? 0 : (typeof v === 'number' ? 1 : (typeof v === 'string' ? 2 : 3))")
external fun cellKind(value: JsAny?): Int

@JsFun("(b64) => { const bin = atob(b64); const u8 = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u8[i] = bin.charCodeAt(i); return u8; }")
external fun base64ToUint8Array(base64: String): Uint8Array
