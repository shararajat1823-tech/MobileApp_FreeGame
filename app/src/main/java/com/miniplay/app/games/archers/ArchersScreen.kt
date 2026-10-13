package com.miniplay.app.games.archers

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.di.rememberViewModel
import kotlinx.coroutines.delay

/**
 * Entry point / router for Ragdoll Archers. Owns one [ArcherViewModel] and
 * switches between the menu and the match; pauses on background and wires Back.
 */
@Composable
fun ArchersScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        ArcherViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.archerPrefs,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.showTutorial) {
        if (state.showTutorial) {
            delay(8000)
            viewModel.dismissTutorial()
        }
    }

    BackHandler(enabled = true) {
        when {
            state.screen == ArcherScreen.MENU -> onExit()
            state.winner != null -> viewModel.backToMenu()
            !state.paused -> viewModel.pause()
            else -> viewModel.backToMenu()
        }
    }

    when (state.screen) {
        ArcherScreen.MENU -> ArcherMenuScreen(
            state = state,
            onExit = onExit,
            onPlay = viewModel::startMatch,
            onSetDifficulty = viewModel::setDifficulty,
            onSetScreenShake = viewModel::setScreenShake,
            onSetReducedEffects = viewModel::setReducedEffects,
            onResetProgress = viewModel::resetProgress,
        )
        ArcherScreen.PLAYING -> ArcherGameScreen(
            state = state,
            match = viewModel.match,
            onFrame = viewModel::onFrame,
            onAim = viewModel::updateAim,
            onRelease = viewModel::releaseAim,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onPlayAgain = { viewModel.startMatch(state.mode) },
            onChangeDifficulty = viewModel::backToMenu,
            onMenu = viewModel::backToMenu,
            onDismissTutorial = viewModel::dismissTutorial,
        )
    }
}
