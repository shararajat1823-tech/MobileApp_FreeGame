package com.miniplay.app.games.tapchallenge

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class TapPhase { IDLE, RUNNING, FINISHED }

/** A single on-screen target. Position and size are fractions (0..1) of the play area. */
data class Target(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float,
    val radiusFraction: Float,
)

data class TapUiState(
    val phase: TapPhase = TapPhase.IDLE,
    val stats: TapStats = TapStats(),
    val millisLeft: Long = TapEngine.DURATION_MS,
    val target: Target? = null,
    val bestScore: Long = 0,
)

/**
 * Drives Tap Challenge. All scoring, the combo multiplier and the difficulty ramp
 * come from [TapEngine]; this class only runs the clock, spawns/expires targets,
 * plays feedback and reports a [GameResult] once per finished run.
 */
class TapViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TapUiState())
    val uiState = _uiState.asStateFlow()

    private var clockJob: Job? = null
    private var resultRecorded = false
    private var nextTargetId = 0L
    private var spawnedAt = 0L

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.TAP_CHALLENGE).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore ?: 0L) }
            }
        }
    }

    /** Starts a fresh run (also used for "New game" / "Play again"). */
    fun start() {
        clockJob?.cancel()
        resultRecorded = false
        _uiState.update {
            it.copy(
                phase = TapPhase.RUNNING,
                stats = TapStats(),
                millisLeft = TapEngine.DURATION_MS,
                target = null,
            )
        }
        spawnTarget(elapsedFraction = 0f)
        startClock()
    }

    fun onTargetHit() {
        if (_uiState.value.phase != TapPhase.RUNNING) return
        val newStats = TapEngine.onHit(_uiState.value.stats)
        _uiState.update { it.copy(stats = newStats) }
        soundManager.play(SoundEffect.CORRECT)
        hapticManager.perform(HapticFeedbackType.LIGHT)
        spawnTarget(elapsedFraction = currentElapsedFraction())
    }

    /** A tap that landed on empty space. Costs points and breaks the combo; the target stays. */
    fun onMissTap() {
        if (_uiState.value.phase != TapPhase.RUNNING) return
        val newStats = TapEngine.onMiss(_uiState.value.stats)
        _uiState.update { it.copy(stats = newStats) }
        soundManager.play(SoundEffect.WRONG)
        hapticManager.perform(HapticFeedbackType.TICK)
    }

    private fun startClock() {
        clockJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            while (isActive) {
                delay(FRAME_MILLIS)
                val elapsed = System.currentTimeMillis() - startedAt
                val left = (TapEngine.DURATION_MS - elapsed).coerceAtLeast(0L)
                _uiState.update { it.copy(millisLeft = left) }

                val fraction = elapsedFractionFor(left)
                // Target expiry → counts as a miss and a fresh target appears.
                if (System.currentTimeMillis() - spawnedAt > TapEngine.targetLifetimeMs(fraction)) {
                    _uiState.update { it.copy(stats = TapEngine.onMiss(it.stats)) }
                    spawnTarget(elapsedFraction = fraction)
                }

                if (left <= 0L) {
                    finish()
                    break
                }
            }
        }
    }

    private fun finish() {
        if (resultRecorded) return
        resultRecorded = true
        clockJob?.cancel()
        clockJob = null
        val stats = _uiState.value.stats
        _uiState.update { it.copy(phase = TapPhase.FINISHED, target = null) }
        soundManager.play(SoundEffect.WIN)
        hapticManager.perform(HapticFeedbackType.SUCCESS)
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.TAP_CHALLENGE,
                    score = stats.score.toLong(),
                    won = null,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = TapEngine.DURATION_MS,
                    flawless = stats.flawless,
                    completed = true,
                    metrics = mapOf(
                        "hits" to stats.hits.toLong(),
                        "bestCombo" to stats.bestCombo.toLong(),
                    ),
                ),
            )
        }
    }

    private fun spawnTarget(elapsedFraction: Float) {
        val radiusFraction = TapEngine.targetRadiusFraction(elapsedFraction)
        val target = Target(
            id = nextTargetId++,
            xFraction = randomFraction(),
            yFraction = randomFraction(),
            radiusFraction = radiusFraction,
        )
        spawnedAt = System.currentTimeMillis()
        _uiState.update { it.copy(target = target) }
    }

    private fun currentElapsedFraction(): Float = elapsedFractionFor(_uiState.value.millisLeft)

    private fun elapsedFractionFor(millisLeft: Long): Float =
        (1f - millisLeft.toFloat() / TapEngine.DURATION_MS).coerceIn(0f, 1f)

    private fun randomFraction(): Float =
        MIN_FRACTION + Random.nextFloat() * (MAX_FRACTION - MIN_FRACTION)

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        clockJob = null
    }

    private companion object {
        const val FRAME_MILLIS = 16L
        const val MIN_FRACTION = 0.12f
        const val MAX_FRACTION = 0.88f
    }
}
