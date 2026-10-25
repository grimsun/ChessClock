package com.lab.chessclock.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lab.chessclock.ui.clock.ClockRoute
import com.lab.chessclock.ui.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Clock : Screen

    @Serializable
    data object Settings : Screen
}

@Composable
fun ChessClockApp() {
    val backStack = rememberNavBackStack(Screen.Clock)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes each screen's ViewModel to its back stack entry.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Screen.Clock> {
                ClockRoute(onOpenSettings = { backStack.add(Screen.Settings) })
            }
            entry<Screen.Settings> {
                SettingsRoute(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
