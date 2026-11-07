package com.lab.chessclock.ui.settings

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lab.chessclock.R
import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.TimeStage
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.ColorPalette
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.settings.TimePreset
import com.lab.chessclock.ui.theme.ChessClockTheme
import com.lab.chessclock.ui.theme.paletteSwatch
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        onBack = onBack,
        onOrientationChange = viewModel::setOrientation,
        onFaceStyleChange = viewModel::setFaceStyle,
        onThemeChange = viewModel::setTheme,
        onPaletteChange = viewModel::setPalette,
        onTimeChange = viewModel::setTime,
        onSecondTimeChange = viewModel::setSecondTime,
        onSeparateTimesChange = viewModel::setSeparateTimes,
        onIncrementChange = viewModel::setIncrement,
        onStageAdd = viewModel::addStage,
        onStageChange = viewModel::setStage,
        onStageRemove = viewModel::removeStage,
        onIncrementModeChange = viewModel::setIncrementMode,
        onVibrateOnTurnEndChange = viewModel::setVibrateOnTurnEnd,
        onVibrateOnLowTimeChange = viewModel::setVibrateOnLowTime,
        onPresetSelect = viewModel::applyPreset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ClockSettings?,
    onBack: () -> Unit,
    onOrientationChange: (Orientation) -> Unit,
    onFaceStyleChange: (FaceStyle) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onTimeChange: (Duration) -> Unit,
    onSecondTimeChange: (Duration) -> Unit,
    onSeparateTimesChange: (Boolean) -> Unit,
    onIncrementChange: (Int) -> Unit,
    onStageAdd: () -> Unit,
    onStageChange: (Int, TimeStage) -> Unit,
    onStageRemove: (Int) -> Unit,
    onIncrementModeChange: (IncrementMode) -> Unit,
    onVibrateOnTurnEndChange: (Boolean) -> Unit,
    onVibrateOnLowTimeChange: (Boolean) -> Unit,
    onPresetSelect: (TimePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24),
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        if (settings == null) return@Scaffold

        // Scrolls across the whole width, but keeps the settings in a readable column on tablets
        // and in landscape instead of stretching rows and sliders edge to edge.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 600.dp),
        ) {
            // One tap for the usual time controls, with the sliders right below for anything else.
            SectionHeader(R.string.presets_title)
            SupportingText(R.string.presets_explanation)
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimePreset.All.forEach { preset ->
                    PresetChip(
                        preset = preset,
                        mode = settings.incrementMode,
                        selected = preset.matches(settings),
                        onClick = { onPresetSelect(preset) },
                    )
                }
            }
            val secondTime = settings.secondTime
            // Named by where each half of the screen is: the first player's is on top, or on the left
            // when the screen is wide.
            val wide = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            TimeSlider(
                label = when {
                    secondTime == null -> R.string.setting_time
                    wide -> R.string.setting_time_left
                    else -> R.string.setting_time_top
                },
                value = settings.time,
                onValueChange = onTimeChange,
            )
            if (secondTime != null) {
                TimeSlider(
                    label = if (wide) R.string.setting_time_right else R.string.setting_time_bottom,
                    value = secondTime,
                    onValueChange = onSecondTimeChange,
                )
            }
            SwitchOption(
                label = R.string.separate_times,
                description = R.string.separate_times_description,
                checked = secondTime != null,
                onCheckedChange = onSeparateTimesChange,
            )

            settings.stages.forEachIndexed { index, stage ->
                StageSetting(
                    number = index + 2,
                    stage = stage,
                    onChange = { onStageChange(index, it) },
                    onRemove = { onStageRemove(index) },
                )
            }
            if (settings.stages.size < ClockSettings.MAX_STAGES) {
                TextButton(onClick = onStageAdd, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(painterResource(R.drawable.ic_add_24), contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(R.string.stage_add), Modifier.padding(start = 8.dp))
                }
            }
            if (settings.stages.isNotEmpty()) SupportingText(R.string.stages_explanation)

            // The mode sits right above the slider whose seconds it gives a meaning to.
            SegmentedSetting(
                title = R.string.increment_mode_title,
                options = IncrementMode.entries,
                selected = settings.incrementMode,
                label = { it.label },
                onSelect = onIncrementModeChange,
            )
            SupportingText(settings.incrementMode.description)
            StepSlider(
                label = settings.incrementMode.sliderLabel,
                steps = INCREMENT_STEPS,
                value = settings.incrementSeconds,
                valueText = { it.toString() },
                onValueChange = onIncrementChange,
            )
            SupportingText(R.string.time_change_restarts)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.orientation_title)
            Column(Modifier.selectableGroup()) {
                Orientation.entries.forEach { orientation ->
                    RadioOption(
                        label = orientation.label,
                        description = orientation.description,
                        selected = orientation == settings.orientation,
                        onSelect = { onOrientationChange(orientation) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.vibration_title)
            SwitchOption(
                label = R.string.vibrate_on_turn_end,
                description = R.string.vibrate_on_turn_end_description,
                checked = settings.vibrateOnTurnEnd,
                onCheckedChange = onVibrateOnTurnEndChange,
            )
            SwitchOption(
                label = R.string.vibrate_on_low_time,
                description = R.string.vibrate_on_low_time_description,
                checked = settings.vibrateOnLowTime,
                onCheckedChange = onVibrateOnLowTimeChange,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.appearance_title)
            // Two or three short choices: segmented buttons show them all at a glance.
            SegmentedSetting(
                title = R.string.face_style_title,
                options = FaceStyle.entries,
                selected = settings.faceStyle,
                label = { it.label },
                onSelect = onFaceStyleChange,
            )
            SegmentedSetting(
                title = R.string.theme_title,
                options = ThemeMode.entries,
                selected = settings.theme,
                label = { it.label },
                onSelect = onThemeChange,
            )
            SettingTitle(R.string.palette_title)
            Column(Modifier.selectableGroup()) {
                ColorPalette.entries.forEach { palette ->
                    RadioOption(
                        label = palette.label,
                        selected = palette == settings.palette,
                        onSelect = { onPaletteChange(palette) },
                    ) {
                        PaletteSwatch(palette)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SettingTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun <T> SegmentedSetting(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    SettingTitle(title)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(stringResource(label(option))) },
            )
        }
    }
}

@Composable
private fun SupportingText(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PresetChip(preset: TimePreset, mode: IncrementMode, selected: Boolean, onClick: () -> Unit) {
    val description = presetDescription(preset, mode)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.preset_label, preset.minutes, preset.incrementSeconds)) },
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun RadioOption(
    @StringRes label: Int,
    selected: Boolean,
    @StringRes description: Int? = null,
    onSelect: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

/** A whole row that toggles, with the switch at its end. */
@Composable
private fun SwitchOption(
    @StringRes label: Int,
    @StringRes description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** The palette's on-move color beside its waiting color, in the theme's current light or dark. */
@Composable
private fun PaletteSwatch(palette: ColorPalette) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (active, inactive) = paletteSwatch(palette, dark)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(32.dp)) {
        drawArc(active, startAngle = 90f, sweepAngle = 180f, useCenter = true)
        drawArc(inactive, startAngle = -90f, sweepAngle = 180f, useCenter = true)
        drawCircle(outline, style = Stroke(1.dp.toPx()))
    }
}

/** A later period: after how many moves it starts, and the time it adds. */
@Composable
private fun StageSetting(number: Int, stage: TimeStage, onChange: (TimeStage) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.stage_title, number),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24),
                contentDescription = stringResource(R.string.stage_remove, number),
            )
        }
    }
    StepSlider(
        label = R.string.stage_moves,
        steps = ClockSettings.MOVE_STEPS,
        value = stage.moves,
        valueText = { pluralStringResource(R.plurals.move_count, it, it) },
        onValueChange = { onChange(stage.copy(moves = it)) },
    )
    TimeSlider(R.string.stage_time, stage.time) { onChange(stage.copy(time = it)) }
}

