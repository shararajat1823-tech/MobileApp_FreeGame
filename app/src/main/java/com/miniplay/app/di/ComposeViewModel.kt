package com.miniplay.app.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Builds a [ViewModel] from the [AppContainer] with the correct Compose
 * lifecycle scoping. This is the one place ViewModels are instantiated, so
 * screens stay free of factory boilerplate:
 *
 * ```
 * val vm = rememberViewModel { container -> HomeViewModel(container.gameStatsRepository) }
 * ```
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
