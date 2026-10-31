package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceStyleTest {

    @Test
    fun storageKeysRoundTrip() {
        FaceStyle.entries.forEach { assertEquals(it, FaceStyle.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownKeyIsRings() {
        assertEquals(FaceStyle.Rings, FaceStyle.fromStorageKey(null))
        assertEquals(FaceStyle.Rings, FaceStyle.fromStorageKey("hexagons"))
    }
}
