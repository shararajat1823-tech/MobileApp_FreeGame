package com.miniplay.app

import com.miniplay.app.games.game2048.Direction
import com.miniplay.app.games.game2048.Game2048Engine
import com.miniplay.app.games.game2048.Game2048State
import com.miniplay.app.games.game2048.Tile
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Game2048EngineTest {

    private fun tilesFromGrid(grid: Array<IntArray>): List<Tile> {
        var id = 0L
        val tiles = ArrayList<Tile>()
        for (r in grid.indices) for (c in grid[r].indices) {
            if (grid[r][c] != 0) tiles += Tile(id++, grid[r][c], r, c)
        }
        return tiles
    }

    private fun grid(vararg rows: IntArray) = arrayOf(*rows)

    private fun row0After(dir: Direction, vararg values: Int): Pair<IntArray, Int> {
        val g = grid(intArrayOf(*values), IntArray(4), IntArray(4), IntArray(4))
        val outcome = Game2048Engine.slide(tilesFromGrid(g), 4, dir, 100)
        val out = IntArray(4)
        outcome.tiles.filter { it.row == 0 }.forEach { out[it.col] = it.value }
        return out to outcome.gained
    }

    @Test fun mergesAdjacentPair() {
        val (out, gained) = row0After(Direction.LEFT, 2, 2, 0, 0)
        assertEquals(listOf(4, 0, 0, 0), out.toList())
        assertEquals(4, gained)
    }

    @Test fun mergesWithGap() {
        val (out, gained) = row0After(Direction.LEFT, 2, 0, 2, 0)
        assertEquals(listOf(4, 0, 0, 0), out.toList())
        assertEquals(4, gained)
    }

    @Test fun mergesTwoPairsIndependently() {
        val (out, gained) = row0After(Direction.LEFT, 2, 2, 2, 2)
        assertEquals(listOf(4, 4, 0, 0), out.toList())
        assertEquals(8, gained)
    }

    @Test fun mergesOnlyLeftmostPairLeavingRemainder() {
        val (out, gained) = row0After(Direction.LEFT, 2, 2, 2, 0)
        assertEquals(listOf(4, 2, 0, 0), out.toList())
        assertEquals(4, gained)
    }

    @Test fun doesNotMergeDifferentValues() {
        val (out, gained) = row0After(Direction.LEFT, 4, 2, 2, 0)
        assertEquals(listOf(4, 4, 0, 0), out.toList())
        assertEquals(4, gained)
    }

    @Test fun slidesRight() {
        val (out, _) = row0After(Direction.RIGHT, 0, 2, 0, 2)
        assertEquals(listOf(0, 0, 0, 4), out.toList())
    }

    @Test fun verticalMergeUp() {
        val g = grid(
            intArrayOf(2, 0, 0, 0),
            intArrayOf(2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
        )
        val outcome = Game2048Engine.slide(tilesFromGrid(g), 4, Direction.UP, 100)
        val col0 = outcome.tiles.filter { it.col == 0 }
        assertEquals(1, col0.size)
        assertEquals(4, col0.first().value)
        assertEquals(0, col0.first().row)
    }

    @Test fun noMoveWhenNothingSlides() {
        val g = grid(
            intArrayOf(2, 4, 8, 16),
            intArrayOf(4, 8, 16, 32),
            intArrayOf(8, 16, 32, 64),
            intArrayOf(16, 32, 64, 128),
        )
        val outcome = Game2048Engine.slide(tilesFromGrid(g), 4, Direction.LEFT, 100)
        assertFalse(outcome.moved)
    }

    @Test fun moveIsNoOpWhenBlocked() {
        val g = grid(
            intArrayOf(2, 4, 8, 16),
            intArrayOf(4, 8, 16, 32),
            intArrayOf(8, 16, 32, 64),
            intArrayOf(16, 32, 64, 128),
        )
        val state = Game2048State(size = 4, tiles = tilesFromGrid(g))
        assertEquals(state, Game2048Engine.move(state, Direction.LEFT, Random(1)))
    }

    @Test fun moveSpawnsExactlyOneTileWhenItMoves() {
        val g = grid(
            intArrayOf(2, 2, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
        )
        val state = Game2048State(size = 4, tiles = tilesFromGrid(g))
        val next = Game2048Engine.move(state, Direction.LEFT, Random(1))
        // One merged tile (4) + one spawned tile = 2 tiles, score +4.
        assertEquals(2, next.tiles.size)
        assertEquals(4, next.score)
        assertTrue(next.tiles.any { it.spawnedThisMove })
    }

    @Test fun detectsWinAtTarget() {
        val g = grid(
            intArrayOf(1024, 1024, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
        )
        val state = Game2048State(size = 4, tiles = tilesFromGrid(g))
        val next = Game2048Engine.move(state, Direction.LEFT, Random(1))
        assertTrue(next.won)
        assertEquals(2048, next.highestTile)
    }

    @Test fun newGameHasTwoTiles() {
        val state = Game2048Engine.newGame(random = Random(7))
        assertEquals(2, state.tiles.size)
        assertTrue(state.tiles.all { it.value == 2 || it.value == 4 })
    }

    @Test fun canMoveFalseOnFullCheckerboard() {
        val g = grid(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
        )
        assertFalse(Game2048Engine.canMove(tilesFromGrid(g), 4))
    }

    @Test fun canMoveTrueWhenAdjacentEqualExists() {
        val g = grid(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 2, 4), // the two 2s are adjacent
        )
        assertTrue(Game2048Engine.canMove(tilesFromGrid(g), 4))
    }
}
