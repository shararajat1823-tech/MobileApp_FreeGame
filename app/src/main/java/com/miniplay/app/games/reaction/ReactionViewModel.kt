package com.miniplay.app.games.reaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.audio.SoundEffect
import com.miniplay.app.core.audio.SoundManager
import com.miniplay.app.core.haptics.HapticFeedbackType
import com.miniplay.app.core.haptics.HapticManager
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.usecase.RecordGameResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReactionPhase { IDLE, WAITING, GO, TOO_SOON, RESULT }

data class ReactionUiState(
    val phase: ReactionPhase = ReactionPhase.IDLE,
    val stats: ReactionStats = ReactionStats(),
    val lastRating: ReactionRating? = null,
)

/**
 * Drives the Reaction Test. The scoring and averaging live in [ReactionLogic];
 * this class only sequences the IDLE → WAITING → GO → RESULT state machine,
 * schedules the random "go" delay as a cancellable job, times the tap with a
 * monotonic clock and reports a [GameResult] once per completed attempt.
 *
 * Score direction is LOWER_IS_BETTER, so the recorded score is the raw reaction
 * time in milliseconds and the fastest run becomes the best.
 */
class ReactionViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReactionUiState())
    val uiState = _uiState.asStateFlow()

    /** The pending "turn green" job, kept so an early tap can cancel it. */
    private var waitingJob: Job? = null

    /** Monotonic timestamp (ns) captured the instant the screen turns GO. */
    private var goTimestamp = 0L

    fun onTap() {
        when (_uiState.value.phase) {
            ReactionPhase.IDLE,
            ReactionPhase.TOO_SOON,
            ReactionPhase.RESULT -> startRound()

            ReactionPhase.WAITING -> handleTooSoon()

            ReactionPhase.GO -> handleGo()
        }
    }

    private fun startRound() {
        waitingJob?.cancel()
        _uiState.update { it.copy(phase = ReactionPhase.WAITING, lastRating = null) }
        waitingJob = viewModelScope.launch {
            delay(ReactionLogic.randomDelayMillis())
            // Only advance if we are still waiting (not cancelled by an early tap).
            if (_uiState.value.phase == ReactionPhase.WAITING) {
                goTimestamp = System.nanoTime()
                _uiState.update { it.copy(phase = ReactionPhase.GO) }
            }
        }
    }

    private fun handleTooSoon() {
        waitingJob?.cancel()
        waitingJob = null
        _uiState.update { it.copy(phase = ReactionPhase.TOO_SOON) }
        soundManager.play(SoundEffect.WRONG)
        hapticManager.perform(HapticFeedbackType.ERROR)
    }

    private fun handleGo() {
        val ms = ((System.nanoTime() - goTimestamp) / 1_000_000).toInt()
        val updatedStats = ReactionLogic.record(_uiState.value.stats, ms)
        val rating = ReactionLogic.rate(ms)
        _uiState.update {
            it.copy(phase = ReactionPhase.RESULT, stats = updatedStats, lastRating = rating)
        }
        soundManager.play(SoundEffect.CORRECT)
        hapticManager.perform(HapticFeedbackType.SUCCESS)
        recordAttempt(ms)
    }

    private fun recordAttempt(ms: Int) {
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.REACTION,
                    score = ms.toLong(),
                    won = null,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = ms.toLong(),
                    metrics = mapOf("reactionMs" to ms.toLong()),
                ),
            )
        }
    }

    /** Full session reset: clears the running stats and returns to IDLE. */
    fun reset() {
        waitingJob?.cancel()
        waitingJob = null
        _uiState.value = ReactionUiState()
    }
}
