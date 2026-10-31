package com.lab.chessclock.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.ColorPalette
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

        composeRule.onNodeWithText("Facing each other at the ends").assertIsSelected()
        composeRule.onNodeWithText("Upright").assertIsNotSelected()
        listOf("2", "45", "30", "7").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun slidersAreLabelledWithTheirValue() {
        setScreen(ClockSettings(minutes = 45, incrementSeconds = 7))

        composeRule.onNode(hasContentDescription("Minutes") and hasStateDescription("45")).assertExists()
        composeRule.onNode(hasContentDescription("Increment per move (seconds)") and hasStateDescription("7")).assertExists()
    }

    @Test
    fun choosingAnOrientationReportsIt() {
        val chosen = mutableListOf<Orientation>()
        setScreen(ClockSettings(), onOrientationChange = chosen::add)

        composeRule.onNodeWithText("Facing each other at the sides").performScrollTo().performClick()

        assertEquals(listOf(Orientation.VerticalOpposite), chosen)
    }

    @Test
    fun choosingAFaceStyleReportsIt() {
        val chosen = mutableListOf<FaceStyle>()
        setScreen(ClockSettings(), onFaceStyleChange = chosen::add)

        composeRule.onNodeWithText("Rings").assertIsSelected()
        composeRule.onNodeWithText("Panels").performScrollTo().assertIsNotSelected().performClick()

        assertEquals(listOf(FaceStyle.Panels), chosen)
    }

    @Test
    fun choosingAThemeReportsIt() {
        val chosen = mutableListOf<ThemeMode>()
        setScreen(ClockSettings(), onThemeChange = chosen::add)

        composeRule.onNodeWithText("System").assertIsSelected()
        composeRule.onNodeWithText("Dark").performScrollTo().assertIsNotSelected().performClick()

        assertEquals(listOf(ThemeMode.Dark), chosen)
    }

    @Test
    fun choosingAPaletteReportsIt() {
        val chosen = mutableListOf<ColorPalette>()
        setScreen(ClockSettings(), onPaletteChange = chosen::add)

        composeRule.onNodeWithText("Walnut").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Ocean (color-blind friendly)").performScrollTo().assertIsNotSelected().performClick()

        assertEquals(listOf(ColorPalette.Ocean), chosen)
    }

    @Test
    fun choosingAnIncrementModeReportsIt() {
        val chosen = mutableListOf<IncrementMode>()
        setScreen(ClockSettings(), onIncrementModeChange = chosen::add)

        composeRule.onNodeWithText("Increment").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Delay").performScrollTo().assertIsNotSelected().performClick()

        assertEquals(listOf(IncrementMode.SimpleDelay), chosen)
    }

    @Test
    fun delayModeExplainsItselfAndRelabelsTheSlider() {
        setScreen(ClockSettings(incrementSeconds = 5, incrementMode = IncrementMode.SimpleDelay))

        composeRule.onNodeWithText("each move, the clock waits", substring = true).assertExists()
        composeRule.onNode(hasContentDescription("Delay per move (seconds)") and hasStateDescription("5")).assertExists()
        composeRule.onNode(hasContentDescription("3 minutes, 2 seconds delay per move")).assertExists()
    }

    @Test
    fun turningVibrationsOffReportsEachOne() {
        val turnEnd = mutableListOf<Boolean>()
        val lowTime = mutableListOf<Boolean>()
        setScreen(
            ClockSettings(vibrateOnLowTime = false),
            onVibrateOnTurnEndChange = turnEnd::add,
            onVibrateOnLowTimeChange = lowTime::add,
        )

        composeRule.onNodeWithText("When a turn ends").performScrollTo().assertIsOn().performClick()
        composeRule.onNodeWithText("When time runs low").performScrollTo().assertIsOff().performClick()

        assertEquals(listOf(false), turnEnd)
        assertEquals(listOf(true), lowTime)
    }

    @Test
    fun marksThePresetMatchingTheSettings() {
        setScreen(ClockSettings(minutes = 3, incrementSeconds = 2))

        composeRule.onNodeWithText("3 + 2").assertIsSelected()
        composeRule.onNodeWithText("3 + 0").assertIsNotSelected()
    }

    @Test
    fun presetsAreSpelledOutForTalkBack() {
        setScreen(ClockSettings())

        composeRule.onNode(hasContentDescription("1 minute, plus 0 seconds per move")).assertExists()
        composeRule.onNode(hasContentDescription("3 minutes, plus 2 seconds per move")).assertExists()
        composeRule.onNode(hasContentDescription("2 minutes, plus 1 second per move")).assertExists()
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
        composeRule.onNodeWithText("Where players sit").assertDoesNotExist()
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun setScreen(
        settings: ClockSettings?,
        onBack: () -> Unit = {},
        onOrientationChange: (Orientation) -> Unit = {},
        onFaceStyleChange: (FaceStyle) -> Unit = {},
        onThemeChange: (ThemeMode) -> Unit = {},
        onPaletteChange: (ColorPalette) -> Unit = {},
        onTimerChange: (TimerSetting, Int) -> Unit = { _, _ -> },
        onIncrementModeChange: (IncrementMode) -> Unit = {},
        onVibrateOnTurnEndChange: (Boolean) -> Unit = {},
        onVibrateOnLowTimeChange: (Boolean) -> Unit = {},
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
                    onPaletteChange = onPaletteChange,
                    onTimerChange = onTimerChange,
                    onIncrementModeChange = onIncrementModeChange,
                    onVibrateOnTurnEndChange = onVibrateOnTurnEndChange,
                    onVibrateOnLowTimeChange = onVibrateOnLowTimeChange,
                    onPresetSelect = onPresetSelect,
                )
            }
        }
    }
}
