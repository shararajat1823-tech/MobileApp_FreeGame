package com.miniplay.app.games.tictactoe

import com.miniplay.app.domain.model.GameDifficulty
import kotlin.random.Random

/**
 * Computer opponent. Difficulty changes *how optimally* it plays:
 *
 *  - EASY: random legal move.
 *  - MEDIUM: takes an immediate win, blocks an immediate loss, otherwise random.
 *    Beatable but not silly.
 *  - HARD: full minimax — plays perfectly, so the best a human can do is draw.
 *
 * Pure and deterministic given a seeded [Random], which makes the AI fully
 * unit-testable (e.g. "HARD always blocks a fork", "HARD never loses").
 */
object TicTacToeAi {

    fun chooseMove(
        board: List<Player?>,
        aiPlayer: Player,
        difficulty: GameDifficulty,
        random: Random = Random.Default,
    ): Int {
        val empties = board.indices.filter { board[it] == null }
        require(empties.isNotEmpty()) { "No moves available" }

        return when (difficulty) {
            GameDifficulty.EASY -> empties.random(random)
            GameDifficulty.MEDIUM -> mediumMove(board, aiPlayer, empties, random)
            GameDifficulty.HARD -> bestMove(board, aiPlayer)
        }
    }

    private fun mediumMove(
        board: List<Player?>,
        aiPlayer: Player,
        empties: List<Int>,
        random: Random,
    ): Int {
        winningMove(board, aiPlayer)?.let { return it }           // win now
        winningMove(board, aiPlayer.opponent())?.let { return it } // block
        return empties.random(random)
    }

    /** The move that immediately completes a line for [player], or null. */
    private fun winningMove(board: List<Player?>, player: Player): Int? {
        for (i in board.indices) {
            if (board[i] != null) continue
            val trial = board.toMutableList().also { it[i] = player }
            if ((TicTacToeEngine.statusFor(trial) as? MatchStatus.Win)?.player == player) return i
        }
        return null
    }

    /** Optimal move via minimax with depth-aware scoring (prefers faster wins). */
    private fun bestMove(board: List<Player?>, aiPlayer: Player): Int {
        var bestScore = Int.MIN_VALUE
        var move = board.indexOfFirst { it == null }
        for (i in board.indices) {
            if (board[i] != null) continue
            val trial = board.toMutableList().also { it[i] = aiPlayer }
            val score = minimax(trial, aiPlayer, maximizing = false, depth = 1)
            if (score > bestScore) {
                bestScore = score
                move = i
            }
        }
        return move
    }

    private fun minimax(
        board: List<Player?>,
        aiPlayer: Player,
        maximizing: Boolean,
        depth: Int,
    ): Int {
        when (val status = TicTacToeEngine.statusFor(board)) {
            is MatchStatus.Win -> {
                // Win sooner / lose later: fold depth into the score.
                return if (status.player == aiPlayer) 10 - depth else depth - 10
            }
            MatchStatus.Draw -> return 0
            MatchStatus.InProgress -> Unit
        }

        val player = if (maximizing) aiPlayer else aiPlayer.opponent()
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        for (i in board.indices) {
            if (board[i] != null) continue
            val trial = board.toMutableList().also { it[i] = player }
            val score = minimax(trial, aiPlayer, !maximizing, depth + 1)
            best = if (maximizing) maxOf(best, score) else minOf(best, score)
        }
        return best
    }
}
