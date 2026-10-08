package com.miniplay.app.games.brickbreaker

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

/**
 * UI state for Brick Breaker. [world] is the authoritative simulation from
 * [BrickBreaker]; [bestScore] is the persisted high score shown on the game-over
 * overlay.
 */
data class BrickUiState(
    val world: BrickWorld,
    val bestScore: Long = 0,
)

/**
 * Drives Brick Breaker. All physics, collisions, scoring and life handling live
 * in the pure [BrickBreaker] engine; this class only advances the simulation on
 * the frame clock supplied by the screen, fires audio/haptic feedback on the
 * transitions it detects between successive worlds, keeps the persisted best and
 * reports a [GameResult] once per finished game.
 */
class BrickBreakerViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrickUiState(world = BrickBreaker.newGame(level = 1)))
    val uiState = _uiState.asStateFlow()

    private var startedAt: Long = System.currentTimeMillis()
    private var resultRecorded = false

    /** The authoritative world; read live so the frame loop always ticks the latest. */
    private val world: BrickWorld get() = _uiState.value.world

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.BRICK_BREAKER).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore ?: 0) }
            }
        }
    }

    fun newGame() {
        resultRecorded = false
        startedAt = System.currentTimeMillis()
        _uiState.update { it.copy(world = BrickBreaker.newGame(level = 1)) }
    }

    fun launch() {
        _uiState.update { it.copy(world = BrickBreaker.launch(it.world)) }
    }

    fun onPaddleMove(centerXWorld: Float) {
        _uiState.update { it.copy(world = BrickBreaker.movePaddle(it.world, centerXWorld)) }
    }

    /** Advances the simulation by [dt] seconds and reacts to what changed. */
    fun tick(dt: Float) {
        val prev = world
        val next = BrickBreaker.step(prev, dt)
        if (next === prev) return
        _uiState.update { it.copy(world = next) }

        if (next.score > prev.score) {
            soundManager.play(SoundEffect.CLICK)
            hapticManager.perform(HapticFeedbackType.TICK)
        }
        if (next.lives < prev.lives) {
            soundManager.play(SoundEffect.WRONG)
            hapticManager.perform(HapticFeedbackType.ERROR)
        }
        if (prev.status != BrickStatus.LEVEL_CLEARED && next.status == BrickStatus.LEVEL_CLEARED) {
            soundManager.play(SoundEffect.WIN)
            hapticManager.perform(HapticFeedbackType.SUCCESS)
        }
        if (prev.status != BrickStatus.GAME_OVER && next.status == BrickStatus.GAME_OVER) {
            soundManager.play(SoundEffect.GAME_OVER)
            finish()
        }
    }

    /** After a life is lost, re-arm the ball on the paddle and wait for a relaunch. */
    fun rearmAfterLifeLost() {
        _uiState.update { it.copy(world = BrickBreaker.rearm(it.world)) }
    }

    /** Builds the next level, carrying over score and lives. */
    fun nextLevel() {
        resultRecorded = false
        startedAt = System.currentTimeMillis()
        _uiState.update { it.copy(world = BrickBreaker.nextLevel(it.world)) }
    }

    /** Records progress if the player leaves before the game finishes on its own. */
    fun recordIfNeeded() {
        if (!resultRecorded && world.score > 0) finish()
    }

    private fun finish() {
        if (resultRecorded) return
        resultRecorded = true
        val w = world
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.BRICK_BREAKER,
                    score = w.score.toLong(),
                    won = w.status == BrickStatus.LEVEL_CLEARED,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = duration,
                    completed = true,
                    metrics = mapOf("level" to w.level.toLong()),
                ),
            )
        }
    }
}
