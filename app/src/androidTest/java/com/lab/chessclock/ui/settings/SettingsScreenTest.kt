package com.lab.chessclock.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.settings.TimePreset
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
    fun slidersAreLabelledWithTheirValue() {
        setScreen(ClockSettings(minutes = 45, incrementSeconds = 7))

        composeRule.onNode(hasContentDescription("Minutes") and hasStateDescription("45")).assertExists()
        composeRule.onNode(hasContentDescription("Increment Seconds") and hasStateDescription("7")).assertExists()
    }

    @Test
    fun choosingAnOrientationReportsIt() {
        val chosen = mutableListOf<Orientation>()
        setScreen(ClockSettings(), onOrientationChange = chosen::add)

        composeRule.onNodeWithText("Vertical, Opposite directions").performClick()

        assertEquals(listOf(Orientation.VerticalOpposite), chosen)
    }

    @Test
    fun choosingAFaceStyleReportsIt() {
        val chosen = mutableListOf<FaceStyle>()
        setScreen(ClockSettings(), onFaceStyleChange = chosen::add)

        composeRule.onNodeWithText("Rings").assertIsSelected()
        composeRule.onNodeWithText("Full-color panels").assertIsNotSelected().performClick()

        assertEquals(listOf(FaceStyle.Panels), chosen)
    }

    @Test
    fun choosingAThemeReportsIt() {
        val chosen = mutableListOf<ThemeMode>()
        setScreen(ClockSettings(), onThemeChange = chosen::add)

        composeRule.onNodeWithText("System default").assertIsSelected()
        composeRule.onNodeWithText("Dark").performScrollTo().assertIsNotSelected().performClick()

        assertEquals(listOf(ThemeMode.Dark), chosen)
    }

    @Test
    fun marksThePresetMatchingTheSettings() {
        setScreen(ClockSettings(minutes = 3, incrementSeconds = 2))

        composeRule.onNodeWithText("3 + 2").assertIsSelected()
        composeRule.onNodeWithText("3 + 0").assertIsNotSelected()
    }

    @Test
    fun choosingAPresetReportsIt() {
        val chosen = mutableListOf<TimePreset>()
        setScreen(ClockSettings(), onPresetSelect = chosen::add)

        composeRule.onNodeWithText("10 + 5").performClick()

        assertEquals(listOf(TimePreset(10, 5)), chosen)
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

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun setScreen(
        settings: ClockSettings?,
        onBack: () -> Unit = {},
        onOrientationChange: (Orientation) -> Unit = {},
        onFaceStyleChange: (FaceStyle) -> Unit = {},
        onThemeChange: (ThemeMode) -> Unit = {},
        onTimerChange: (TimerSetting, Int) -> Unit = { _, _ -> },
        onPresetSelect: (TimePreset) -> Unit = {},
    ) {
        composeRule.setContent {
            ChessClockTheme {
                SettingsScreen(
                    settings = settings,
                    onBack = onBack,
                    onOrientationChange = onOrientationChange,
                    onFaceStyleChange = onFaceStyleChange,
                    onThemeChange = onThemeChange,
                    onTimerChange = onTimerChange,
                    onPresetSelect = onPresetSelect,
                )
            }
        }
    }
}
