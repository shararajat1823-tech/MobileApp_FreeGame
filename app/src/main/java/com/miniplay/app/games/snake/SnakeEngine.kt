package com.miniplay.app.games.snake

import kotlin.random.Random

/** A board cell. */
data class Cell(val x: Int, val y: Int)

enum class SnakeDir(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    fun isOpposite(other: SnakeDir): Boolean = dx == -other.dx && dy == -other.dy
}

enum class SnakeStatus { RUNNING, GAME_OVER }

/** Food varieties. Golden is the "timed special" and expires if not eaten. */
enum class FoodType(val points: Int) { NORMAL(10), BONUS(25), GOLDEN(50) }

/** The single piece of food on the board, with its type and optional expiry tick. */
data class Food(
    val cell: Cell,
    val type: FoodType,
    val expiresAtTick: Int? = null,
) {
    fun isTimed(): Boolean = expiresAtTick != null
}

/**
 * Board rules that vary by game mode: size, whether walls wrap, and static
 * obstacle cells that end the game on contact.
 */
data class SnakeConfig(
    val cols: Int = SnakeEngine.DEFAULT_COLS,
    val rows: Int = SnakeEngine.DEFAULT_ROWS,
    val wrap: Boolean = false,
    val obstacles: Set<Cell> = emptySet(),
)

/**
 * Immutable snake game state. [snake] is head-first (index 0 is the head).
 * [nextDir] is the direction queued for the next [SnakeEngine.step], so a single
 * tick can never reverse the snake into itself. [tick] drives timed-food expiry.
 */
data class SnakeState(
    val config: SnakeConfig,
    val snake: List<Cell>,
    val dir: SnakeDir,
    val nextDir: SnakeDir,
    val food: Food,
    val score: Int = 0,
    val status: SnakeStatus = SnakeStatus.RUNNING,
    val tick: Int = 0,
) {
    val cols: Int get() = config.cols
    val rows: Int get() = config.rows
    val head: Cell get() = snake.first()
    val length: Int get() = snake.size
}

/**
 * Pure Snake rules — movement, growth, wall/wrap/obstacle/self collision,
 * weighted food spawning (incl. a timed special), and scoring. No Android, and
 * randomness only via the passed [Random], so every rule is unit-testable.
 */
object SnakeEngine {

    const val DEFAULT_COLS = 17
    const val DEFAULT_ROWS = 17
    const val GOLDEN_LIFETIME_TICKS = 28

    fun newGame(config: SnakeConfig = SnakeConfig(), random: Random = Random.Default): SnakeState {
        val cx = config.cols / 2
        val cy = config.rows / 2
        // Length-3 snake heading right, in the middle of the board.
        val snake = listOf(Cell(cx, cy), Cell(cx - 1, cy), Cell(cx - 2, cy))
        val occupied = snake.toHashSet().apply { addAll(config.obstacles) }
        val cell = randomFreeCell(config.cols, config.rows, occupied, random) ?: Cell(0, 0)
        return SnakeState(
            config = config,
            snake = snake,
            dir = SnakeDir.RIGHT,
            nextDir = SnakeDir.RIGHT,
            // First food is always a plain one, so the opening is fair.
            food = Food(cell, FoodType.NORMAL),
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
        val newTick = state.tick + 1

        var nx = state.head.x + dir.dx
        var ny = state.head.y + dir.dy
        if (state.config.wrap) {
            nx = (nx + state.cols) % state.cols
            ny = (ny + state.rows) % state.rows
        } else if (nx < 0 || ny < 0 || nx >= state.cols || ny >= state.rows) {
            return state.copy(dir = dir, status = SnakeStatus.GAME_OVER, tick = newTick)
        }
        val newHead = Cell(nx, ny)

        // Obstacle collision.
        if (newHead in state.config.obstacles) {
            return state.copy(dir = dir, status = SnakeStatus.GAME_OVER, tick = newTick)
        }

        val eating = newHead == state.food.cell
        // The cells the body will occupy after moving (the tail vacates unless we grow).
        val bodyAfterMove = if (eating) state.snake else state.snake.dropLast(1)
        if (newHead in bodyAfterMove) {
            return state.copy(dir = dir, status = SnakeStatus.GAME_OVER, tick = newTick)
        }

        val newSnake = ArrayList<Cell>(bodyAfterMove.size + 1)
        newSnake.add(newHead)
        newSnake.addAll(bodyAfterMove)

        if (!eating) {
            // Timed food vanishes if the player did not reach it in time.
            val expired = state.food.expiresAtTick?.let { newTick >= it } == true
            val food = if (expired) {
                spawnFood(newSnake, state.config, newTick, random) ?: state.food
            } else {
                state.food
            }
            return state.copy(snake = newSnake, dir = dir, food = food, tick = newTick)
        }

        // Ate food: score by type, and spawn the next piece (or end if the board is full).
        val nextFood = spawnFood(newSnake, state.config, newTick, random)
        return state.copy(
            snake = newSnake,
            dir = dir,
            score = state.score + state.food.type.points,
            food = nextFood ?: state.food,
            status = if (nextFood == null) SnakeStatus.GAME_OVER else SnakeStatus.RUNNING,
            tick = newTick,
        )
    }

    /** Picks a free cell and a weighted food type (Golden becomes timed). */
    fun spawnFood(snake: List<Cell>, config: SnakeConfig, tick: Int, random: Random): Food? {
        val occupied = snake.toHashSet().apply { addAll(config.obstacles) }
        val cell = randomFreeCell(config.cols, config.rows, occupied, random) ?: return null
        val roll = random.nextInt(100)
        val type = when {
            roll < 76 -> FoodType.NORMAL
            roll < 93 -> FoodType.BONUS
            else -> FoodType.GOLDEN
        }
        val expiry = if (type == FoodType.GOLDEN) tick + GOLDEN_LIFETIME_TICKS else null
        return Food(cell, type, expiry)
    }

    private fun randomFreeCell(cols: Int, rows: Int, occupied: Set<Cell>, random: Random): Cell? {
        val free = ArrayList<Cell>(cols * rows - occupied.size)
        for (y in 0 until rows) for (x in 0 until cols) {
            val c = Cell(x, y)
            if (c !in occupied) free.add(c)
        }
        return if (free.isEmpty()) null else free[random.nextInt(free.size)]
    }
}
