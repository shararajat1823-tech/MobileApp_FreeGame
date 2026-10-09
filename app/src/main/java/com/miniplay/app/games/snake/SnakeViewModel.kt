package com.miniplay.app.games.snake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.audio.SoundEffect
import com.miniplay.app.core.audio.SoundManager
import com.miniplay.app.core.haptics.HapticFeedbackType
import com.miniplay.app.core.haptics.HapticManager
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.domain.usecase.RecordGameResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SnakeUiState(
    val game: SnakeState,
    val started: Boolean = false,
    val bestScore: Long = 0,
    /** Increments once per engine step; the UI uses it to detect a new tick. */
    val tick: Long = 0L,
    /** Current step interval, so the UI can interpolate motion smoothly. */
    val tickIntervalMillis: Long = 200L,
) {
    val gameOver: Boolean get() = game.status == SnakeStatus.GAME_OVER
}

/**
 * Drives Snake. The engine owns the rules; this class runs the fixed-interval
 * game clock (which accelerates as the snake grows), relays turns, plays
 * feedback and records a result once per game.
 */
class SnakeViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnakeUiState(SnakeEngine.newGame()))
    val uiState = _uiState.asStateFlow()

    private var loopJob: Job? = null
    private var startedAt = 0L
    private var resultRecorded = false

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.SNAKE).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore ?: 0L) }
            }
        }
    }

    fun newGame() {
        loopJob?.cancel()
        loopJob = null
        resultRecorded = false
        _uiState.update { it.copy(game = SnakeEngine.newGame(), started = false, tick = 0L) }
    }

    fun turn(dir: SnakeDir) {
        _uiState.update { it.copy(game = SnakeEngine.turn(it.game, dir)) }
        start()
    }

    fun start() {
        if (_uiState.value.started || _uiState.value.gameOver) return
        _uiState.update { it.copy(started = true) }
        startedAt = System.currentTimeMillis()
        loopJob = viewModelScope.launch {
            while (_uiState.value.game.status == SnakeStatus.RUNNING) {
                delay(intervalMillis(_uiState.value.game.length))
                tick()
            }
        }
    }

    private fun tick() {
        val prev = _uiState.value.game
        if (prev.status != SnakeStatus.RUNNING) return
        val next = SnakeEngine.step(prev)
        _uiState.update {
            it.copy(game = next, tick = it.tick + 1, tickIntervalMillis = intervalMillis(next.length))
        }

        if (next.score > prev.score) {
            soundManager.play(SoundEffect.CORRECT)
            hapticManager.perform(HapticFeedbackType.TICK)
        }
        if (next.status == SnakeStatus.GAME_OVER) {
            soundManager.play(SoundEffect.GAME_OVER)
            hapticManager.perform(HapticFeedbackType.ERROR)
            finish(next)
        }
    }

    private fun finish(state: SnakeState) {
        if (resultRecorded) return
        resultRecorded = true
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.SNAKE,
                    score = state.score.toLong(),
                    won = null,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = duration,
                    completed = true,
                    metrics = mapOf("length" to state.length.toLong()),
                ),
            )
        }
    }

    /** Speeds up as the snake grows: 200ms down to a 80ms floor. */
    private fun intervalMillis(length: Int): Long =
        (200L - (length - 3) * 4L).coerceAtLeast(80L)
}
