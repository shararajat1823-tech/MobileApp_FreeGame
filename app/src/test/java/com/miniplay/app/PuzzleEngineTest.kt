package com.miniplay.app

import com.miniplay.app.games.numberpuzzle.PuzzleEngine
import com.miniplay.app.games.numberpuzzle.PuzzleState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PuzzleEngineTest {

    @Test fun solvedBoardIsRecognised() {
        assertTrue(PuzzleEngine.isSolved(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)))
        assertFalse(PuzzleEngine.isSolved(listOf(1, 2, 3, 4, 5, 6, 7, 0, 8)))
    }

    @Test fun legalMoveSlidesTileAndCountsMove() {
        // 3x3 solved: blank at index 8. Tile 6 (index 5) is above blank -> legal.
        val state = PuzzleEngine.newSolved(3)
        assertTrue(PuzzleEngine.canMove(state, 5))
        val moved = PuzzleEngine.move(state, 5)
        assertEquals(0, moved.tiles[5])
        assertEquals(6, moved.tiles[8])
        assertEquals(1, moved.moves)
    }

    @Test fun illegalMoveIsRejected() {
        val state = PuzzleEngine.newSolved(3)
        // Index 0 is not adjacent to blank at 8.
        assertFalse(PuzzleEngine.canMove(state, 0))
        assertEquals(state, PuzzleEngine.move(state, 0))
    }

    @Test fun movingTileThenBackReturnsToSolved() {
        val state = PuzzleEngine.newSolved(3)
        val once = PuzzleEngine.move(state, 5)
        val back = PuzzleEngine.move(once, 8) // tile now at 8 slides back
        assertTrue(back.isSolved)
        assertEquals(2, back.moves)
    }

    @Test fun shuffleIsSolvableAndNotAlreadySolved() {
        repeat(50) { seed ->
            val shuffled = PuzzleEngine.shuffle(4, Random(seed.toLong()))
            assertFalse(shuffled.isSolved, "shuffle $seed produced a solved board")
            assertTrue(
                PuzzleEngine.isSolvable(shuffled.tiles, 4),
                "shuffle $seed produced an UNSOLVABLE board",
            )
            assertEquals(0, shuffled.moves)
        }
    }

    @Test fun shuffle3x3AlwaysSolvable() {
        repeat(50) { seed ->
            val shuffled = PuzzleEngine.shuffle(3, Random(seed * 13L + 1))
            assertTrue(PuzzleEngine.isSolvable(shuffled.tiles, 3))
        }
    }

    @Test fun neighborsAtCorners() {
        assertEquals(setOf(1, 3), PuzzleEngine.neighbors(0, 3).toSet())
        assertEquals(setOf(5, 7), PuzzleEngine.neighbors(8, 3).toSet())
        assertEquals(setOf(1, 3, 5, 7), PuzzleEngine.neighbors(4, 3).toSet())
    }

    @Test fun knownSolvableAndUnsolvableParity() {
        // Classic 15-puzzle: swapping 14 and 15 from solved makes it unsolvable.
        val solved = PuzzleEngine.solved(4)
        assertTrue(PuzzleEngine.isSolvable(solved, 4))
        val swapped = solved.toMutableList().also {
            val a = it.indexOf(14); val b = it.indexOf(15)
            val t = it[a]; it[a] = it[b]; it[b] = t
        }
        assertFalse(PuzzleEngine.isSolvable(swapped, 4))
    }
}
