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
import com.lab.chessclock.clock.TimeStage
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.ColorPalette
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.settings.TimePreset
import com.lab.chessclock.ui.theme.ChessClockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCurrentOrientationAndTimerValues() {
        setScreen(ClockSettings(time = 2.hours + 45.minutes + 30.seconds, incrementSeconds = 7))

        composeRule.onNodeWithText("Facing each other at the ends").assertIsSelected()
        composeRule.onNodeWithText("Upright").assertIsNotSelected()
        listOf("2 h 45 min 30 s", "7").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun slidersAreLabelledWithTheirValue() {
        setScreen(ClockSettings(time = 1.hours + 30.minutes, incrementSeconds = 7))

        composeRule.onNode(hasContentDescription("Time") and hasStateDescription("1 hour 30 minutes")).assertExists()
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
        setScreen(ClockSettings(time = 3.minutes, incrementSeconds = 2))

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
    fun differentTimesShowASliderForEachHalf() {
        val separate = mutableListOf<Boolean>()
        setScreen(ClockSettings(), onSeparateTimesChange = separate::add)
        composeRule.onNodeWithText("Top player").assertDoesNotExist()

        composeRule.onNodeWithText("Different times").performScrollTo().assertIsOff().performClick()
        assertEquals(listOf(true), separate)
    }

    @Test
    fun eachPlayersTimeIsNamedByTheirHalf() {
        setScreen(ClockSettings(time = 5.minutes, secondTime = 3.minutes))

        composeRule.onNode(hasContentDescription("Top player") and hasStateDescription("5 minutes")).assertExists()
        composeRule.onNode(hasContentDescription("Bottom player") and hasStateDescription("3 minutes")).assertExists()
        composeRule.onNodeWithText("Different times").performScrollTo().assertIsOn()
        composeRule.onNodeWithText("3 + 2").assertIsNotSelected()
    }

    @Test
    fun periodsShowTheirMovesAndTimeAndCanBeRemoved() {
        val removed = mutableListOf<Int>()
        setScreen(
            ClockSettings(time = 90.minutes, stages = listOf(TimeStage(40, 30.minutes))),
            onStageRemove = removed::add,
        )

        composeRule.onNodeWithText("Period 2").performScrollTo().assertExists()
        composeRule.onNode(hasContentDescription("Starts after") and hasStateDescription("40 moves")).assertExists()
        composeRule.onNode(hasContentDescription("Time added") and hasStateDescription("30 minutes")).assertExists()
        composeRule.onNodeWithContentDescription("Remove period 2").performClick()

        assertEquals(listOf(0), removed)
    }

    @Test
    fun aPeriodCanBeAddedUpToTheMost() {
        var added = 0
        setScreen(ClockSettings(), onStageAdd = { added++ })

        composeRule.onNodeWithText("Add time after a number of moves").performScrollTo().performClick()
        assertEquals(1, added)
    }

    @Test
    fun noMorePeriodsThanTheMostCanBeAdded() {
        setScreen(ClockSettings().withStageAdded().withStageAdded())

        composeRule.onNodeWithText("Period 3").performScrollTo().assertExists()
        composeRule.onNodeWithText("Add time after a number of moves").assertDoesNotExist()
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
        onSeparateTimesChange: (Boolean) -> Unit = {},
        onStageAdd: () -> Unit = {},
        onStageRemove: (Int) -> Unit = {},
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
                    onTimeChange = {},
                    onSecondTimeChange = {},
                    onSeparateTimesChange = onSeparateTimesChange,
                    onIncrementChange = {},
                    onStageAdd = onStageAdd,
                    onStageChange = { _, _ -> },
                    onStageRemove = onStageRemove,
                    onIncrementModeChange = onIncrementModeChange,
                    onVibrateOnTurnEndChange = onVibrateOnTurnEndChange,
                    onVibrateOnLowTimeChange = onVibrateOnLowTimeChange,
                    onPresetSelect = onPresetSelect,
                )
            }
        }
    }
}
