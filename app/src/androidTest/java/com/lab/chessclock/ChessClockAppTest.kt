package com.lab.chessclock

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChessClockAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun controlButtonStartsPausesAndResets() {
        composeRule.onNodeWithText("Start").performClick()
        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Reset").performClick()
        composeRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun settingsOpensAndReturns() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Orientation").assertExists()
        composeRule.onNodeWithText("Increment Seconds").assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun doubleTappingSettingsOpensItOnce() {
        doubleTap(composeRule.onNodeWithContentDescription("Settings"))

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun doubleTappingBackReturnsToTheClock() {
        composeRule.onNodeWithContentDescription("Settings").performClick()

        doubleTap(composeRule.onNodeWithContentDescription("Back"))

        composeRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun rotatingKeepsTheClockRunning() {
        composeRule.onNodeWithText("Start").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Reset").assertExists()
    }

    /** Taps twice without letting the screen transition run in between, like a quick double tap. */
    private fun doubleTap(node: SemanticsNodeInteraction) {
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        node.performClick()
        node.performClick()
        composeRule.mainClock.autoAdvance = true
    }
}
