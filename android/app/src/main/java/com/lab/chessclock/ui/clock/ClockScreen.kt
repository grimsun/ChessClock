package com.lab.chessclock.ui.clock

import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lab.chessclock.R
import com.lab.chessclock.clock.ChessClock
import com.lab.chessclock.clock.ClockStatus
import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.Player
import com.lab.chessclock.clock.PlayerTime
import com.lab.chessclock.clock.TimeControl
import com.lab.chessclock.settings.ColorPalette
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.ui.theme.ChessClockTheme
import com.lab.chessclock.ui.theme.clockColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ClockRoute(
    onOpenSettings: () -> Unit,
    viewModel: ClockViewModel = viewModel(factory = ClockViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Startup is done once the clock shows, not at the first frame, which is still loading.
    ReportDrawnWhen { uiState is ClockUiState.Ready }

    // Stop the clock when the app goes to the background, but not when the screen just rotates:
    // the ViewModel and its ticker survive that.
    val activity = LocalActivity.current
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (activity?.isChangingConfigurations != true) viewModel.pause()
    }

    ClockScreen(
        uiState = uiState,
        onPlayerPressed = viewModel::onPlayerPressed,
        onControlPressed = viewModel::onControlPressed,
        onOpenSettings = {
            viewModel.pause()
            onOpenSettings()
        },
    )
}

@Composable
fun ClockScreen(
    uiState: ClockUiState,
    /** A player touched their face, and how long ago the screen sensed the touch. */
    onPlayerPressed: (Player, Duration) -> Unit,
    onControlPressed: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (uiState is ClockUiState.Ready) {
            val (clock, orientation, faceStyle) = uiState
            // A player thinking longer than the screen timeout would otherwise see the screen turn
            // off, which pauses the app and with it the clock.
            if (clock.status == ClockStatus.Running) KeepScreenOn()

            // Faces are drawn clear of the corner buttons, the one opposite them by as much so both
            // stay the same size; each whole half still takes touches.
            val face = @Composable { player: Player, inset: PaddingValues, modifier: Modifier ->
                PlayerClockFace(clock.faceOf(player), player, orientation, faceStyle, onPlayerPressed, modifier, inset)
            }
            val bottomInset = PaddingValues(bottom = CORNER_BUTTON_STRIP)
            val topInset = PaddingValues(top = CORNER_BUTTON_STRIP)
            val control = @Composable {
                ControlButton(
                    status = clock.status,
                    onClick = onControlPressed,
                    modifier = Modifier.rotate(orientation.controlRotation),
                )
            }

            // Faces stacked on a tall screen, side by side on a wide one, so each gets half the room.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                if (maxWidth > maxHeight) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        face(Player.First, bottomInset, Modifier.weight(1f))
                        control()
                        face(Player.Second, bottomInset, Modifier.weight(1f))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        face(Player.First, topInset, Modifier.weight(1f))
                        control()
                        face(Player.Second, bottomInset, Modifier.weight(1f))
                    }
                }
            }
        }

        // Both stay in the corners at all times, clear of the faces drawn beside them.
        val cornerModifier = Modifier.safeDrawingPadding().padding(16.dp)
        if (uiState is ClockUiState.Ready) {
            MoveCounter(uiState.clock.moves, cornerModifier.align(Alignment.BottomStart))
        }
        SettingsButton(onClick = onOpenSettings, modifier = cornerModifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun PlayerClockFace(
    face: FaceState,
    player: Player,
    orientation: Orientation,
    faceStyle: FaceStyle,
    onPress: (Player, Duration) -> Unit,
    modifier: Modifier = Modifier,
    inset: PaddingValues = PaddingValues(),
) {
    val time = face.time
    val finished = face.role == FaceRole.Won || face.role == FaceRole.Lost
    val progress by animateFloatAsState(
        targetValue = if (finished) 1f else time.progress,
        // Smooth between ticks while running; otherwise a reset or a new time control sweeps the
        // ring back to full instead of jumping.
        animationSpec = if (face.running) {
            tween(ClockViewModel.TICK_INTERVAL.inWholeMilliseconds.toInt(), easing = LinearEasing)
        } else {
            tween(500)
        },
        label = "progress",
    )
    val colors = MaterialTheme.clockColors
    // The face color, and the color of a panel's content drawn on it.
    val (targetColor, targetOnColor) = when (face.role) {
        FaceRole.Lost, FaceRole.LowOnTime -> colors.loss to colors.onLoss
        FaceRole.Won -> colors.win to colors.onWin
        FaceRole.OnMove -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        FaceRole.Waiting -> colors.inactive to colors.onInactive
    }
    // The turn passes from one face to the other as a short cross-fade instead of a jump.
    val color by animateColorAsState(targetColor, tween(COLOR_FADE_MILLIS), label = "color")
    val onColor by animateColorAsState(targetOnColor, tween(COLOR_FADE_MILLIS), label = "onColor")
    // The face gives a little under the finger while it is down.
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    val state = when (face.role) {
        FaceRole.Lost -> stringResource(R.string.state_out_of_time)
        FaceRole.OnMove, FaceRole.LowOnTime -> stringResource(R.string.state_on_move)
        FaceRole.Waiting, FaceRole.Won -> null
    }

    // The whole half of the screen is the player's button, like a physical clock, and like one it
    // acts the moment it is touched rather than when the finger lifts: the opponent's clock starts,
    // and the player stops being charged, a tap's length sooner. Every finger that lands counts,
    // even while another still rests on the face.
    val endTurnLabel = stringResource(R.string.end_turn)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player, onPress) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }?.let { down ->
                            onPress(player, touchAge(down.uptimeMillis))
                        }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            // TalkBack double-taps instead of touching, so it gets the same action as a click, and
            // reads the face as one button with its time and state.
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = endTurnLabel) {
                    onPress(player, Duration.ZERO)
                    true
                }
                if (state != null) stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        val text = formatClockTime(time.remaining)
        // With a simple delay, the seconds the player on move can still think before their time runs.
        val delay = face.delaySeconds?.let { stringResource(R.string.delay_remaining, it) }
        val rotation = orientation.rotationOf(player)
        Box(
            modifier = Modifier
                .padding(inset)
                .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center,
        ) {
            when (faceStyle) {
                FaceStyle.Rings -> RingFace(text, delay, { progress }, color, rotation)
                FaceStyle.Panels -> PanelFace(text, delay, { progress }, color, onColor, rotation)
            }
        }
    }
}

