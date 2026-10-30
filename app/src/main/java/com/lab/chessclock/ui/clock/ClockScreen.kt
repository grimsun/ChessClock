package com.lab.chessclock.ui.clock

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
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
    onPlayerPressed: (Player) -> Unit,
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

            val face = @Composable { player: Player, modifier: Modifier ->
                PlayerClockFace(clock, player, orientation, faceStyle, onPlayerPressed, modifier)
            }
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
                        face(Player.First, Modifier.weight(1f))
                        control()
                        face(Player.Second, Modifier.weight(1f))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        face(Player.First, Modifier.weight(1f))
                        control()
                        face(Player.Second, Modifier.weight(1f))
                    }
                }
            }
        }

        // The button sits on the second player's half: hide it during play so a hard tap near the
        // corner ends the turn instead of opening settings and pausing the game.
        val running = uiState is ClockUiState.Ready && uiState.clock.status == ClockStatus.Running
        if (!running) {
            SmallFloatingActionButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.clockColors.inactive,
                contentColor = MaterialTheme.clockColors.onInactive,
                elevation = FloatingActionButtonDefaults.elevation(1.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_settings_24),
                    contentDescription = stringResource(R.string.open_settings),
                )
            }
        }
    }
}

@Composable
private fun PlayerClockFace(
    clock: ChessClock,
    player: Player,
    orientation: Orientation,
    faceStyle: FaceStyle,
    onPress: (Player) -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = clock.timeOf(player)
    val finished = clock.status == ClockStatus.Finished
    val progress by animateFloatAsState(
        targetValue = if (finished) 1f else time.progress,
        animationSpec = if (clock.status == ClockStatus.Running) {
            tween(ClockViewModel.TICK_INTERVAL.inWholeMilliseconds.toInt(), easing = LinearEasing)
        } else {
            snap()
        },
        label = "progress",
    )
    val colors = MaterialTheme.clockColors
    // The face color, and the color of a panel's content drawn on it.
    val (color, onColor) = when {
        finished && time.isExpired -> colors.loss to colors.onLoss
        finished -> colors.win to colors.onWin
        clock.isLowOnTime(player) -> colors.loss to colors.onLoss
        // While paused too, so it's clear whose move it is when play resumes.
        clock.turn == player -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> colors.inactive to colors.onInactive
    }
    val state = when {
        time.isExpired -> stringResource(R.string.state_out_of_time)
        !finished && clock.turn == player -> stringResource(R.string.state_on_move)
        else -> null
    }

    // The whole half of the screen is the player's button, like a physical clock.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = stringResource(R.string.end_turn),
                onClick = { onPress(player) },
            )
            .semantics { if (state != null) stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        val text = formatClockTime(time.remaining)
        // With a simple delay, the seconds the player on move can still think before their time runs.
        val delay = clock.delayRemaining.takeIf { clock.turn == player && it > Duration.ZERO }
            ?.let { stringResource(R.string.delay_remaining, it.inWholeSecondsRoundedUp) }
        val rotation = orientation.rotationOf(player)
        when (faceStyle) {
            FaceStyle.Rings -> RingFace(text, delay, { progress }, color, rotation)
            FaceStyle.Panels -> PanelFace(text, delay, { progress }, color, onColor, rotation)
        }
    }
}

@Composable
private fun RingFace(text: String, caption: String?, progress: () -> Float, color: Color, rotation: Float) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
            .aspectRatio(1f)
            .rotate(rotation),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = RING_STROKE,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
        ClockText(
            text = text,
            color = MaterialTheme.clockColors.text,
            maxFontSize = 60.sp,
            // Inside the ring's stroke, shrinking the text when the face is small or the
            // system font is large instead of letting it overflow the ring.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RING_STROKE + 12.dp),
        )
        if (caption != null) {
            Text(
                text = caption,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = RING_STROKE + 16.dp),
                color = MaterialTheme.clockColors.text,
                style = MaterialTheme.typography.titleMedium,
            )
        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(color),
    ) {
        // Turned with the player's seat, and laid out for the turned box so the digits fill the
        // panel's long side when the players sit at its ends.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotateFitted(rotation)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            ClockText(
                text = text,
                color = onPanel,
                maxFontSize = 112.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (caption != null) {
                Text(
                    text = caption,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = onPanel,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            // The remaining share of the player's time, as a thin bar below the digits.
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = onPanel,
                trackColor = onPanel.copy(alpha = 0.25f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
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
 * Rotates the content like [rotate], but for a quarter turn measures it with width and height
 * swapped, so it fills the box the way it is seen instead of being clipped to the unturned shape.
 */
private fun Modifier.rotateFitted(degrees: Float): Modifier = layout { measurable, constraints ->
    val sideways = degrees % 180f != 0f
    val placeable = measurable.measure(
        if (sideways) {
            Constraints(constraints.minHeight, constraints.maxHeight, constraints.minWidth, constraints.maxWidth)
        } else {
            constraints
        },
    )
    val width = if (sideways) placeable.height else placeable.width
    val height = if (sideways) placeable.width else placeable.height
    layout(width, height) {
        placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
            rotationZ = degrees
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
        modifier = modifier.size(100.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(stringResource(label), fontSize = 18.sp)
    }
}

private val RING_STROKE = 15.dp

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
                    onPlayerPressed = {},
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
                    onPlayerPressed = {},
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
            onPlayerPressed = {},
            onControlPressed = {},
            onOpenSettings = {},
        )
    }
}
