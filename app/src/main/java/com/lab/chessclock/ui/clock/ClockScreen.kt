package com.lab.chessclock.ui.clock

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lab.chessclock.R
import com.lab.chessclock.clock.ChessClock
import com.lab.chessclock.clock.ClockStatus
import com.lab.chessclock.clock.Player
import com.lab.chessclock.clock.TimeControl
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.ui.theme.ChessClockTheme
import com.lab.chessclock.ui.theme.ClockColors
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ClockRoute(
    onOpenSettings: () -> Unit,
    viewModel: ClockViewModel = viewModel(factory = ClockViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    Box(modifier.fillMaxSize()) {
        if (uiState is ClockUiState.Ready) {
            val (clock, orientation) = uiState
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayerClockFace(clock, Player.First, orientation, onPlayerPressed, Modifier.weight(1f))
                ControlButton(
                    status = clock.status,
                    onClick = onControlPressed,
                    modifier = Modifier.rotate(orientation.controlRotation),
                )
                PlayerClockFace(clock, Player.Second, orientation, onPlayerPressed, Modifier.weight(1f))
            }
        }

        SmallFloatingActionButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(16.dp),
            containerColor = ClockColors.Inactive,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(1.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_settings_24),
                contentDescription = stringResource(R.string.open_settings),
            )
        }
    }
}

@Composable
private fun PlayerClockFace(
    clock: ChessClock,
    player: Player,
    orientation: Orientation,
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
    val color = when {
        finished && time.isExpired -> ClockColors.Loss
        finished -> ClockColors.Win
        clock.active == player -> MaterialTheme.colorScheme.primary
        else -> ClockColors.Inactive
    }

    // The whole half of the screen is the player's button, like a physical clock.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = stringResource(R.string.end_turn),
                onClick = { onPress(player) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                .aspectRatio(1f)
                .rotate(orientation.rotationOf(player)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 15.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
            )
            Text(
                text = formatClockTime(time.remaining),
                color = ClockColors.Text,
                maxLines = 1,
                softWrap = false,
                // Fixed-width digits so the text doesn't jitter as it counts down.
                style = TextStyle(fontSize = 60.sp, fontFeatureSettings = "tnum"),
            )
        }
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
@Composable
private fun ClockScreenPreview() {
    val clock = ChessClock(TimeControl(5.minutes, 2.seconds)).press(Player.First).tick(42.seconds)
    ChessClockTheme {
        ClockScreen(
            uiState = ClockUiState.Ready(clock, Orientation.HorizontalOpposite),
            onPlayerPressed = {},
            onControlPressed = {},
            onOpenSettings = {},
        )
    }
}
