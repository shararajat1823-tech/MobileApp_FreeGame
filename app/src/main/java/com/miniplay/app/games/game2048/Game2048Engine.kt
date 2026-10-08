package com.miniplay.app.games.game2048

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * A single tile on the 2048 board. [id] is stable across moves for tiles that
 * merely slide, which lets the UI animate movement by id. A tile created by a
 * merge gets a fresh id and [mergedThisMove] = true (for the pop animation); a
 * tile dropped in after a move has [spawnedThisMove] = true.
 */
data class Tile(
    val id: Long,
    val value: Int,
    val row: Int,
    val col: Int,
    val mergedThisMove: Boolean = false,
    val spawnedThisMove: Boolean = false,
)

data class Game2048State(
    val size: Int,
    val tiles: List<Tile>,
    val score: Int = 0,
    val won: Boolean = false,
    val keepGoingAfterWin: Boolean = false,
    val over: Boolean = false,
) {
    fun valueGrid(): Array<IntArray> {
        val grid = Array(size) { IntArray(size) }
        tiles.forEach { grid[it.row][it.col] = it.value }
        return grid
    }

    val highestTile: Int get() = tiles.maxOfOrNull { it.value } ?: 0
}

/** Outcome of a move, before a new tile is spawned. */
data class MoveOutcome(
    val tiles: List<Tile>,
    val gained: Int,
    val moved: Boolean,
)

/**
 * Pure 2048 rules: sliding, merging, scoring, spawning, win/over detection. No
 * Android, no randomness except where a [Random] is explicitly passed, so every
 * rule is unit-testable.
 */
object Game2048Engine {

    const val WIN_VALUE = 2048
    const val DEFAULT_SIZE = 4

    fun newGame(size: Int = DEFAULT_SIZE, random: Random = Random.Default): Game2048State {
        var ids = 0L
        val tiles = ArrayList<Tile>()
        repeat(2) {
            val spot = emptyCells(tiles, size).randomOrNull(random) ?: return@repeat
            tiles += Tile(id = ids++, value = spawnValue(random), row = spot.first, col = spot.second, spawnedThisMove = true)
        }
        return Game2048State(size = size, tiles = tiles)
    }

    /** Applies [direction]; if anything moved, spawns one new tile. No-op if blocked. */
    fun move(state: Game2048State, direction: Direction, random: Random = Random.Default): Game2048State {
        if (state.over) return state

        val outcome = slide(state.tiles, state.size, direction, nextId(state.tiles))
        if (!outcome.moved) return state

        val afterSpawn = spawn(outcome.tiles, state.size, nextId(outcome.tiles), random)
        val won = state.won || afterSpawn.any { it.value >= WIN_VALUE }
        val over = !canMove(afterSpawn, state.size)

        return state.copy(
            tiles = afterSpawn,
            score = state.score + outcome.gained,
            won = won,
            over = over,
        )
    }

    /** Core slide+merge for one move. Exposed for focused tests. */
    fun slide(tiles: List<Tile>, size: Int, direction: Direction, startId: Long): MoveOutcome {
        val byCell = tiles.associateBy { it.row to it.col }
        val result = ArrayList<Tile>(tiles.size)
        var gained = 0
        var moved = false
        var idCounter = startId

        for (line in lineCoords(direction, size)) {
            // Tiles along this line, ordered from the target edge inward.
            val lineTiles = line.mapNotNull { byCell[it] }
            var writeIndex = 0
            var i = 0
            while (i < lineTiles.size) {
                val current = lineTiles[i]
                val next = lineTiles.getOrNull(i + 1)
                val (targetRow, targetCol) = line[writeIndex]

                if (next != null && next.value == current.value) {
                    // Merge current + next into a new doubled tile at the target cell.
                    val mergedValue = current.value * 2
                    result += Tile(idCounter++, mergedValue, targetRow, targetCol, mergedThisMove = true)
                    gained += mergedValue
                    moved = true
                    i += 2
                } else {
                    val placed = current.copy(row = targetRow, col = targetCol, mergedThisMove = false, spawnedThisMove = false)
                    if (placed.row != current.row || placed.col != current.col) moved = true
                    result += placed
                    i += 1
                }
                writeIndex++
            }
        }
        return MoveOutcome(result, gained, moved)
    }

    fun spawn(tiles: List<Tile>, size: Int, id: Long, random: Random): List<Tile> {
        val spot = emptyCells(tiles, size).randomOrNull(random) ?: return tiles
        return tiles + Tile(id, spawnValue(random), spot.first, spot.second, spawnedThisMove = true)
    }

    fun canMove(tiles: List<Tile>, size: Int): Boolean {
        if (tiles.size < size * size) return true
        val grid = Array(size) { IntArray(size) }
        tiles.forEach { grid[it.row][it.col] = it.value }
        for (r in 0 until size) for (c in 0 until size) {
            val v = grid[r][c]
            if (r + 1 < size && grid[r + 1][c] == v) return true
            if (c + 1 < size && grid[r][c + 1] == v) return true
        }
        return false
    }

    private fun spawnValue(random: Random): Int = if (random.nextInt(10) == 0) 4 else 2

    private fun emptyCells(tiles: List<Tile>, size: Int): List<Pair<Int, Int>> {
        val occupied = tiles.mapTo(HashSet()) { it.row to it.col }
        val cells = ArrayList<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size) {
            val cell = r to c
            if (cell !in occupied) cells += cell
        }
        return cells
    }

    private fun nextId(tiles: List<Tile>): Long = (tiles.maxOfOrNull { it.id } ?: -1L) + 1L

    /**
     * Ordered cell coordinates per line, from the edge the tiles move toward.
     * Writing survivors back at line[0], line[1], … produces the slide.
     */
    private fun lineCoords(direction: Direction, size: Int): List<List<Pair<Int, Int>>> {
        val lines = ArrayList<List<Pair<Int, Int>>>(size)
        when (direction) {
            Direction.LEFT -> for (r in 0 until size) lines += (0 until size).map { c -> r to c }
            Direction.RIGHT -> for (r in 0 until size) lines += (size - 1 downTo 0).map { c -> r to c }
            Direction.UP -> for (c in 0 until size) lines += (0 until size).map { r -> r to c }
            Direction.DOWN -> for (c in 0 until size) lines += (size - 1 downTo 0).map { r -> r to c }
        }
        return lines
    }
}
