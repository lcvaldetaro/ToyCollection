package com.gepetto.toydb.service

import kotlinx.coroutines.sync.Mutex

/**
 * Only one full collection replacement (restore or web sync) runs at a time, and Back Up does not read the
 * collection while one runs. Not re-entrant: never call one from inside another.
 */
object CollectionWriteLock {
    val mutex = Mutex()
}
