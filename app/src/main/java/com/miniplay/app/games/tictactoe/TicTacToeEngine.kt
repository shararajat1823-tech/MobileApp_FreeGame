package com.miniplay.app.games.tictactoe

/**
 * Pure, Android-free Tic-Tac-Toe rules. All game logic lives here so it can be
 * unit-tested exhaustively; the ViewModel and Compose UI are thin layers on top.
 *
 * The board is a flat list of 9 cells, indexed:
 * ```
 *  0 | 1 | 2
 *  3 | 4 | 5
 *  6 | 7 | 8
 * ```
 */
enum class Player {
    X, O;

    fun opponent(): Player = if (this == X) O else X
}

sealed interface MatchStatus {
    data object InProgress : MatchStatus
    data class Win(val player: Player, val line: List<Int>) : MatchStatus
    data object Draw : MatchStatus
}

data class TicTacToeState(
    val board: List<Player?> = List(9) { null },
    val currentPlayer: Player = Player.X,
    val status: MatchStatus = MatchStatus.InProgress,
) {
    val isFinished: Boolean get() = status != MatchStatus.InProgress
    val winningLine: List<Int>? get() = (status as? MatchStatus.Win)?.line

    fun isEmptyAt(index: Int): Boolean = board[index] == null
    fun emptyCells(): List<Int> = board.indices.filter { board[it] == null }
}

object TicTacToeEngine {

    /** All eight winning triples. */
    val WIN_LINES: List<List<Int>> = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // cols
        listOf(0, 4, 8), listOf(2, 4, 6),                  // diagonals
    )

    fun newGame(startingPlayer: Player = Player.X): TicTacToeState =
        TicTacToeState(currentPlayer = startingPlayer)

    /**
     * Applies a move at [index]. Returns the same state unchanged if the move is
     * illegal (cell taken or game already over), so callers never need to guard.
     */
    fun play(state: TicTacToeState, index: Int): TicTacToeState {
        if (state.isFinished || index !in 0..8 || !state.isEmptyAt(index)) return state

        val newBoard = state.board.toMutableList().also { it[index] = state.currentPlayer }
        val status = statusFor(newBoard)
        val next = if (status == MatchStatus.InProgress) state.currentPlayer.opponent() else state.currentPlayer
        return state.copy(board = newBoard, currentPlayer = next, status = status)
    }

    /** Evaluates a raw board into a [MatchStatus]. */
    fun statusFor(board: List<Player?>): MatchStatus {
        for (line in WIN_LINES) {
            val (a, b, c) = line
            val mark = board[a]
            if (mark != null && board[b] == mark && board[c] == mark) {
                return MatchStatus.Win(mark, line)
            }
        }
        return if (board.all { it != null }) MatchStatus.Draw else MatchStatus.InProgress
    }
}