/**
 * What one face shows. From tick to tick it stays equal for the face that waits, so only the face
 * whose time runs is drawn again.
 */
private data class FaceState(
    val time: PlayerTime,
    val role: FaceRole,
    /** Whether the clock runs, so the ring follows each tick smoothly. */
    val running: Boolean,
    /** With a simple delay, the whole seconds the player on move can still think; null otherwise. */
    val delaySeconds: Long?,
)

private enum class FaceRole { Waiting, OnMove, LowOnTime, Won, Lost }

private fun ChessClock.faceOf(player: Player): FaceState {
    val time = timeOf(player)
    val role = when {
        status == ClockStatus.Finished -> if (time.isExpired) FaceRole.Lost else FaceRole.Won
        isLowOnTime(player) -> FaceRole.LowOnTime
        // While paused too, so it's clear whose move it is when play resumes.
        turn == player -> FaceRole.OnMove
        else -> FaceRole.Waiting
    }
    val delay = delayRemaining.takeIf { turn == player && it > Duration.ZERO }
    return FaceState(time, role, status == ClockStatus.Running, delay?.inWholeSecondsRoundedUp)
}

/**
 * How long ago a touch at [uptimeMillis] (the [SystemClock.uptimeMillis] the screen sensed it at)
 * happened: the time the app took to receive and handle it. Zero for a time that can't be a real
 * touch's, such as one injected by a test with its own clock.
 */
private fun touchAge(uptimeMillis: Long): Duration =
    (SystemClock.uptimeMillis() - uptimeMillis).milliseconds.takeIf { it in Duration.ZERO..MAX_TOUCH_AGE }
        ?: Duration.ZERO

