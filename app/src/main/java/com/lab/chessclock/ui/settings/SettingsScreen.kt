package com.lab.chessclock.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
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
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.TimePreset
import com.lab.chessclock.settings.TimerSetting
import com.lab.chessclock.ui.theme.ChessClockTheme
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
            // One tap for the usual time controls; the sliders below stay for anything else.
            SectionHeader(R.string.presets_title)
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimePreset.All.forEach { preset ->
                    FilterChip(
                        selected = preset.matches(settings),
                        onClick = { onPresetSelect(preset) },
                        label = { Text(stringResource(R.string.preset_label, preset.minutes, preset.incrementSeconds)) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.orientation_title)
            Column(Modifier.selectableGroup()) {
                Orientation.entries.forEach { orientation ->
                    OrientationOption(
                        orientation = orientation,
                        selected = orientation == settings.orientation,
                        onSelect = { onOrientationChange(orientation) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.timer_section)
            TimerSetting.entries.forEach { setting ->
                SettingSlider(
                    label = setting.label,
                    value = setting.valueIn(settings),
                    max = setting.max,
                    onValueChange = { onTimerChange(setting, it) },
                )
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
private fun OrientationOption(orientation: Orientation, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(orientation.label), style = MaterialTheme.typography.bodyLarge)
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

private val TimerSetting.label: Int
    @StringRes get() = when (this) {
        TimerSetting.Hours -> R.string.setting_hours
        TimerSetting.Minutes -> R.string.setting_minutes
        TimerSetting.Seconds -> R.string.setting_seconds
        TimerSetting.Increment -> R.string.setting_increment
    }

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ChessClockTheme {
        SettingsScreen(
            settings = ClockSettings(minutes = 3, incrementSeconds = 2),
            onBack = {},
            onOrientationChange = {},
            onTimerChange = { _, _ -> },
            onPresetSelect = {},
        )
    }
}
