package com.lab.chessclock

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.ui.ChessClockApp
import com.lab.chessclock.ui.theme.ChessClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsFlow = (application as ChessClockApplication).container.settingsRepository.settings
        setContent {
            // Wait for the stored theme rather than flash the system one first; the window
            // background shows for the few milliseconds the first read takes.
            val stored by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val settings = stored ?: return@setContent
            val darkTheme = when (settings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Transparent bars whose icons follow the app's theme, which can differ from the system's.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            ChessClockTheme(darkTheme, settings.palette) {
                ChessClockApp()
            }
        }
    }
}
