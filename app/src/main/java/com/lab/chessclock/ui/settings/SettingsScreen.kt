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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lab.chessclock.R
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.ColorPalette
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.settings.TimePreset
import com.lab.chessclock.settings.TimerSetting
import com.lab.chessclock.ui.theme.ChessClockTheme
import com.lab.chessclock.ui.theme.paletteSwatch
import kotlin.math.roundToInt

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
        onTimerChange = viewModel::setTimer,
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
    onTimerChange: (TimerSetting, Int) -> Unit,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // One tap for the usual time controls, with the sliders right below for anything else.
            SectionHeader(R.string.presets_title)
            SupportingText(R.string.presets_explanation)
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimePreset.All.forEach { preset ->
                    PresetChip(preset, selected = preset.matches(settings), onClick = { onPresetSelect(preset) })
                }
            }
            TimerSetting.entries.forEach { setting ->
                SettingSlider(
                    label = setting.label,
                    value = setting.valueIn(settings),
                    max = setting.max,
                    onValueChange = { onTimerChange(setting, it) },
                )
            }
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
private fun PresetChip(preset: TimePreset, selected: Boolean, onClick: () -> Unit) {
    // "3 + 2" is how players write it, but read aloud it means little: spell it out for TalkBack.
    val description = stringResource(
        R.string.preset_description,
        pluralStringResource(R.plurals.minutes, preset.minutes, preset.minutes),
        pluralStringResource(R.plurals.seconds, preset.incrementSeconds, preset.incrementSeconds),
    )
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

@Composable
private fun SettingSlider(@StringRes label: Int, value: Int, max: Int, onValueChange: (Int) -> Unit) {
    // Follow the finger locally and save once the drag ends, instead of on every step.
    var position by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val labelText = stringResource(label)

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(labelText, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = position,
                onValueChange = { position = it },
                onValueChangeFinished = { onValueChange(position.roundToInt()) },
                valueRange = 0f..max.toFloat(),
                steps = max - 1,
                // Without these TalkBack reads an unnamed percentage instead of "Minutes, 30".
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = labelText
                        stateDescription = position.roundToInt().toString()
                    },
                // Up to 60 steps: tick marks would just be a row of dots.
                colors = SliderDefaults.colors(
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Text(
                text = position.roundToInt().toString(),
                modifier = Modifier.widthIn(min = 48.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
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

private val TimerSetting.label: Int
    @StringRes get() = when (this) {
        TimerSetting.Hours -> R.string.setting_hours
        TimerSetting.Minutes -> R.string.setting_minutes
        TimerSetting.Seconds -> R.string.setting_seconds
        TimerSetting.Increment -> R.string.setting_increment
    }

@Preview(showBackground = true)
@Preview(name = "Whole page", showBackground = true, heightDp = 1500)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun SettingsScreenPreview() {
    ChessClockTheme {
        SettingsScreen(
            settings = ClockSettings(minutes = 3, incrementSeconds = 2),
            onBack = {},
            onOrientationChange = {},
            onFaceStyleChange = {},
            onThemeChange = {},
            onPaletteChange = {},
            onTimerChange = { _, _ -> },
            onPresetSelect = {},
        )
    }
}
