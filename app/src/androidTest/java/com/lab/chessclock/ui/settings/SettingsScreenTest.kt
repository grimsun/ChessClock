package com.lab.chessclock.ui.settings

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.TimerSetting
import com.lab.chessclock.ui.theme.ChessClockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCurrentOrientationAndTimerValues() {
        setScreen(ClockSettings(hours = 2, minutes = 45, seconds = 30, incrementSeconds = 7))

        composeRule.onNodeWithText("Horizontal, Same direction").assertIsSelected()
        composeRule.onNodeWithText("Vertical, Opposite directions").assertIsNotSelected()
        listOf("2", "45", "30", "7").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun choosingAnOrientationReportsIt() {
        val chosen = mutableListOf<Orientation>()
        setScreen(ClockSettings(), onOrientationChange = chosen::add)

        composeRule.onNodeWithText("Vertical, Opposite directions").performClick()

        assertEquals(listOf(Orientation.VerticalOpposite), chosen)
    }

    @Test
    fun backButtonNavigatesBack() {
        var back = false
        setScreen(ClockSettings(), onBack = { back = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(back)
    }

    @Test
    fun showsNothingUntilSettingsLoad() {
        setScreen(null)

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("Orientation").assertDoesNotExist()
    }

    private fun setScreen(
        settings: ClockSettings?,
        onBack: () -> Unit = {},
        onOrientationChange: (Orientation) -> Unit = {},
        onTimerChange: (TimerSetting, Int) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            ChessClockTheme {
                SettingsScreen(settings, onBack, onOrientationChange, onTimerChange)
            }
        }
    }
}
