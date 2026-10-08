package com.miniplay.app

import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.games.tictactoe.MatchStatus
import com.miniplay.app.games.tictactoe.Player
import com.miniplay.app.games.tictactoe.TicTacToeAi
import com.miniplay.app.games.tictactoe.TicTacToeEngine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TicTacToeEngineTest {

    private fun boardOf(s: String): List<Player?> = s.map {
        when (it) { 'X' -> Player.X; 'O' -> Player.O; else -> null }
    }

    @Test fun detectsRowWin() {
        val status = TicTacToeEngine.statusFor(boardOf("XXX......"))
        assertTrue(status is MatchStatus.Win)
        assertEquals(Player.X, (status as MatchStatus.Win).player)
        assertEquals(listOf(0, 1, 2), status.line)
    }

    @Test fun detectsColumnWin() {
        val status = TicTacToeEngine.statusFor(boardOf("O..O..O.."))
        assertTrue(status is MatchStatus.Win)
        assertEquals(listOf(0, 3, 6), (status as MatchStatus.Win).line)
    }

    @Test fun detectsDiagonalWin() {
        val status = TicTacToeEngine.statusFor(boardOf("X...X...X"))
        assertEquals(listOf(0, 4, 8), (status as MatchStatus.Win).line)
    }

    @Test fun detectsDraw() {
        // X O X / X O O / O X X  -> full, no winner
        assertEquals(MatchStatus.Draw, TicTacToeEngine.statusFor(boardOf("XOXXOOOXX")))
    }

    @Test fun inProgressWhenNotFull() {
        assertEquals(MatchStatus.InProgress, TicTacToeEngine.statusFor(boardOf("X........")))
    }

    @Test fun playRejectsOccupiedCellAndFinishedGame() {
        val start = TicTacToeEngine.newGame()
        val afterX = TicTacToeEngine.play(start, 0)
        assertEquals(afterX, TicTacToeEngine.play(afterX, 0)) // occupied -> unchanged
        assertEquals(Player.O, afterX.currentPlayer)          // turn advanced
    }

    @Test fun playAdvancesTurnsAndSetsWinner() {
        var s = TicTacToeEngine.newGame()
        // X:0 O:3 X:1 O:4 X:2 => X wins top row
        for (m in listOf(0, 3, 1, 4, 2)) s = TicTacToeEngine.play(s, m)
        assertTrue(s.isFinished)
        assertEquals(listOf(0, 1, 2), s.winningLine)
    }

    @Test fun newGameResetsBoard() {
        val fresh = TicTacToeEngine.newGame()
        assertTrue(fresh.board.all { it == null })
        assertEquals(Player.X, fresh.currentPlayer)
        assertFalse(fresh.isFinished)
    }

    // --------------------------- AI ---------------------------

    @Test fun easyAiPicksLegalCell() {
        val board = boardOf("XOXO.....")
        val move = TicTacToeAi.chooseMove(board, Player.X, GameDifficulty.EASY, Random(1))
        assertTrue(board[move] == null)
    }

    @Test fun mediumTakesImmediateWin() {
        // X at 0,1 ; winning move is 2
        val board = boardOf("XX..O.O..")
        val move = TicTacToeAi.chooseMove(board, Player.X, GameDifficulty.MEDIUM, Random(1))
        assertEquals(2, move)
    }

    @Test fun mediumBlocksImmediateLoss() {
        // O threatens 0,1 -> AI (X) must block at 2
        val board = boardOf("OO..X....")
        val move = TicTacToeAi.chooseMove(board, Player.X, GameDifficulty.MEDIUM, Random(1))
        assertEquals(2, move)
    }

    @Test fun hardTakesCenterOrCornerOpening() {
        val empty = List<Player?>(9) { null }
        val move = TicTacToeAi.chooseMove(empty, Player.X, GameDifficulty.HARD)
        // Optimal opening is center or a corner, never an edge.
        assertTrue(move in listOf(0, 2, 4, 6, 8), "unexpected opening $move")
    }

    @Test fun hardTakesWinOverBlock() {
        // X can win at 2; O threatens at 6/3. Perfect play wins immediately.
        val board = boardOf("XX..O.O..")
        val move = TicTacToeAi.chooseMove(board, Player.X, GameDifficulty.HARD)
        assertEquals(2, move)
    }

    /**
     * The defining property of a perfect player: it never loses. We simulate many
     * full games of HARD-AI (O) vs a random opponent (X) and assert O never loses.
     */
    @Test fun hardAiNeverLoses() {
        val rng = Random(42)
        repeat(400) {
            var state = TicTacToeEngine.newGame(startingPlayer = Player.X)
            while (!state.isFinished) {
                val move = if (state.currentPlayer == Player.O) {
                    TicTacToeAi.chooseMove(state.board, Player.O, GameDifficulty.HARD)
                } else {
                    state.emptyCells().random(rng)
                }
                state = TicTacToeEngine.play(state, move)
            }
            val status = state.status
            if (status is MatchStatus.Win) {
                assertNotEquals(Player.X, status.player, "HARD AI lost — minimax is wrong")
            }
        }
    }

    @Test fun twoHardPlayersAlwaysDraw() {
        var state = TicTacToeEngine.newGame()
        while (!state.isFinished) {
            val move = TicTacToeAi.chooseMove(state.board, state.currentPlayer, GameDifficulty.HARD)
            state = TicTacToeEngine.play(state, move)
        }
        assertEquals(MatchStatus.Draw, state.status)
    }
}
