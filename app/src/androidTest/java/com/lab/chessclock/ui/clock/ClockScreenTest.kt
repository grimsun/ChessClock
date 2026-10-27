package com.lab.chessclock.ui.clock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lab.chessclock.clock.ChessClock
import com.lab.chessclock.clock.Player
import com.lab.chessclock.clock.TimeControl
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.ui.theme.ChessClockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ClockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val newGame = ChessClock(TimeControl(5.minutes, 0.seconds))

    @Test
    fun showsBothPlayersTimeAndStart() {
        setScreen(ClockUiState.Ready(newGame, Orientation.HorizontalSame))

        composeRule.onAllNodesWithText("05:00").assertCountEquals(2)
        composeRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun tappingAFaceReportsThatPlayer() {
        val clock = newGame.press(Player.First).tick(30.seconds)
        val pressed = mutableListOf<Player>()
        setScreen(ClockUiState.Ready(clock, Orientation.VerticalOpposite), onPlayerPressed = pressed::add)

        composeRule.onNodeWithText("05:00").performClick()
        composeRule.onNodeWithText("04:30").performClick()

        assertEquals(listOf(Player.First, Player.Second), pressed)
    }

    @Test
    fun controlButtonLabelFollowsTheGame() {
        var state by mutableStateOf<ClockUiState>(ClockUiState.Ready(newGame, Orientation.HorizontalSame))
        var controlPresses = 0
        setScreen({ state }, onControlPressed = { controlPresses++ })

        composeRule.onNodeWithText("Start").performClick()
        state = ClockUiState.Ready(newGame.toggle(), Orientation.HorizontalSame)
        composeRule.onNodeWithText("Pause").assertExists()
        state = ClockUiState.Ready(newGame.toggle().tick(10.minutes), Orientation.HorizontalSame)
        composeRule.onNodeWithText("Reset").assertExists()

        assertEquals(1, controlPresses)
    }

    @Test
    fun finishedGameShowsTheLoserAtZero() {
        setScreen(ClockUiState.Ready(newGame.toggle().tick(10.minutes), Orientation.HorizontalSame))

        composeRule.onNodeWithText("00:00.00").assertExists()
        composeRule.onNodeWithText("05:00").assertExists()
    }

    @Test
    fun pausedGameStillMarksThePlayerOnMove() {
        setScreen(ClockUiState.Ready(newGame.press(Player.First).tick(30.seconds).pause(), Orientation.HorizontalSame))

        composeRule.onNode(hasText("04:30") and hasStateDescription("On move")).assertExists()
        composeRule.onNode(hasText("05:00") and hasStateDescription("On move")).assertDoesNotExist()
    }

    @Test
    fun finishedGameMarksWhoRanOutOfTime() {
        setScreen(ClockUiState.Ready(newGame.toggle().tick(10.minutes), Orientation.HorizontalSame))

        composeRule.onNode(hasText("00:00.00") and hasStateDescription("Out of time")).assertExists()
        composeRule.onNode(hasText("05:00") and hasStateDescription("On move")).assertDoesNotExist()
    }

    @Test
    fun panelsWorkLikeRingsInEveryOrientation() {
        val clock = newGame.press(Player.First).tick(30.seconds)
        var orientation by mutableStateOf(Orientation.HorizontalSame)
        val pressed = mutableListOf<Player>()
        setScreen({ ClockUiState.Ready(clock, orientation, FaceStyle.Panels) }, onPlayerPressed = pressed::add)

        Orientation.entries.forEach {
            orientation = it
            composeRule.onNode(hasText("04:30") and hasStateDescription("On move")).performClick()
            composeRule.onNodeWithText("05:00").performClick()
        }

        assertEquals(List(Orientation.entries.size) { listOf(Player.Second, Player.First) }.flatten(), pressed)
    }

    @Test
    fun settingsAreHiddenOnlyWhileRunning() {
        var state by mutableStateOf<ClockUiState>(ClockUiState.Ready(newGame, Orientation.HorizontalSame))
        setScreen({ state })
        val settings = composeRule.onNodeWithContentDescription("Settings")

        settings.assertExists()
        state = ClockUiState.Ready(newGame.toggle(), Orientation.HorizontalSame)
        settings.assertDoesNotExist()
        state = ClockUiState.Ready(newGame.toggle().pause(), Orientation.HorizontalSame)
        settings.assertExists()
    }

    @Test
    fun loadingShowsOnlySettings() {
        var opened = false
        setScreen(ClockUiState.Loading, onOpenSettings = { opened = true })

        composeRule.onNodeWithText("Start").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertTrue(opened)
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun setScreen(
        uiState: ClockUiState,
        onPlayerPressed: (Player) -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) = setScreen({ uiState }, onPlayerPressed, onOpenSettings = onOpenSettings)

    private fun setScreen(
        uiState: () -> ClockUiState,
        onPlayerPressed: (Player) -> Unit = {},
        onControlPressed: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChessClockTheme {
                ClockScreen(uiState(), onPlayerPressed, onControlPressed, onOpenSettings)
            }
        }
    }
}
