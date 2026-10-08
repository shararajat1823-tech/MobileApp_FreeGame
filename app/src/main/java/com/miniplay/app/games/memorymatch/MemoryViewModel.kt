package com.miniplay.app.games.memorymatch

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

data class MemoryUiState(
    val engineState: MemoryState,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val elapsedMillis: Long = 0L,
    val bestScore: Long? = null,
    val finalScore: Int? = null,
) {
    /** The board is cleared — show the result overlay. */
    val showResult: Boolean get() = engineState.isComplete
}

/**
 * Drives Memory Match. All rules come from [MemoryEngine]; this class only
 * sequences flips, runs the play-clock, plays feedback and reports a
 * [GameResult] once per cleared board. The mismatch flip-back is scheduled by
 * the screen (after a "look at the cards" delay) via [resolveMismatch].
 */
class MemoryViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MemoryUiState(
            engineState = MemoryEngine.newGame(pairs(GameDifficulty.EASY), SYMBOLS),
            difficulty = GameDifficulty.EASY,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var resultRecorded = false

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.MEMORY_MATCH).collect { stats ->
                _uiState.update { it.copy(bestScore = stats.bestScore) }
            }
        }
    }

    fun onCardClick(index: Int) {
        val current = _uiState.value
        if (current.engineState.isComplete) return

        val outcome = MemoryEngine.flip(current.engineState, index)
        if (outcome.result == FlipResult.IGNORED) return

        _uiState.update { it.copy(engineState = outcome.state) }

        when (outcome.result) {
            FlipResult.FLIPPED_FIRST -> {
                soundManager.play(SoundEffect.CARD_FLIP)
                hapticManager.perform(HapticFeedbackType.TICK)
                startTimerIfNeeded()
            }
            FlipResult.MATCH -> {
                soundManager.play(SoundEffect.CARD_FLIP)
                soundManager.play(SoundEffect.CORRECT)
                hapticManager.perform(HapticFeedbackType.SUCCESS)
            }
            FlipResult.MISMATCH -> {
                soundManager.play(SoundEffect.WRONG)
                hapticManager.perform(HapticFeedbackType.LIGHT)
            }
            FlipResult.IGNORED -> Unit
        }

        if (outcome.state.isComplete) finish()
    }

    /** Flips the two mismatched cards back down; called by the screen after a delay. */
    fun resolveMismatch() {
        _uiState.update { it.copy(engineState = MemoryEngine.resolveMismatch(it.engineState)) }
    }

    fun newGame() {
        stopTimer()
        resultRecorded = false
        val difficulty = _uiState.value.difficulty
        val fresh = MemoryEngine.newGame(pairs(difficulty), SYMBOLS)
        _uiState.update { it.copy(engineState = fresh, elapsedMillis = 0L, finalScore = null) }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        newGame()
    }

    private fun finish() {
        if (resultRecorded) return
        stopTimer()
        val current = _uiState.value
        val pairs = current.engineState.totalPairs
        val difficultyBonus = when (current.difficulty) {
            GameDifficulty.EASY -> 0
            GameDifficulty.MEDIUM -> 150
            GameDifficulty.HARD -> 400
        }
        val score = MemoryEngine.score(
            pairs = pairs,
            moves = current.engineState.moves,
            elapsedMillis = current.elapsedMillis,
            difficultyBonus = difficultyBonus,
        )
        _uiState.update { it.copy(finalScore = score) }
        soundManager.play(SoundEffect.WIN)
        hapticManager.perform(HapticFeedbackType.SUCCESS)
        recordResult(score)
    }

    private fun recordResult(score: Int) {
        if (resultRecorded) return
        resultRecorded = true
        val state = _uiState.value
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.MEMORY_MATCH,
                    score = score.toLong(),
                    won = null,
                    difficulty = state.difficulty,
                    durationMillis = state.elapsedMillis,
                    flawless = state.engineState.flawless,
                    completed = true,
                    metrics = mapOf("moves" to state.engineState.moves.toLong()),
                ),
            )
        }
    }

    /** The play-clock starts on the first flip and ticks until the board is cleared. */
    private fun startTimerIfNeeded() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                _uiState.update { it.copy(elapsedMillis = it.elapsedMillis + TICK_MILLIS) }
                if (_uiState.value.engineState.isComplete) break
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    companion object {
        private const val TICK_MILLIS = 100L

        private val SYMBOLS = listOf(
            "🍎", "🍊", "🍋", "🍇", "🍓", "🍐", "🥝", "🍒", "🍍",
            "🥥", "🍉", "🍑", "🫐", "🥭", "🌽", "🥕", "🍄", "🌶️",
        )

        /** Pairs (and therefore cards = pairs * 2) per difficulty. */
        fun pairs(difficulty: GameDifficulty): Int = when (difficulty) {
            GameDifficulty.EASY -> 8   // 4x4
            GameDifficulty.MEDIUM -> 10 // 5x4
            GameDifficulty.HARD -> 18   // 6x6
        }

        /** Grid columns per difficulty — used by the screen to lay out the board. */
        fun columns(difficulty: GameDifficulty): Int = when (difficulty) {
            GameDifficulty.EASY -> 4
            GameDifficulty.MEDIUM -> 4
            GameDifficulty.HARD -> 6
        }
    }
}
