package com.miniplay.app.games.snake

/** The playable Snake modes. Each maps to a board [SnakeConfig] and a speed/time rule. */
enum class SnakeMode { CLASSIC, ZEN, TIME_ATTACK, OBSTACLE }

/**
 * Per-mode rules kept out of the engine so the pure engine stays generic: board
 * config (wrap/obstacles), tick speed curve, and any time limit.
 */
object SnakeModes {

    const val COLS = 17
    const val ROWS = 17

    /** A scattered obstacle layout that never blocks the central spawn/first moves. */
    val obstacles: Set<Cell> = buildSet {
        addAll(listOf(Cell(4, 3), Cell(5, 3), Cell(11, 13), Cell(12, 13)))
        addAll(listOf(Cell(12, 3), Cell(11, 3), Cell(4, 13), Cell(5, 13)))
        addAll(listOf(Cell(3, 8), Cell(13, 8)))
        addAll(listOf(Cell(8, 3), Cell(8, 13)))
    }

    fun config(mode: SnakeMode): SnakeConfig = when (mode) {
        SnakeMode.CLASSIC -> SnakeConfig(COLS, ROWS)
        SnakeMode.ZEN -> SnakeConfig(COLS, ROWS, wrap = true)
        SnakeMode.TIME_ATTACK -> SnakeConfig(COLS, ROWS)
        SnakeMode.OBSTACLE -> SnakeConfig(COLS, ROWS, obstacles = obstacles)
    }

    fun timeLimitMillis(mode: SnakeMode): Long? =
        if (mode == SnakeMode.TIME_ATTACK) 60_000L else null

    private fun accelerates(mode: SnakeMode): Boolean = mode != SnakeMode.ZEN

    fun intervalMillis(mode: SnakeMode, length: Int): Long {
        val base = when (mode) {
            SnakeMode.CLASSIC -> 200L
            SnakeMode.ZEN -> 185L
            SnakeMode.TIME_ATTACK -> 170L
            SnakeMode.OBSTACLE -> 195L
        }
        if (!accelerates(mode)) return base
        val min = 80L
        return (base - (length - 3) * 4L).coerceAtLeast(min)
    }
}
