package com.miniplay.app.games.numberpuzzle

import kotlin.random.Random

/**
 * Pure sliding-puzzle (n² − 1) logic. The board is a flat list of length n*n:
 * values 1..n²−1 are tiles, 0 is the blank. The goal is 1,2,…,n²−1,0.
 *
 * Shuffling applies random legal moves from the solved state, which guarantees
 * solvability by construction (every move is reversible). [isSolvable] is also
 * provided for tests and for validating externally supplied boards.
 */
data class PuzzleState(
    val size: Int,
    val tiles: List<Int>,
    val moves: Int = 0,
) {
    val blankIndex: Int get() = tiles.indexOf(0)
    val isSolved: Boolean get() = PuzzleEngine.isSolved(tiles)
}

object PuzzleEngine {

    fun solved(size: Int): List<Int> = (1 until size * size).toList() + 0

    fun isSolved(tiles: List<Int>): Boolean {
        for (i in 0 until tiles.size - 1) if (tiles[i] != i + 1) return false
        return tiles.last() == 0
    }

    fun newSolved(size: Int): PuzzleState = PuzzleState(size, solved(size))

    /** Indices orthogonally adjacent to [index] on an [size]×[size] grid. */
    fun neighbors(index: Int, size: Int): List<Int> {
        val row = index / size
        val col = index % size
        val result = ArrayList<Int>(4)
        if (row > 0) result += index - size
        if (row < size - 1) result += index + size
        if (col > 0) result += index - 1
        if (col < size - 1) result += index + 1
        return result
    }

    fun canMove(state: PuzzleState, index: Int): Boolean =
        index in state.tiles.indices && index != state.blankIndex &&
            state.blankIndex in neighbors(index, state.size)

    /** Slides the tile at [index] into the blank if adjacent; otherwise unchanged. */
    fun move(state: PuzzleState, index: Int): PuzzleState {
        if (!canMove(state, index)) return state
        val newTiles = state.tiles.toMutableList()
        val blank = state.blankIndex
        newTiles[blank] = newTiles[index]
        newTiles[index] = 0
        return state.copy(tiles = newTiles, moves = state.moves + 1)
    }

    /**
     * Produces a shuffled, solvable, non-trivial board by walking [steps] random
     * legal moves from solved. Move counter resets to 0.
     */
    fun shuffle(size: Int, random: Random = Random.Default, steps: Int = 0): PuzzleState {
        val walkSteps = if (steps > 0) steps else size * size * 40
        var tiles = solved(size)
        var blank = tiles.indexOf(0)
        var previousBlank = -1
        repeat(walkSteps) {
            val options = neighbors(blank, size).filter { it != previousBlank }
            val pick = options.random(random)
            tiles = tiles.toMutableList().also {
                it[blank] = it[pick]
                it[pick] = 0
            }
            previousBlank = blank
            blank = pick
        }
        // Avoid handing back an already-solved board.
        if (isSolved(tiles)) {
            val a = (1 until size * size).first()
            val b = a + 1
            tiles = tiles.toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }
        }
        return PuzzleState(size = size, tiles = tiles, moves = 0)
    }

    /** Standard inversion-parity solvability test. */
    fun isSolvable(tiles: List<Int>, size: Int): Boolean {
        val flat = tiles.filter { it != 0 }
        var inversions = 0
        for (i in flat.indices) for (j in i + 1 until flat.size) {
            if (flat[i] > flat[j]) inversions++
        }
        if (size % 2 == 1) return inversions % 2 == 0
        // Even width: solvable iff inversions + blank-row-from-bottom (1-indexed) is odd.
        val blankRowFromBottom = size - (tiles.indexOf(0) / size)
        return (inversions + blankRowFromBottom) % 2 == 1
    }
}