@Composable
private fun TimeSlider(@StringRes label: Int, value: Duration, onValueChange: (Duration) -> Unit) {
    StepSlider(
        label = label,
        steps = ClockSettings.TIME_STEPS,
        value = value,
        valueText = { compactDuration(it) },
        spokenValue = { spokenDuration(it) },
        onValueChange = onValueChange,
    )
}

/**
 * A slider over a list of [steps], showing the value at its end. A [value] between steps, such as
 * one saved by an older version, shows as it is until the slider moves.
 */
@Composable
private fun <T : Comparable<T>> StepSlider(
    @StringRes label: Int,
    steps: List<T>,
    value: T,
    valueText: @Composable (T) -> String,
    onValueChange: (T) -> Unit,
    spokenValue: @Composable (T) -> String = valueText,
) {
    // Follow the finger locally and save once the drag ends, instead of on every step.
    var position by remember(value, steps) { mutableFloatStateOf(steps.stepIndexOf(value).toFloat()) }
    var moved by remember(value) { mutableStateOf(false) }
    val shown = if (moved) steps[position.roundToInt()] else value
    val labelText = stringResource(label)
    val spoken = spokenValue(shown)

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // The value on the label's line, so the slider gets the whole width whatever its length.
        Row {
            Text(labelText, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText(shown),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = position,
            onValueChange = {
                position = it
                moved = true
            },
            onValueChangeFinished = { if (moved) onValueChange(steps[position.roundToInt()]) },
            valueRange = 0f..steps.lastIndex.toFloat(),
            steps = steps.size - 2,
            // Without these TalkBack reads an unnamed percentage instead of "Time, 5 minutes".
            modifier = Modifier.semantics {
                contentDescription = labelText
                stateDescription = spoken
            },
            // Dozens of steps: tick marks would just be a row of dots.
            colors = SliderDefaults.colors(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** The step at [value], or the next one up from a value between steps. */
private fun <T : Comparable<T>> List<T>.stepIndexOf(value: T): Int =
    indexOfFirst { it >= value }.takeIf { it >= 0 } ?: lastIndex

/** "1 h 30 min", as a time is shown beside its slider. */
@Composable
private fun compactDuration(duration: Duration): String = duration.toComponents { hours, minutes, seconds, _ ->
    listOfNotNull(
        stringResource(R.string.duration_hours, hours).takeIf { hours > 0 },
        stringResource(R.string.duration_minutes, minutes).takeIf { minutes > 0 },
        stringResource(R.string.duration_seconds, seconds).takeIf { seconds > 0 || duration == Duration.ZERO },
    ).joinToString(" ")
}

/** "1 hour 30 minutes", as TalkBack reads it. */
@Composable
private fun spokenDuration(duration: Duration): String = duration.toComponents { hours, minutes, seconds, _ ->
    listOfNotNull(
        pluralStringResource(R.plurals.hours, hours.toInt(), hours.toInt()).takeIf { hours > 0 },
        pluralStringResource(R.plurals.minutes, minutes, minutes).takeIf { minutes > 0 },
        pluralStringResource(R.plurals.seconds, seconds, seconds).takeIf { seconds > 0 || duration == Duration.ZERO },
    ).joinToString(" ")
}

/** Increments and delays of up to a minute, in whole seconds. */
private val INCREMENT_STEPS = (0..ClockSettings.MAX_INCREMENT_SECONDS).toList()

private val IncrementMode.label: Int
    @StringRes get() = when (this) {
        IncrementMode.Fischer -> R.string.increment_mode_fischer
        IncrementMode.Bronstein -> R.string.increment_mode_bronstein
        IncrementMode.SimpleDelay -> R.string.increment_mode_delay
    }

private val IncrementMode.description: Int
    @StringRes get() = when (this) {
        IncrementMode.Fischer -> R.string.increment_mode_fischer_description
        IncrementMode.Bronstein -> R.string.increment_mode_bronstein_description
        IncrementMode.SimpleDelay -> R.string.increment_mode_delay_description
    }

private val IncrementMode.sliderLabel: Int
    @StringRes get() = when (this) {
        IncrementMode.Fischer, IncrementMode.Bronstein -> R.string.setting_increment
        IncrementMode.SimpleDelay -> R.string.setting_delay
    }

/** "3 + 2" is how players write it, but read aloud it means little: spelled out for TalkBack. */
@Composable
private fun presetDescription(preset: TimePreset, mode: IncrementMode): String = stringResource(
    mode.presetDescription,
    pluralStringResource(R.plurals.minutes, preset.minutes, preset.minutes),
    pluralStringResource(R.plurals.seconds, preset.incrementSeconds, preset.incrementSeconds),
)

private val IncrementMode.presetDescription: Int
    @StringRes get() = when (this) {
        IncrementMode.Fischer -> R.string.preset_description
        IncrementMode.Bronstein -> R.string.preset_description_bronstein
        IncrementMode.SimpleDelay -> R.string.preset_description_delay
    }

private val Orientation.label: Int
    @StringRes get() = when (this) {
        Orientation.HorizontalSame -> R.string.orientation_horizontal_same
        Orientation.HorizontalOpposite -> R.string.orientation_horizontal_opposite
        Orientation.VerticalSame -> R.string.orientation_vertical_same
        Orientation.VerticalOpposite -> R.string.orientation_vertical_opposite
    }

private val Orientation.description: Int
    @StringRes get() = when (this) {
        Orientation.HorizontalSame -> R.string.orientation_horizontal_same_description
        Orientation.HorizontalOpposite -> R.string.orientation_horizontal_opposite_description
        Orientation.VerticalSame -> R.string.orientation_vertical_same_description
        Orientation.VerticalOpposite -> R.string.orientation_vertical_opposite_description
    }

private val FaceStyle.label: Int
    @StringRes get() = when (this) {
        FaceStyle.Rings -> R.string.face_style_rings
        FaceStyle.Panels -> R.string.face_style_panels
    }

private val ThemeMode.label: Int
    @StringRes get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

private val ColorPalette.label: Int
    @StringRes get() = when (this) {
        ColorPalette.Walnut -> R.string.palette_walnut
        ColorPalette.Forest -> R.string.palette_forest
        ColorPalette.Ocean -> R.string.palette_ocean
        ColorPalette.Graphite -> R.string.palette_graphite
    }

@Preview(showBackground = true)
@Preview(name = "Whole page", showBackground = true, heightDp = 1500)
@Preview(name = "Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun SettingsScreenPreview() {
    ChessClockTheme {
        SettingsScreen(
            settings = ClockSettings(
                time = 90.minutes,
                incrementSeconds = 30,
                stages = listOf(TimeStage(40, 30.minutes)),
            ),
            onBack = {},
            onOrientationChange = {},
            onFaceStyleChange = {},
            onThemeChange = {},
            onPaletteChange = {},
            onTimeChange = {},
            onSecondTimeChange = {},
            onSeparateTimesChange = {},
            onIncrementChange = {},
            onStageAdd = {},
            onStageChange = { _, _ -> },
            onStageRemove = {},
            onIncrementModeChange = {},
            onVibrateOnTurnEndChange = {},
            onVibrateOnLowTimeChange = {},
            onPresetSelect = {},
        )
    }
}
