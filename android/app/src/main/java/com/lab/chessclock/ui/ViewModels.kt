package com.lab.chessclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lab.chessclock.AppContainer
import com.lab.chessclock.ChessClockApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** A factory that builds the ViewModel from the app's [AppContainer]. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: AppContainer.() -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { (this[APPLICATION_KEY] as ChessClockApplication).container.create() }
}

/**
 * Holds the latest value for the UI. Upstream stays active for 5s after the last collector
 * leaves, so a configuration change doesn't restart it.
 */
fun <T> Flow<T>.stateInUi(viewModel: ViewModel, initialValue: T): StateFlow<T> =
    stateIn(viewModel.viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)