@Composable
private fun RingFace(text: String, caption: String?, progress: () -> Float, color: Color, rotation: Float) {
    FaceSquare(rotation) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = RING_STROKE,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
        FaceContent(text, caption, MaterialTheme.clockColors.text)
    }
}

@Composable
private fun PanelFace(
    text: String,
    caption: String?,
    progress: () -> Float,
    color: Color,
    onPanel: Color,
    rotation: Float,
) {
    FaceSquare(rotation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(PANEL_CORNER))
                .background(color),
        )
        FaceContent(text, caption, onPanel)
        // The remaining share of the player's time, as a thin bar along the panel's bottom edge.
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = PANEL_CORNER, vertical = 24.dp),
            color = onPanel,
            trackColor = onPanel.copy(alpha = 0.25f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/**
 * The square both face styles draw in, turned to the player. Rings and panels share its size and
 * [FaceContent], so they differ only in shape: an outline that empties or a filled block.
 */
@Composable
private fun FaceSquare(rotation: Float, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .sizeIn(maxWidth = FACE_SIZE, maxHeight = FACE_SIZE)
            .aspectRatio(1f)
            .rotate(rotation),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** The time, and below it any delay countdown, laid out the same on every face. */
@Composable
private fun BoxScope.FaceContent(text: String, caption: String?, color: Color) {
    ClockText(
        text = text,
        color = color,
        maxFontSize = 96.sp,
        fontWeight = FontWeight.Medium,
        // Clear of the ring's stroke even at the corners of the text, shrinking the text when the
        // face is small or the system font is large instead of letting it overflow.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RING_STROKE + 24.dp),
    )
    if (caption != null) {
        Text(
            text = caption,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = RING_STROKE + 40.dp),
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ClockText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
            // Fixed-width digits so the text doesn't jitter as it counts down.
            fontFeatureSettings = "tnum",
        ),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxFontSize),
    )
}

/**
 * The moves played, opposite the settings button. Only a label: a touch on it does nothing,
 * rather than reaching the face below and ending the turn.
 */
@Composable
private fun MoveCounter(moves: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.moves_played, moves, moves)
    Box(
        modifier = modifier
            .size(CORNER_BUTTON_SIZE)
            .shadow(1.dp, CircleShape)
            .background(MaterialTheme.clockColors.inactive, CircleShape)
            // Takes the touches it covers without acting on them, as a non-clickable Surface does.
            .pointerInput(Unit) {}
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // A long game's three digits shrink to fit rather than crowding the circle's edge.
        BasicText(
            text = moves.toString(),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.clockColors.onInactive,
                textAlign = TextAlign.Center,
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        )
    }
}

