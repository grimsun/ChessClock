package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPaletteTest {

    @Test
    fun storageKeysRoundTrip() {
        ColorPalette.entries.forEach { assertEquals(it, ColorPalette.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownKeyIsWalnut() {
        assertEquals(ColorPalette.Walnut, ColorPalette.fromStorageKey(null))
        assertEquals(ColorPalette.Walnut, ColorPalette.fromStorageKey("neon"))
    }
}
