package com.miniplay.app.games.minesweeper

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

data class MinesweeperUiState(
    val game: MinesweeperState,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val elapsedMillis: Long = 0L,
    val bestScore: Long = 0L,
    val finalScore: Int? = null,
) {
    val won: Boolean get() = game.status == MineStatus.WON
    val lost: Boolean get() = game.status == MineStatus.LOST
    val finished: Boolean get() = game.isOver
}

/**
 * Drives Minesweeper. The engine owns the rules; this class maps difficulty to a
 * board, runs the clock, relays taps/flags, and records a result once per game.
 * Score rewards a win and speed; a loss scores 0 so it never becomes a best.
 */
class MinesweeperViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MinesweeperUiState(newBoard(GameDifficulty.EASY)))
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var timerStarted = false
    private var resultRecorded = false

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.MINESWEEPER).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore ?: 0L) }
            }
        }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        newGame()
    }

    fun newGame() {
        timerJob?.cancel()
        timerStarted = false
        resultRecorded = false
        val difficulty = _uiState.value.difficulty
        _uiState.update {
            it.copy(game = newBoard(difficulty), elapsedMillis = 0L, finalScore = null)
        }
    }

    fun onReveal(index: Int) {
        val current = _uiState.value
        if (current.game.isOver) return
        startTimer()
        val next = MinesweeperEngine.reveal(current.game, index)
        _uiState.update { it.copy(game = next) }
        when (next.status) {
            MineStatus.LOST -> {
                soundManager.play(SoundEffect.GAME_OVER)
                hapticManager.perform(HapticFeedbackType.ERROR)
                finish(next)
            }
            MineStatus.WON -> {
                soundManager.play(SoundEffect.WIN)
                hapticManager.perform(HapticFeedbackType.SUCCESS)
                finish(next)
            }
            else -> {
                soundManager.play(SoundEffect.CLICK)
                hapticManager.perform(HapticFeedbackType.TICK)
            }
        }
    }

    fun onFlag(index: Int) {
        val current = _uiState.value
        if (current.game.isOver) return
        _uiState.update { it.copy(game = MinesweeperEngine.toggleFlag(it.game, index)) }
        soundManager.play(SoundEffect.CLICK)
        hapticManager.perform(HapticFeedbackType.LIGHT)
    }

    private fun startTimer() {
        if (timerStarted) return
        timerStarted = true
        timerJob = viewModelScope.launch {
            while (!_uiState.value.game.isOver) {
                delay(250)
                _uiState.update { it.copy(elapsedMillis = it.elapsedMillis + 250) }
            }
        }
    }

    private fun finish(state: MinesweeperState) {
        if (resultRecorded) return
        resultRecorded = true
        timerJob?.cancel()
        val won = state.status == MineStatus.WON
        val elapsedSeconds = (_uiState.value.elapsedMillis / 1000).toInt()
        val score = if (won) scoreFor(_uiState.value.difficulty, elapsedSeconds) else 0
        _uiState.update { it.copy(finalScore = score) }
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.MINESWEEPER,
                    score = score.toLong(),
                    won = won,
                    difficulty = _uiState.value.difficulty,
                    durationMillis = _uiState.value.elapsedMillis,
                    flawless = won, // a clean clear hits no mine
                    completed = true,
                    metrics = mapOf("timeSec" to elapsedSeconds.toLong()),
                ),
            )
        }
    }

    private fun scoreFor(difficulty: GameDifficulty, elapsedSeconds: Int): Int {
        val base = when (difficulty) {
            GameDifficulty.EASY -> 500
            GameDifficulty.MEDIUM -> 1000
            GameDifficulty.HARD -> 2000
        }
        val timeBonus = (300 - elapsedSeconds).coerceAtLeast(0) * 2
        return base + timeBonus
    }

    private fun newBoard(difficulty: GameDifficulty): MinesweeperState {
        val (rows, cols, mines) = dims(difficulty)
        return MinesweeperEngine.newGame(rows, cols, mines)
    }

    private fun dims(difficulty: GameDifficulty): Triple<Int, Int, Int> = when (difficulty) {
        GameDifficulty.EASY -> Triple(9, 9, 10)
        GameDifficulty.MEDIUM -> Triple(12, 10, 22)
        GameDifficulty.HARD -> Triple(14, 11, 36)
    }
}
