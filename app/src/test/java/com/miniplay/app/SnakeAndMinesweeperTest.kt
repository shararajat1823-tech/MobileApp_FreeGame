package com.miniplay.app

import com.miniplay.app.games.minesweeper.MineStatus
import com.miniplay.app.games.minesweeper.MinesweeperEngine
import com.miniplay.app.games.snake.Cell
import com.miniplay.app.games.snake.SnakeDir
import com.miniplay.app.games.snake.SnakeEngine
import com.miniplay.app.games.snake.SnakeState
import com.miniplay.app.games.snake.SnakeStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnakeEngineTest {

    @Test fun newGameHasLengthThreeAndFoodOffSnake() {
        val s = SnakeEngine.newGame(17, 17, Random(1))
        assertEquals(3, s.length)
        assertEquals(SnakeDir.RIGHT, s.dir)
        assertFalse(s.food in s.snake)
        assertEquals(SnakeStatus.RUNNING, s.status)
    }

    @Test fun stepMovesHeadInDirection() {
        val s = SnakeEngine.newGame(17, 17, Random(1))
        val head = s.head
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(Cell(head.x + 1, head.y), next.head)
        assertEquals(3, next.length) // no growth without food
    }

    @Test fun wallCollisionEndsGame() {
        // Tiny board; drive the snake straight into the right wall.
        var s = SnakeEngine.newGame(5, 5, Random(1))
        repeat(10) { s = SnakeEngine.step(s, Random(1)) }
        assertEquals(SnakeStatus.GAME_OVER, s.status)
    }

    @Test fun reversalIsIgnored() {
        val s = SnakeEngine.newGame(17, 17, Random(1)) // heading RIGHT
        val turned = SnakeEngine.turn(s, SnakeDir.LEFT) // opposite -> ignored
        assertEquals(SnakeDir.RIGHT, turned.nextDir)
    }

    @Test fun eatingFoodGrowsAndScores() {
        // Place food directly ahead of the head.
        val base = SnakeEngine.newGame(17, 17, Random(1))
        val foodAhead = Cell(base.head.x + 1, base.head.y)
        val s = base.copy(food = foodAhead)
        val next = SnakeEngine.step(s, Random(2))
        assertEquals(base.length + 1, next.length)
        assertEquals(SnakeEngine.FOOD_SCORE, next.score)
        assertFalse(next.food in next.snake)
    }

    @Test fun selfCollisionEndsGame() {
        // Head (2,2) heading RIGHT runs straight into its own mid-body cell (3,2),
        // which is NOT the tail, so it is a genuine self-collision (no reversal).
        val body = listOf(
            Cell(2, 2), Cell(2, 3), Cell(2, 4), Cell(3, 4), Cell(3, 3), Cell(3, 2), Cell(4, 2),
        )
        val s = SnakeState(
            cols = 17, rows = 17, snake = body,
            dir = SnakeDir.RIGHT, nextDir = SnakeDir.RIGHT, food = Cell(0, 0),
        )
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(SnakeStatus.GAME_OVER, next.status)
    }

    @Test fun movingIntoVacatedTailCellIsAllowed() {
        // Tail cell will move away this tick, so stepping onto it must NOT be death.
        val body = listOf(Cell(5, 5), Cell(5, 6), Cell(6, 6), Cell(6, 5))
        // Head (5,5) heading UP, turn RIGHT -> (6,5) which is the current tail; tail vacates.
        val s = SnakeState(
            cols = 17, rows = 17, snake = body,
            dir = SnakeDir.UP, nextDir = SnakeDir.RIGHT, food = Cell(0, 0),
        )
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(SnakeStatus.RUNNING, next.status)
        assertEquals(Cell(6, 5), next.head)
    }
}

class MinesweeperEngineTest {

    @Test fun firstRevealIsAlwaysSafeAndPlacesMines() {
        val game = MinesweeperEngine.newGame(9, 9, 10)
        val revealed = MinesweeperEngine.reveal(game, index = 40, random = Random(7))
        assertTrue(revealed.minesPlaced)
        assertFalse(revealed.cells[40].isMine)
        assertEquals(10, revealed.cells.count { it.isMine })
        assertTrue(revealed.cells[40].revealed)
        assertTrue(revealed.status == MineStatus.PLAYING || revealed.status == MineStatus.WON)
    }

    @Test fun adjacencyCountsAreCorrect() {
        // 3x3 with a single mine at centre (index 4): all 8 neighbours should read 1.
        val base = MinesweeperEngine.newGame(3, 3, 1)
        val s = MinesweeperEngine.withMines(base, setOf(4))
        for (i in 0..8) {
            if (i == 4) assertTrue(s.cells[i].isMine)
            else assertEquals(1, s.cells[i].adjacent, "cell $i adjacency")
        }
    }

    @Test fun floodRevealExpandsThroughZeros() {
        // 5x5 with one mine in a corner (index 0). Revealing the opposite corner
        // should flood most of the board (all connected zero-region + border numbers).
        val base = MinesweeperEngine.newGame(5, 5, 1)
        val s = MinesweeperEngine.withMines(base, setOf(0))
        val revealed = MinesweeperEngine.reveal(s, index = 24, random = Random(1))
        // Every non-mine cell is reachable, so the whole safe board clears -> WON.
        assertEquals(MineStatus.WON, revealed.status)
        assertEquals(24, revealed.cells.count { it.revealed && !it.isMine })
    }

    @Test fun revealingAMineLoses() {
        val base = MinesweeperEngine.newGame(4, 4, 1)
        val s = MinesweeperEngine.withMines(base, setOf(5))
        val next = MinesweeperEngine.reveal(s, index = 5, random = Random(1))
        assertEquals(MineStatus.LOST, next.status)
        assertTrue(next.cells[5].revealed)
    }

    @Test fun clearingAllSafeCellsWins() {
        val base = MinesweeperEngine.newGame(3, 3, 1)
        val s = MinesweeperEngine.withMines(base, setOf(0)) // mine top-left
        // Reveal every safe cell one by one.
        var state = s
        for (i in 1..8) state = MinesweeperEngine.reveal(state, i, Random(1))
        assertEquals(MineStatus.WON, state.status)
    }

    @Test fun flaggingBlocksRevealAndCountsDown() {
        val base = MinesweeperEngine.newGame(5, 5, 3)
        val flagged = MinesweeperEngine.toggleFlag(base, 12)
        assertTrue(flagged.cells[12].flagged)
        assertEquals(2, flagged.minesRemaining)
        // A flagged cell is not revealed by a tap.
        val afterReveal = MinesweeperEngine.reveal(flagged, 12, Random(1))
        assertFalse(afterReveal.cells[12].revealed)
        // Unflag restores the count.
        assertEquals(3, MinesweeperEngine.toggleFlag(flagged, 12).minesRemaining)
    }

    @Test fun neighborsAtCornerAndCentre() {
        assertEquals(setOf(1, 3, 4), MinesweeperEngine.neighbors(0, 3, 3).toSet())
        assertEquals(setOf(0, 1, 2, 3, 5, 6, 7, 8), MinesweeperEngine.neighbors(4, 3, 3).toSet())
    }
}
