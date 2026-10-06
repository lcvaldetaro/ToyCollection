@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.database

import kotlin.js.JsAny
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

// Rev 6: each call closes its connection when the transaction ends.
@JsFun("""(key) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const db = open.result;
        const req = db.transaction('kv', 'readonly').objectStore('kv').get(key);
        req.onsuccess = () => { db.close(); resolve(req.result === undefined ? null : req.result); };
        req.onerror = () => { db.close(); reject(req.error); };
    };
})""")
external fun idbGet(key: String): Promise<Uint8Array?>

@JsFun("""(key, value) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const db = open.result;
        const tx = db.transaction('kv', 'readwrite');
        tx.objectStore('kv').put(value, key);
        tx.oncomplete = () => { db.close(); resolve(null); };
        tx.onerror = () => { db.close(); reject(tx.error); };
        tx.onabort = () => { db.close(); reject(tx.error); };
    };
})""")
external fun idbPut(key: String, value: Uint8Array): Promise<JsAny?>

@JsFun("(callback) => { document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'hidden') callback(); }); window.addEventListener('pagehide', () => callback()); }")
external fun onPageHidden(callback: () -> Unit)
