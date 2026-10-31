package com.lab.chessclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.lab.chessclock.settings.ColorPalette

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

/** The few colors a palette picks for one of light or dark; the rest of the scheme derives from them. */
internal class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val inactive: Color,
    val onInactive: Color,
    val win: Color,
    val onWin: Color,
    val loss: Color,
    val onLoss: Color,
    val background: Color,
    val onBackground: Color,
) {
    val clockColors = ClockColors(win, onWin, loss, onLoss, inactive, onInactive, text = onInactive)

    /**
     * Every Material role set from the palette, with surfaces and outlines blended from its own
     * colors, so no role falls back to Material's default purple.
     */
    fun colorScheme(dark: Boolean): ColorScheme {
        fun tint(amount: Float) = lerp(background, primary, amount)
        fun fade(amount: Float) = lerp(onBackground, background, amount)
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = tint(0.25f),
            onPrimaryContainer = onBackground,
            inversePrimary = lerp(primary, background, 0.5f),
            secondary = lerp(primary, onBackground, 0.4f),
            onSecondary = background,
            secondaryContainer = inactive,
            onSecondaryContainer = onInactive,
            tertiary = win,
            onTertiary = onWin,
            tertiaryContainer = lerp(background, win, 0.25f),
            onTertiaryContainer = onBackground,
            error = loss,
            onError = onLoss,
            errorContainer = lerp(background, loss, 0.25f),
            onErrorContainer = onBackground,
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = tint(0.12f),
            onSurfaceVariant = fade(0.25f),
            surfaceTint = primary,
            inverseSurface = onBackground,
            inverseOnSurface = background,
            outline = fade(0.5f),
            outlineVariant = fade(0.8f),
            scrim = Color.Black,
            surfaceBright = tint(if (dark) 0.14f else 0f),
            surfaceDim = tint(if (dark) 0f else 0.1f),
            surfaceContainerLowest = background,
            surfaceContainerLow = tint(0.04f),
            surfaceContainer = tint(0.07f),
            surfaceContainerHigh = tint(0.1f),
            surfaceContainerHighest = tint(0.14f),
        )
    }
}

internal class Palette(val light: PaletteColors, val dark: PaletteColors)

private val Walnut = Palette(
    light = PaletteColors(
        primary = Color(0xFF924C00), onPrimary = Color.White,
        inactive = Color(0xFFE4BFA7), onInactive = Color(0xFF4E2600),
        win = Color(0xFF52643F), onWin = Color.White,
        loss = Color(0xFFBA1A1A), onLoss = Color.White,
        background = Color.White, onBackground = Color(0xFF221A14),
    ),
    dark = PaletteColors(
        primary = Color(0xFFFFB77C), onPrimary = Color(0xFF4E2600),
        inactive = Color(0xFF53443A), onInactive = Color(0xFFFFDCC5),
        win = Color(0xFFB8CE9D), onWin = Color(0xFF253514),
        loss = Color(0xFFFFB4AB), onLoss = Color(0xFF690005),
        background = Color(0xFF19120D), onBackground = Color(0xFFF0DFD6),
    ),
)

// Chessboard green; the finished winner takes teal so it doesn't read as just "on move".
private val Forest = Palette(
    light = PaletteColors(
        primary = Color(0xFF3B6939), onPrimary = Color.White,
        inactive = Color(0xFFD5E8CF), onInactive = Color(0xFF102010),
        win = Color(0xFF00696E), onWin = Color.White,
        loss = Color(0xFFBA1A1A), onLoss = Color.White,
        background = Color(0xFFFBFDF7), onBackground = Color(0xFF191D17),
    ),
    dark = PaletteColors(
        primary = Color(0xFFA1D39A), onPrimary = Color(0xFF0A390F),
        inactive = Color(0xFF3A4B37), onInactive = Color(0xFFD5E8CF),
        win = Color(0xFF80D4DA), onWin = Color(0xFF003739),
        loss = Color(0xFFFFB4AB), onLoss = Color(0xFF690005),
        background = Color(0xFF11140F), onBackground = Color(0xFFE1E4DC),
    ),
)

// Blue against orange, which stays distinct with red-green color blindness.
private val Ocean = Palette(
    light = PaletteColors(
        primary = Color(0xFF2A5EA8), onPrimary = Color.White,
        inactive = Color(0xFFD6E3FF), onInactive = Color(0xFF001B3E),
        win = Color(0xFF006A60), onWin = Color.White,
        loss = Color(0xFFA93B00), onLoss = Color.White,
        background = Color(0xFFFAFBFF), onBackground = Color(0xFF1A1C20),
    ),
    dark = PaletteColors(
        primary = Color(0xFFA8C8FF), onPrimary = Color(0xFF003062),
        inactive = Color(0xFF2E3A4E), onInactive = Color(0xFFD6E3FF),
        win = Color(0xFF7FD6C9), onWin = Color(0xFF003731),
        loss = Color(0xFFFFB599), onLoss = Color(0xFF5A1A00),
        background = Color(0xFF111318), onBackground = Color(0xFFE2E2E9),
    ),
)

// High contrast; the dark variant is true black for OLED screens.
private val Graphite = Palette(
    light = PaletteColors(
        primary = Color(0xFF1B1B1B), onPrimary = Color.White,
        inactive = Color(0xFFE4E4E4), onInactive = Color(0xFF1B1B1B),
        win = Color(0xFF2E6B30), onWin = Color.White,
        loss = Color(0xFFC62828), onLoss = Color.White,
        background = Color.White, onBackground = Color(0xFF1B1B1B),
    ),
    dark = PaletteColors(
        primary = Color(0xFFF2F2F2), onPrimary = Color(0xFF1B1B1B),
        inactive = Color(0xFF2E2E2E), onInactive = Color(0xFFF2F2F2),
        win = Color(0xFFA5D6A7), onWin = Color(0xFF0B3A0E),
        loss = Color(0xFFFFB4AB), onLoss = Color(0xFF690005),
        background = Color.Black, onBackground = Color(0xFFE8E8E8),
    ),
)

internal val ColorPalette.colors: Palette
    get() = when (this) {
        ColorPalette.Walnut -> Walnut
        ColorPalette.Forest -> Forest
        ColorPalette.Ocean -> Ocean
        ColorPalette.Graphite -> Graphite
    }

/** The colors a swatch shows for [palette]: the player on move against the one waiting. */
fun paletteSwatch(palette: ColorPalette, dark: Boolean): Pair<Color, Color> =
    (if (dark) palette.colors.dark else palette.colors.light).let { it.primary to it.inactive }

private val LocalClockColors = staticCompositionLocalOf { Walnut.light.clockColors }

/** The clock colors of the current [ChessClockTheme]. */
val MaterialTheme.clockColors: ClockColors
    @Composable @ReadOnlyComposable get() = LocalClockColors.current

@Composable
fun ChessClockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ColorPalette = ColorPalette.Walnut,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) palette.colors.dark else palette.colors.light
    val colorScheme = remember(colors, darkTheme) { colors.colorScheme(darkTheme) }
    CompositionLocalProvider(LocalClockColors provides colors.clockColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
