package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun storageKeysRoundTrip() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownKeyFollowsTheSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStorageKey("sepia"))
    }
}
