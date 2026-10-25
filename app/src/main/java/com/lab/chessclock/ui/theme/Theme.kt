package com.lab.chessclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Clock-specific colors that have no Material color role. */
object ClockColors {
    val Win = Color(0xFF52643F)
    val Loss = Color(0xFFBA1A1A)
    val Inactive = Color(0xFFE4BFA7)
    val Text = Color(0xFF4E2600)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF924C00),
    secondary = Color(0xFF745945),
    tertiary = Color(0xFF5D6136),
    secondaryContainer = ClockColors.Inactive,
    onSecondaryContainer = ClockColors.Text,
    background = Color.White,
    surface = Color.White,
)

@Composable
fun ChessClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
