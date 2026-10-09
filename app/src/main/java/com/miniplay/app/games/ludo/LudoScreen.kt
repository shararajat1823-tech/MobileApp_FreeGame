package com.miniplay.app.games.ludo

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
 * Router for Ludo: owns one [LudoViewModel] and switches between the menu and the
 * in-game screen. Persists/pauses the match on background and wires the back button.
 */
@Composable
fun LudoScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        LudoViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.ludoPrefs,
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
            delay(7000)
            viewModel.dismissTutorial()
        }
    }

    BackHandler(enabled = true) {
        when {
            state.screen == LudoScreenPhase.MENU -> onExit()
            state.winner != null -> viewModel.backToMenu()
            !state.paused -> viewModel.pauseGame()
            else -> viewModel.backToMenu()
        }
    }

    when (state.screen) {
        LudoScreenPhase.MENU -> LudoMenuScreen(
            state = state,
            onExit = onExit,
            onPlay = viewModel::startGame,
            onResume = viewModel::resumeSavedMatch,
            onSetDifficulty = viewModel::setBotDifficulty,
            onSetReducedEffects = viewModel::setReducedEffects,
            onSetQuickWin = viewModel::setQuickWin,
            onSetThreeSixes = viewModel::setThreeSixes,
        )
        LudoScreenPhase.PLAYING -> LudoGameScreen(
            state = state,
            moveAnims = viewModel.moveAnims,
            onRoll = viewModel::onRollClick,
            onTokenTap = viewModel::onTokenTap,
            onPause = viewModel::pauseGame,
            onResume = viewModel::resumeGame,
            onPlayAgain = { viewModel.startGame(state.mode, state.seats.size) },
            onMenu = viewModel::backToMenu,
            onDismissTutorial = viewModel::dismissTutorial,
        )
    }
}
