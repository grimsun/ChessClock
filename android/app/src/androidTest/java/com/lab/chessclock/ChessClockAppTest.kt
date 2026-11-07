package com.lab.chessclock

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChessClockAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForStoredSettings() {
        // Nothing is drawn until the settings load from disk, which Compose's idling doesn't track.
        composeRule.waitUntil { composeRule.onAllNodesWithContentDescription("Start").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun controlButtonStartsPausesAndResets() {
        composeRule.onNodeWithContentDescription("Start").performClick()
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Reset").performClick()
        composeRule.onNodeWithContentDescription("Start").assertExists()
    }

    @Test
    fun settingsOpensAndReturns() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Where players sit").assertExists()
        composeRule.onNodeWithText("Increment per move (seconds)").assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Start").assertExists()
    }

    @Test
    fun openingSettingsDuringPlayPausesTheClock() {
        composeRule.onNodeWithContentDescription("Start").performClick()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Reset").assertExists()
    }

    @Test
    fun doubleTappingSettingsOpensItOnce() {
        doubleTap(composeRule.onNodeWithContentDescription("Settings"))

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Start").assertExists()
    }

    @Test
    fun doubleTappingBackReturnsToTheClock() {
        composeRule.onNodeWithContentDescription("Settings").performClick()

        doubleTap(composeRule.onNodeWithContentDescription("Back"))

        composeRule.onNodeWithContentDescription("Start").assertExists()
    }

    @Test
    fun screenStaysOnOnlyWhileTheClockRuns() {
        assertFalse(keepsScreenOn())

        composeRule.onNodeWithContentDescription("Start").performClick()
        assertTrue(keepsScreenOn())

        composeRule.onNodeWithContentDescription("Pause").performClick()
        assertFalse(keepsScreenOn())
    }

    @Test
    fun rotatingKeepsTheClockRunning() {
        composeRule.onNodeWithContentDescription("Start").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Reset").assertExists()
    }

    /** Taps twice without letting the screen transition run in between, like a quick double tap. */
    private fun doubleTap(node: SemanticsNodeInteraction) {
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        node.performClick()
        node.performClick()
        composeRule.mainClock.autoAdvance = true
    }

    private fun keepsScreenOn(): Boolean {
        composeRule.waitForIdle()
        return composeRule.activity.window.decorView.anyView { it.keepScreenOn }
    }

    private fun View.anyView(predicate: (View) -> Boolean): Boolean =
        predicate(this) || (this is ViewGroup && children.any { it.anyView(predicate) })
}
