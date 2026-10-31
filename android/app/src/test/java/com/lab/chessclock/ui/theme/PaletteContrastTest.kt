package com.lab.chessclock.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.lab.chessclock.settings.ColorPalette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class PaletteContrastTest {

    @Test
    fun everyTextColorMeetsWcagAaOnItsBackground() {
        ColorPalette.entries.forEach { palette ->
            listOf("light" to palette.colors.light, "dark" to palette.colors.dark).forEach { (mode, colors) ->
                with(colors) {
                    mapOf(
                        "onPrimary" to (onPrimary to primary),
                        "onInactive" to (onInactive to inactive),
                        "onWin" to (onWin to win),
                        "onLoss" to (onLoss to loss),
                        "onBackground" to (onBackground to background),
                        "ring text" to (clockColors.text to background),
                    ).forEach { (name, pair) ->
                        val ratio = contrast(pair.first, pair.second)
                        assertTrue("$palette $mode $name contrast is $ratio", ratio >= 4.5)
                    }
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (lighter, darker) = max(a.luminance(), b.luminance()) to min(a.luminance(), b.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }
}
