package com.lab.chessclock.settings

import com.lab.chessclock.clock.IncrementMode
import org.junit.Assert.assertEquals
import org.junit.Test

class IncrementModeTest {

    @Test
    fun storageKeysRoundTrip() {
        IncrementMode.entries.forEach { assertEquals(it, incrementModeFromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownKeyIsFischer() {
        // Settings saved before the modes existed are Fischer increments.
        assertEquals(IncrementMode.Fischer, incrementModeFromStorageKey(null))
        assertEquals(IncrementMode.Fischer, incrementModeFromStorageKey("hourglass"))
    }
}