/** The settings button, the size of a small FAB, matching the move counter opposite. */
@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(CORNER_BUTTON_SIZE),
        shape = CircleShape,
        color = MaterialTheme.clockColors.inactive,
        contentColor = MaterialTheme.clockColors.onInactive,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_settings_24),
                contentDescription = stringResource(R.string.open_settings),
            )
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun ControlButton(status: ClockStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = when (status) {
        ClockStatus.NotStarted -> R.string.button_start
        ClockStatus.Running -> R.string.button_pause
        ClockStatus.Paused, ClockStatus.Finished -> R.string.button_reset
    }
    Button(
        onClick = onClick,
        // Material's medium FAB size: clear to hit before a game, small enough during one that a
        // hard tap near the middle ends the turn rather than pausing.
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(COLOR_FADE_MILLIS)) togetherWith fadeOut(tween(COLOR_FADE_MILLIS)) },
            label = "controlLabel",
        ) {
            Text(stringResource(it), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val RING_STROKE = 16.dp

// Short, so the turn is seen to pass at once.
private const val COLOR_FADE_MILLIS = 100

/** Longer than the main thread is ever busy, short enough to reject a timestamp from another clock. */
private val MAX_TOUCH_AGE = 1.seconds

/** Large enough for big digits, small enough that a ring stays a ring on a tablet. */
private val FACE_SIZE = 340.dp

/** Material's extra-large shape. */
private val PANEL_CORNER = 28.dp

/** What a face leaves for the corner buttons, beyond its own padding. */
private val CORNER_BUTTON_STRIP = 40.dp

/** Material's small FAB. */
private val CORNER_BUTTON_SIZE = 40.dp

/** Below this the player on move sees their face turn red. */
internal val LOW_TIME = 10.seconds

/** Whether [player] is on move in an unfinished game with less than [LOW_TIME] left. */
internal fun ChessClock.isLowOnTime(player: Player): Boolean =
    status != ClockStatus.Finished && turn == player && timeOf(player).remaining < LOW_TIME

/** Whole seconds, counting a started second as a whole one, as a countdown shows them. */
private val Duration.inWholeSecondsRoundedUp: Long
    get() = (this + 999.milliseconds).inWholeSeconds

/** The second player (bottom) always reads the screen the right way up for their side. */
private val Orientation.controlRotation: Float
    get() = when (this) {
        Orientation.HorizontalSame, Orientation.HorizontalOpposite -> 0f
        Orientation.VerticalSame, Orientation.VerticalOpposite -> 90f
    }

/** Facing each other turns the first player's face half a turn from the second's. */
private fun Orientation.rotationOf(player: Player): Float = when (this) {
    Orientation.HorizontalSame, Orientation.VerticalSame -> controlRotation
    Orientation.HorizontalOpposite, Orientation.VerticalOpposite ->
        if (player == Player.First) controlRotation + 180f else controlRotation
}

@Preview(showBackground = true)
@Preview(name = "Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Preview(name = "Small, large font", showBackground = true, widthDp = 320, heightDp = 568, fontScale = 2f)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ClockScreenPreview() {
    ClockScreenPreview(FaceStyle.Rings, Orientation.HorizontalOpposite)
}

@Preview(showBackground = true)
@Preview(name = "Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun PanelsPreview() {
    ClockScreenPreview(FaceStyle.Panels, Orientation.HorizontalOpposite)
}

@Preview(showBackground = true)
@Composable
private fun PanelsVerticalPreview() {
    ClockScreenPreview(FaceStyle.Panels, Orientation.VerticalOpposite)
}

@Preview(showBackground = true)
@Composable
private fun DelayPreview() {
    val clock = ChessClock(TimeControl(5.minutes, 5.seconds, IncrementMode.SimpleDelay))
        .press(Player.First).tick(1.seconds).press(Player.Second).tick(2.seconds)
    ChessClockTheme {
        Row {
            FaceStyle.entries.forEach { faceStyle ->
                ClockScreen(
                    uiState = ClockUiState.Ready(clock, Orientation.HorizontalOpposite, faceStyle),
                    onPlayerPressed = { _, _ -> },
                    onControlPressed = {},
                    onOpenSettings = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "Palettes", widthDp = 1200, heightDp = 600)
@Preview(
    name = "Palettes, dark",
    widthDp = 1200,
    heightDp = 600,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun PalettesPreview() {
    val clock = ChessClock(TimeControl(5.minutes, 2.seconds)).press(Player.First).tick(42.seconds)
    Row {
        ColorPalette.entries.forEach { palette ->
            ChessClockTheme(palette = palette) {
                ClockScreen(
                    uiState = ClockUiState.Ready(clock, Orientation.HorizontalOpposite, FaceStyle.Panels),
                    onPlayerPressed = { _, _ -> },
                    onControlPressed = {},
                    onOpenSettings = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ClockScreenPreview(faceStyle: FaceStyle, orientation: Orientation) {
    val clock = ChessClock(TimeControl(5.minutes, 2.seconds)).press(Player.First).tick(42.seconds)
    ChessClockTheme {
        ClockScreen(
            uiState = ClockUiState.Ready(clock, orientation, faceStyle),
            onPlayerPressed = { _, _ -> },
            onControlPressed = {},
            onOpenSettings = {},
        )
    }
}
