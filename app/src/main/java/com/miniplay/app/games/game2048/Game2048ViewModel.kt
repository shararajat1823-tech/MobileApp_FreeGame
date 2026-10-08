package com.miniplay.app.games.game2048

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Game2048UiState(
    val state: Game2048State,
    val bestScore: Long = 0,
    val canUndo: Boolean = false,
    val showWin: Boolean = false,
)

/**
 * Drives 2048. All rules come from [Game2048Engine]; this class only forwards
 * swipes, plays feedback, keeps a one-level undo, surfaces the live best score
 * and reports a [GameResult] exactly once per game (when the board locks up, or
 * when the player leaves a game they actually started).
 */
class Game2048ViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(Game2048UiState(state = Game2048Engine.newGame()))
    val uiState = _uiState.asStateFlow()

    /** The board before the last successful move — enough for a single undo. */
    private var previousState: Game2048State? = null
    private var startedAt = System.currentTimeMillis()
    private var resultRecorded = false
    private var madeAMove = false
    /** Guards the win overlay so it celebrates 2048 only once per game. */
    private var celebratedWin = false

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.GAME_2048).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore ?: 0L) }
            }
        }
    }

    fun newGame() {
        previousState = null
        resultRecorded = false
        madeAMove = false
        celebratedWin = false
        startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                state = Game2048Engine.newGame(),
                canUndo = false,
                showWin = false,
            )
        }
    }

    fun onSwipe(direction: Direction) {
        val current = _uiState.value.state
        // The engine returns the SAME instance when nothing moved.
        val next = Game2048Engine.move(current, direction)
        if (next === current) return

        previousState = current
        madeAMove = true

        soundManager.play(SoundEffect.CLICK)
        if (next.score > current.score) soundManager.play(SoundEffect.CORRECT)
        hapticManager.perform(HapticFeedbackType.TICK)

        val reachedWin = next.won && !celebratedWin
        _uiState.update {
            it.copy(
                state = next,
                canUndo = true,
                showWin = if (reachedWin) true else it.showWin,
            )
        }

        if (reachedWin) {
            celebratedWin = true
            soundManager.play(SoundEffect.WIN)
            hapticManager.perform(HapticFeedbackType.SUCCESS)
        }
        if (next.over) finish()
    }

    /** Dismisses the win overlay and lets the player chase a higher score. */
    fun continueAfterWin() {
        celebratedWin = true
        _uiState.update {
            it.copy(
                showWin = false,
                state = it.state.copy(keepGoingAfterWin = true),
            )
        }
    }

    fun undo() {
        val prev = previousState ?: return
        previousState = null
        _uiState.update { it.copy(state = prev, canUndo = false) }
    }

    /**
     * Records the in-progress game if the player made at least one move and it
     * has not been recorded yet. The screen calls this before navigating away so
     * an abandoned game still counts — guarded so play count is never doubled.
     */
    fun recordIfNeeded() {
        if (madeAMove && !resultRecorded) finish()
    }

    private fun finish() {
        if (resultRecorded) return
        resultRecorded = true
        val current = _uiState.value.state
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.GAME_2048,
                    score = current.score.toLong(),
                    won = current.won,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = duration,
                    completed = true,
                    metrics = mapOf("highestTile" to current.highestTile.toLong()),
                ),
            )
        }
    }
}
