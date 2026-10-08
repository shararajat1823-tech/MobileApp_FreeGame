package com.miniplay.app.games.snake

import kotlin.random.Random

/** A board cell. */
data class Cell(val x: Int, val y: Int)

enum class SnakeDir(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    fun isOpposite(other: SnakeDir): Boolean = dx == -other.dx && dy == -other.dy
}

enum class SnakeStatus { RUNNING, GAME_OVER }

/**
 * Immutable snake game state. [snake] is head-first (index 0 is the head). [nextDir]
 * is the direction queued by the player, applied on the next [SnakeEngine.step]
 * so a single tick can never reverse the snake into itself.
 */
data class SnakeState(
    val cols: Int,
    val rows: Int,
    val snake: List<Cell>,
    val dir: SnakeDir,
    val nextDir: SnakeDir,
    val food: Cell,
    val score: Int = 0,
    val status: SnakeStatus = SnakeStatus.RUNNING,
) {
    val head: Cell get() = snake.first()
    val length: Int get() = snake.size
}

/**
 * Pure Snake rules — movement, growth, wall/self collision, food spawning and
 * scoring. No Android, no real randomness except the explicitly passed [Random],
 * so every rule is unit-testable.
 */
object SnakeEngine {

    const val DEFAULT_COLS = 17
    const val DEFAULT_ROWS = 17
    const val FOOD_SCORE = 10

    fun newGame(
        cols: Int = DEFAULT_COLS,
        rows: Int = DEFAULT_ROWS,
        random: Random = Random.Default,
    ): SnakeState {
        val cx = cols / 2
        val cy = rows / 2
        // Length-3 snake heading right, in the middle of the board.
        val snake = listOf(Cell(cx, cy), Cell(cx - 1, cy), Cell(cx - 2, cy))
        val food = randomFreeCell(cols, rows, snake, random) ?: Cell(0, 0)
        return SnakeState(
            cols = cols,
            rows = rows,
            snake = snake,
            dir = SnakeDir.RIGHT,
            nextDir = SnakeDir.RIGHT,
            food = food,
        )
    }

    /** Queues a turn. Ignores a direct reversal (which would be instant death). */
    fun turn(state: SnakeState, dir: SnakeDir): SnakeState {
        if (state.status != SnakeStatus.RUNNING) return state
        if (state.length > 1 && dir.isOpposite(state.dir)) return state
        return state.copy(nextDir = dir)
    }

    /** Advances one step: resolve direction, move the head, handle food/collisions. */
    fun step(state: SnakeState, random: Random = Random.Default): SnakeState {
        if (state.status != SnakeStatus.RUNNING) return state

        val dir = if (state.nextDir.isOpposite(state.dir) && state.length > 1) state.dir else state.nextDir
        val newHead = Cell(state.head.x + dir.dx, state.head.y + dir.dy)

        // Wall collision.
        if (newHead.x < 0 || newHead.y < 0 || newHead.x >= state.cols || newHead.y >= state.rows) {
            return state.copy(dir = dir, status = SnakeStatus.GAME_OVER)
        }

        val eating = newHead == state.food
        // The cells the body will occupy after moving (the tail vacates unless we grow).
        val bodyAfterMove = if (eating) state.snake else state.snake.dropLast(1)
        if (newHead in bodyAfterMove) {
            return state.copy(dir = dir, status = SnakeStatus.GAME_OVER)
        }

        val newSnake = ArrayList<Cell>(bodyAfterMove.size + 1)
        newSnake.add(newHead)
        newSnake.addAll(bodyAfterMove)

        if (!eating) {
            return state.copy(snake = newSnake, dir = dir)
        }

        // Ate food: score, and spawn the next piece (or end if the board is full).
        val nextFood = randomFreeCell(state.cols, state.rows, newSnake, random)
        return state.copy(
            snake = newSnake,
            dir = dir,
            score = state.score + FOOD_SCORE,
            food = nextFood ?: state.food,
            status = if (nextFood == null) SnakeStatus.GAME_OVER else SnakeStatus.RUNNING,
        )
    }

    private fun randomFreeCell(cols: Int, rows: Int, occupied: List<Cell>, random: Random): Cell? {
        val taken = occupied.toHashSet()
        val free = ArrayList<Cell>(cols * rows - taken.size)
        for (y in 0 until rows) for (x in 0 until cols) {
            val c = Cell(x, y)
            if (c !in taken) free.add(c)
        }
        return if (free.isEmpty()) null else free[random.nextInt(free.size)]
    }
}
