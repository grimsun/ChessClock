package com.lab.chessclock.testing

import com.lab.chessclock.haptics.Haptics

/** Records the vibrations played, by name, in order. */
class FakeHaptics : Haptics {
    val played = mutableListOf<String>()

    override fun turnEnded() {
        played += "turnEnded"
    }
}
