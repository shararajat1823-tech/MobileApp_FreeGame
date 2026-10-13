package com.miniplay.app.games.minesweeper

import kotlin.random.Random

data class MineCell(
    val isMine: Boolean = false,
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
)

enum class MineStatus { READY, PLAYING, WON, LOST }

/**
 * Immutable Minesweeper board. Cells are a flat, row-major list of size
 * [rows] * [cols]. Mines are placed lazily on the first reveal so the first tap
 * is always safe.
 */
data class MinesweeperState(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val cells: List<MineCell>,
    val status: MineStatus = MineStatus.READY,
    val minesPlaced: Boolean = false,
) {
    val flagsPlaced: Int get() = cells.count { it.flagged }
    val minesRemaining: Int get() = mineCount - flagsPlaced
    val isOver: Boolean get() = status == MineStatus.WON || status == MineStatus.LOST
    fun index(row: Int, col: Int) = row * cols + col
}

/**
 * Pure Minesweeper rules: lazy first-safe mine placement, adjacency counting,
 * flood reveal of empty regions, flagging, and win/lose detection. No Android,
 * randomness only via the passed [Random], so it is fully unit-testable.
 */
object MinesweeperEngine {

    fun newGame(rows: Int, cols: Int, mineCount: Int): MinesweeperState =
        MinesweeperState(
            rows = rows,
            cols = cols,
            mineCount = mineCount.coerceAtMost(rows * cols - 1),
            cells = List(rows * cols) { MineCell() },
        )

    fun neighbors(index: Int, rows: Int, cols: Int): List<Int> {
        val r = index / cols
        val c = index % cols
        val result = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) result.add(nr * cols + nc)
        }
        return result
    }

    /** Reveals [index]. Places mines on the first reveal (never under the first tap). */
    fun reveal(state: MinesweeperState, index: Int, random: Random = Random.Default): MinesweeperState {
        if (state.isOver) return state
        val placed = if (!state.minesPlaced) placeMines(state, safeIndex = index, random = random) else state
        val cell = placed.cells[index]
        if (cell.revealed || cell.flagged) return placed

        if (cell.isMine) {
            val exposed = placed.cells.mapIndexed { i, c ->
                if (c.isMine) c.copy(revealed = true) else c
            }
            return placed.copy(cells = exposed, status = MineStatus.LOST)
        }

        val revealed = floodReveal(placed, index)
        val won = isCleared(revealed, placed.rows * placed.cols, placed.mineCount)
        return placed.copy(
            cells = revealed,
            status = if (won) MineStatus.WON else MineStatus.PLAYING,
        )
    }

    /** Toggles a flag on an unrevealed cell. */
    fun toggleFlag(state: MinesweeperState, index: Int): MinesweeperState {
        if (state.status == MineStatus.WON || state.status == MineStatus.LOST) return state
        val cell = state.cells[index]
        if (cell.revealed) return state
        val updated = state.cells.toMutableList()
        updated[index] = cell.copy(flagged = !cell.flagged)
        return state.copy(cells = updated)
    }

    /**
     * Suggests a covered cell that is guaranteed safe to reveal, or null if none
     * can be offered. It prefers a cell logically implied safe by an already
     * satisfied number (its adjacent count equals its flagged neighbours), then
     * falls back to any covered non-mine cell. A suggestion is always filtered to
     * be a non-mine, so a hint can never detonate even if the player's flags are
     * wrong. Before the first reveal it points at the centre, which is always safe.
     */
    fun hint(state: MinesweeperState): Int? {
        if (state.isOver) return null
        if (!state.minesPlaced) return state.index(state.rows / 2, state.cols / 2)

        // 1) A satisfied number proves its remaining covered neighbours are safe.
        for (i in state.cells.indices) {
            val cell = state.cells[i]
            if (!cell.revealed || cell.isMine || cell.adjacent == 0) continue
            val nbrs = neighbors(i, state.rows, state.cols)
            val flagged = nbrs.count { state.cells[it].flagged }
            if (flagged == cell.adjacent) {
                val safe = nbrs.firstOrNull {
                    val n = state.cells[it]
                    !n.revealed && !n.flagged && !n.isMine
                }
                if (safe != null) return safe
            }
        }

        // 2) Fallback: any covered, unflagged, non-mine cell is safe to reveal.
        return state.cells.indices.firstOrNull { i ->
            val c = state.cells[i]
            !c.revealed && !c.flagged && !c.isMine
        }
    }

    /** Test/explicit seam: build a PLAYING board with specific mines already placed. */
    fun withMines(state: MinesweeperState, mines: Set<Int>): MinesweeperState {
        val cells = buildCells(state.rows, state.cols, mines)
        return state.copy(cells = cells, minesPlaced = true, status = MineStatus.PLAYING)
    }

    // ----------------------------- internals -----------------------------

    private fun placeMines(state: MinesweeperState, safeIndex: Int, random: Random): MinesweeperState {
        val total = state.rows * state.cols
        val forbidden = (neighbors(safeIndex, state.rows, state.cols) + safeIndex).toHashSet()
        // If the safe pocket leaves too few cells, only protect the tapped cell.
        val candidates = (0 until total).filter {
            if (total - forbidden.size >= state.mineCount) it !in forbidden else it != safeIndex
        }
        val mines = candidates.shuffled(random).take(state.mineCount).toHashSet()
        return state.copy(
            // Preserve any flags the player placed before the first reveal.
            cells = assignMines(state.rows, state.cols, mines, state.cells),
            minesPlaced = true,
            status = MineStatus.PLAYING,
        )
    }

    private fun buildCells(rows: Int, cols: Int, mines: Set<Int>): List<MineCell> =
        assignMines(rows, cols, mines, prior = null)

    private fun assignMines(rows: Int, cols: Int, mines: Set<Int>, prior: List<MineCell>?): List<MineCell> =
        List(rows * cols) { i ->
            val flagged = prior?.get(i)?.flagged ?: false
            if (i in mines) {
                MineCell(isMine = true, flagged = flagged)
            } else {
                MineCell(adjacent = neighbors(i, rows, cols).count { it in mines }, flagged = flagged)
            }
        }

    /** Iterative flood reveal: reveals [start] and, through any 0-cells, their region. */
    private fun floodReveal(state: MinesweeperState, start: Int): List<MineCell> {
        val cells = state.cells.toMutableList()
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            val cell = cells[i]
            if (cell.revealed || cell.flagged || cell.isMine) continue
            cells[i] = cell.copy(revealed = true)
            if (cell.adjacent == 0) {
                for (n in neighbors(i, state.rows, state.cols)) {
                    if (!cells[n].revealed && !cells[n].flagged && !cells[n].isMine) stack.addLast(n)
                }
            }
        }
        return cells
    }

    private fun isCleared(cells: List<MineCell>, total: Int, mineCount: Int): Boolean {
        val revealedSafe = cells.count { it.revealed && !it.isMine }
        return revealedSafe == total - mineCount
    }
}
