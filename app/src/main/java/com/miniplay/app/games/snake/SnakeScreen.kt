package com.miniplay.app.games.snake

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import com.miniplay.app.di.rememberViewModel
import kotlinx.coroutines.delay

/**
 * Router for the Snake experience. Owns a single [SnakeViewModel] and switches
 * between the menu (mode / skin / control selection) and the in-game screen
 * (countdown → running → paused → game over) based on the current phase.
 */
@Composable
fun SnakeScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        SnakeViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.snakePrefs,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pause the game whenever the app is backgrounded so the snake never moves
    // while the player isn't looking.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pauseForLifecycle()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-dismiss the first-play tutorial after a short read window.
    LaunchedEffect(state.showTutorial) {
        if (state.showTutorial) {
            delay(6500)
            viewModel.dismissTutorial()
        }
    }

    BackHandler(enabled = true) {
        when (state.phase) {
            SnakePhase.MENU -> onExit()
            SnakePhase.RUNNING -> viewModel.pauseGame()
            else -> viewModel.backToMenu()
        }
    }

    when (state.phase) {
        SnakePhase.MENU -> SnakeMenuScreen(
            state = state,
            onExit = onExit,
            onPlay = viewModel::startNewGame,
            onSetSkin = viewModel::setSkin,
            onSetSensitivity = viewModel::setSensitivity,
            onSetControlStyle = viewModel::setControlStyle,
        )
        else -> SnakeGameScreen(
            state = state,
            eatEvents = viewModel.eatEvents,
            onTurn = viewModel::turn,
            onPause = viewModel::pauseGame,
            onResume = viewModel::resumeGame,
            onRetry = viewModel::retry,
            onMenu = viewModel::backToMenu,
            onDismissTutorial = viewModel::dismissTutorial,
        )
    }
}
