package com.miniplay.app.games.tictactoe

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TicTacToeMode { TWO_PLAYER, VS_COMPUTER }

data class TicTacToeUiState(
    val game: TicTacToeState = TicTacToeEngine.newGame(),
    val mode: TicTacToeMode = TicTacToeMode.VS_COMPUTER,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0,
    val aiThinking: Boolean = false,
) {
    val showResult: Boolean get() = game.isFinished
}

/**
 * Drives Tic-Tac-Toe. The human is always X and moves first; the computer is O.
 * All rules come from [TicTacToeEngine]/[TicTacToeAi]; this class only sequences
 * turns, schedules the AI's "thinking" delay, keeps the scoreboard and reports a
 * [GameResult] once per finished game.
 */
class TicTacToeViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicTacToeUiState())
    val uiState = _uiState.asStateFlow()

    private var gameStartedAt = System.currentTimeMillis()
    private var resultRecorded = false

    fun onCellClick(index: Int) {
        val state = _uiState.value
        if (state.game.isFinished || state.aiThinking) return
        // In single-player it must be the human's (X) turn.
        if (state.mode == TicTacToeMode.VS_COMPUTER && state.game.currentPlayer != Player.X) return
        if (!state.game.isEmptyAt(index)) return

        soundManager.play(SoundEffect.CLICK)
        hapticManager.perform(HapticFeedbackType.TICK)

        val afterMove = TicTacToeEngine.play(state.game, index)
        _uiState.update { it.copy(game = afterMove) }
        afterOptionallyFinish(afterMove)

        if (!afterMove.isFinished && _uiState.value.mode == TicTacToeMode.VS_COMPUTER) {
            scheduleComputerMove()
        }
    }

    private fun scheduleComputerMove() {
        _uiState.update { it.copy(aiThinking = true) }
        viewModelScope.launch {
            delay(COMPUTER_THINK_MILLIS)
            val state = _uiState.value
            if (state.game.isFinished) {
                _uiState.update { it.copy(aiThinking = false) }
                return@launch
            }
            val move = TicTacToeAi.chooseMove(state.game.board, Player.O, state.difficulty)
            val afterAi = TicTacToeEngine.play(state.game, move)
            soundManager.play(SoundEffect.CLICK)
            _uiState.update { it.copy(game = afterAi, aiThinking = false) }
            afterOptionallyFinish(afterAi)
        }
    }

    private fun afterOptionallyFinish(game: TicTacToeState) {
        when (val status = game.status) {
            is MatchStatus.Win -> {
                _uiState.update {
                    if (status.player == Player.X) it.copy(xWins = it.xWins + 1)
                    else it.copy(oWins = it.oWins + 1)
                }
                soundManager.play(if (isHumanWin(status.player)) SoundEffect.WIN else SoundEffect.GAME_OVER)
                hapticManager.perform(if (isHumanWin(status.player)) HapticFeedbackType.SUCCESS else HapticFeedbackType.ERROR)
                recordResult(won = resolveWon(status.player))
            }
            MatchStatus.Draw -> {
                _uiState.update { it.copy(draws = it.draws + 1) }
                soundManager.play(SoundEffect.GAME_OVER)
                hapticManager.perform(HapticFeedbackType.MEDIUM)
                recordResult(won = if (currentMode() == TicTacToeMode.VS_COMPUTER) false else null)
            }
            MatchStatus.InProgress -> Unit
        }
    }

    private fun isHumanWin(winner: Player): Boolean =
        currentMode() == TicTacToeMode.VS_COMPUTER && winner == Player.X

    /** In single-player a win counts only when the human (X) wins; PvP is unscored. */
    private fun resolveWon(winner: Player): Boolean? = when (currentMode()) {
        TicTacToeMode.VS_COMPUTER -> winner == Player.X
        TicTacToeMode.TWO_PLAYER -> null
    }

    private fun recordResult(won: Boolean?) {
        if (resultRecorded) return
        resultRecorded = true
        val duration = System.currentTimeMillis() - gameStartedAt
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.TIC_TAC_TOE,
                    score = 0,
                    won = won,
                    difficulty = _uiState.value.difficulty,
                    durationMillis = duration,
                    completed = true,
                ),
            )
        }
    }

    fun newGame() {
        resultRecorded = false
        gameStartedAt = System.currentTimeMillis()
        _uiState.update { it.copy(game = TicTacToeEngine.newGame(), aiThinking = false) }
    }

    fun setMode(mode: TicTacToeMode) {
        _uiState.update { it.copy(mode = mode) }
        newGame()
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        newGame()
    }

    fun resetScores() {
        _uiState.update { it.copy(xWins = 0, oWins = 0, draws = 0) }
    }

    private fun currentMode() = _uiState.value.mode

    private companion object {
        const val COMPUTER_THINK_MILLIS = 450L
    }
}
