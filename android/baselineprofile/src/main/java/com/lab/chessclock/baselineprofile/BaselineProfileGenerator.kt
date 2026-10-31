package com.lab.chessclock.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code a game uses, from launch through play to settings, so it ships precompiled.
 * Run with `./gradlew :app:generateBaselineProfile` on a device or API 33+ emulator.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        playAFewMoves()
        openSettingsAndBack()
    }
}
