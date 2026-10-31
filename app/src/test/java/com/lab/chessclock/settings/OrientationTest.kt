package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationTest {

    @Test
    fun storageKeysRoundTrip() {
        Orientation.entries.forEach { assertEquals(it, Orientation.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun storageKeysAreUnique() {
        assertEquals(Orientation.entries.size, Orientation.entries.map { it.storageKey }.toSet().size)
    }

    @Test
    fun missingOrUnknownKeyIsTheDefault() {
        assertEquals(ClockSettings().orientation, Orientation.fromStorageKey(null))
        assertEquals(ClockSettings().orientation, Orientation.fromStorageKey("diagonal"))
    }
}
