package com.gepetto.toydb.service

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlSyncServiceHashTest {
    @Test
    fun testCalculateHash() {
        val hashAbc = HtmlSyncService.calculateHash("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hashAbc)

        val hashEmpty = HtmlSyncService.calculateHash("")
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hashEmpty)
    }
}
