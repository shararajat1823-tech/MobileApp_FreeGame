package com.miniplay.app.games.numberpuzzle

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

/**
 * UI state for the sliding Number Puzzle. [state] is the authoritative board from
 * [PuzzleEngine]; everything else is presentation (elapsed time, the persisted
 * best move-count and whether this board is solved).
 */
data class PuzzleUiState(
    val state: PuzzleState,
    val size: Int = 3,
    val elapsedMillis: Long = 0,
    val bestMoves: Long? = null,
    val solved: Boolean = false,
) {
    val moves: Int get() = state.moves
}

/**
 * Drives the Number Puzzle. All board rules live in [PuzzleEngine]; this class
 * only sequences taps, runs the play-clock, keeps the persisted best (fewest
 * moves — score direction is LOWER_IS_BETTER) and reports a [GameResult] once per
 * solve. The timer starts on the first legal move so an untouched board reads 0.
 */
class PuzzleViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val statsRepository: GameStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PuzzleUiState(state = PuzzleEngine.shuffle(DEFAULT_SIZE), size = DEFAULT_SIZE),
    )
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startedAt: Long = 0L
    private var resultRecorded = false

    init {
        viewModelScope.launch {
            statsRepository.observeStats(GameIds.NUMBER_PUZZLE).collect { stats ->
                _uiState.update { it.copy(bestMoves = stats.bestScore) }
            }
        }
    }

    fun onTileClick(index: Int) {
        val current = _uiState.value
        if (current.solved) return
        if (!PuzzleEngine.canMove(current.state, index)) return

        soundManager.play(SoundEffect.CLICK)
        hapticManager.perform(HapticFeedbackType.TICK)

        val moved = PuzzleEngine.move(current.state, index)
        _uiState.update { it.copy(state = moved) }
        startTimerIfNeeded()

        if (moved.isSolved) finish()
    }

    /** Current board size; supports 3×3 and 4×4. Changing it starts a new game. */
    fun setSize(size: Int) {
        if (size == _uiState.value.size) return
        _uiState.update { it.copy(size = size) }
        newGame()
    }

    fun newGame() {
        timerJob?.cancel()
        timerJob = null
        startedAt = 0L
        resultRecorded = false
        val size = _uiState.value.size
        _uiState.update {
            it.copy(
                state = PuzzleEngine.shuffle(size),
                elapsedMillis = 0,
                solved = false,
            )
        }
    }

    /** The Shuffle action is a fresh, solvable board at the current size. */
    fun shuffle() = newGame()

    private fun startTimerIfNeeded() {
        if (timerJob != null) return
        startedAt = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                _uiState.update { it.copy(elapsedMillis = System.currentTimeMillis() - startedAt) }
            }
        }
    }

    private fun finish() {
        val current = _uiState.value
        timerJob?.cancel()
        timerJob = null
        val finalElapsed = if (startedAt > 0L) System.currentTimeMillis() - startedAt else current.elapsedMillis
        _uiState.update { it.copy(solved = true, elapsedMillis = finalElapsed) }

        soundManager.play(SoundEffect.WIN)
        hapticManager.perform(HapticFeedbackType.SUCCESS)

        recordResult(moves = current.state.moves, size = current.size, elapsed = finalElapsed)
    }

    private fun recordResult(moves: Int, size: Int, elapsed: Long) {
        if (resultRecorded) return
        resultRecorded = true
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.NUMBER_PUZZLE,
                    score = moves.toLong(),
                    won = null,
                    difficulty = if (size == DEFAULT_SIZE) GameDifficulty.EASY else GameDifficulty.HARD,
                    durationMillis = elapsed,
                    completed = true,
                    metrics = mapOf(
                        "moves" to moves.toLong(),
                        "size" to size.toLong(),
                    ),
                ),
            )
        }
    }

    private companion object {
        const val DEFAULT_SIZE = 3
        const val TICK_MILLIS = 100L
    }
}
