package com.lab.chessclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Clock-specific colors that have no Material color role, each with the color for content on it. */
@Immutable
data class ClockColors(
    val win: Color,
    val onWin: Color,
    val loss: Color,
    val onLoss: Color,
    val inactive: Color,
    val onInactive: Color,
    /** The time drawn inside a ring, on the screen background. */
    val text: Color,
)

private val LightClockColors = ClockColors(
    win = Color(0xFF52643F),
    onWin = Color.White,
    loss = Color(0xFFBA1A1A),
    onLoss = Color.White,
    inactive = Color(0xFFE4BFA7),
    onInactive = Color(0xFF4E2600),
    text = Color(0xFF4E2600),
)

private val DarkClockColors = ClockColors(
    win = Color(0xFFB8CE9D),
    onWin = Color(0xFF253514),
    loss = Color(0xFFFFB4AB),
    onLoss = Color(0xFF690005),
    inactive = Color(0xFF53443A),
    onInactive = Color(0xFFFFDCC5),
    text = Color(0xFFFFDCC5),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF924C00),
    secondary = Color(0xFF745945),
    tertiary = Color(0xFF5D6136),
    secondaryContainer = LightClockColors.inactive,
    onSecondaryContainer = LightClockColors.onInactive,
    background = Color.White,
    surface = Color.White,
)

// Warm dark surfaces rather than neutral grey, so the browns still read as the same app.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4E2600),
    secondary = Color(0xFFE4BFA7),
    onSecondary = Color(0xFF422B1B),
    tertiary = Color(0xFFC6CA95),
    secondaryContainer = DarkClockColors.inactive,
    onSecondaryContainer = DarkClockColors.onInactive,
    background = Color(0xFF19120D),
    onBackground = Color(0xFFF0DFD6),
    surface = Color(0xFF19120D),
    onSurface = Color(0xFFF0DFD6),
    surfaceContainerHighest = Color(0xFF3C332D),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD7C3B7),
    outline = Color(0xFFA08D82),
    outlineVariant = Color(0xFF52443B),
)

private val LocalClockColors = staticCompositionLocalOf { LightClockColors }

/** The clock colors of the current [ChessClockTheme]. */
val MaterialTheme.clockColors: ClockColors
    @Composable @ReadOnlyComposable get() = LocalClockColors.current

@Composable
fun ChessClockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalClockColors provides if (darkTheme) DarkClockColors else LightClockColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}
