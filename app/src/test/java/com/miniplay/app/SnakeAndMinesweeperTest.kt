package com.miniplay.app

import com.miniplay.app.games.minesweeper.MineStatus
import com.miniplay.app.games.minesweeper.MinesweeperEngine
import com.miniplay.app.games.snake.Cell
import com.miniplay.app.games.snake.Food
import com.miniplay.app.games.snake.FoodType
import com.miniplay.app.games.snake.SnakeConfig
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

    private fun state(
        snake: List<Cell>,
        dir: SnakeDir,
        nextDir: SnakeDir = dir,
        config: SnakeConfig = SnakeConfig(17, 17),
        food: Food = Food(Cell(0, 0), FoodType.NORMAL),
    ) = SnakeState(config = config, snake = snake, dir = dir, nextDir = nextDir, food = food)

    @Test fun newGameHasLengthThreeAndFoodOffSnake() {
        val s = SnakeEngine.newGame(SnakeConfig(17, 17), Random(1))
        assertEquals(3, s.length)
        assertEquals(SnakeDir.RIGHT, s.dir)
        assertFalse(s.food.cell in s.snake)
        assertEquals(FoodType.NORMAL, s.food.type) // first food is always plain
        assertEquals(SnakeStatus.RUNNING, s.status)
    }

    @Test fun stepMovesHeadInDirection() {
        val s = SnakeEngine.newGame(SnakeConfig(17, 17), Random(1))
        val head = s.head
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(Cell(head.x + 1, head.y), next.head)
        assertEquals(3, next.length) // no growth without food
    }

    @Test fun wallCollisionEndsGame() {
        var s = SnakeEngine.newGame(SnakeConfig(5, 5), Random(1))
        repeat(10) { s = SnakeEngine.step(s, Random(1)) }
        assertEquals(SnakeStatus.GAME_OVER, s.status)
    }

    @Test fun wrapModePassesThroughWalls() {
        // Head at the right edge moving RIGHT should wrap to column 0, not die.
        val cfg = SnakeConfig(cols = 6, rows = 6, wrap = true)
        val s = state(
            snake = listOf(Cell(5, 2), Cell(4, 2), Cell(3, 2)),
            dir = SnakeDir.RIGHT,
            config = cfg,
            food = Food(Cell(0, 0), FoodType.NORMAL),
        )
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(SnakeStatus.RUNNING, next.status)
        assertEquals(Cell(0, 2), next.head)
    }

    @Test fun obstacleCollisionEndsGame() {
        val cfg = SnakeConfig(cols = 10, rows = 10, obstacles = setOf(Cell(6, 5)))
        val s = state(
            snake = listOf(Cell(5, 5), Cell(4, 5), Cell(3, 5)),
            dir = SnakeDir.RIGHT,
            config = cfg,
        )
        val next = SnakeEngine.step(s, Random(1))
        assertEquals(SnakeStatus.GAME_OVER, next.status)
    }

    @Test fun reversalIsIgnored() {
        val s = SnakeEngine.newGame(SnakeConfig(17, 17), Random(1)) // heading RIGHT
        val turned = SnakeEngine.turn(s, SnakeDir.LEFT) // opposite -> ignored
        assertEquals(SnakeDir.RIGHT, turned.nextDir)
    }

    @Test fun eatingNormalFoodGrowsAndScores() {
        val base = SnakeEngine.newGame(SnakeConfig(17, 17), Random(1))
        val foodAhead = Cell(base.head.x + 1, base.head.y)
        val s = base.copy(food = Food(foodAhead, FoodType.NORMAL))
        val next = SnakeEngine.step(s, Random(2))
        assertEquals(base.length + 1, next.length)
        assertEquals(FoodType.NORMAL.points, next.score)
        assertFalse(next.food.cell in next.snake)
    }

    @Test fun bonusAndGoldenFoodScoreMore() {
        val base = SnakeEngine.newGame(SnakeConfig(17, 17), Random(1))
        val ahead = Cell(base.head.x + 1, base.head.y)
        val bonus = SnakeEngine.step(base.copy(food = Food(ahead, FoodType.BONUS)), Random(2))
        assertEquals(FoodType.BONUS.points, bonus.score)
        val golden = SnakeEngine.step(base.copy(food = Food(ahead, FoodType.GOLDEN, expiresAtTick = 999)), Random(2))
        assertEquals(FoodType.GOLDEN.points, golden.score)
    }

    @Test fun timedGoldenFoodExpiresAndRespawns() {
        val base = SnakeEngine.newGame(SnakeConfig(17, 17), Random(5))
        // Golden food far from the head that expires next tick.
        val golden = Food(Cell(0, 0), FoodType.GOLDEN, expiresAtTick = base.tick + 1)
        val s = base.copy(food = golden, tick = base.tick)
        val next = SnakeEngine.step(s, Random(5))
        // Not eaten; expired -> a new (likely different) food exists and score unchanged.
        assertEquals(0, next.score)
        assertTrue(next.food.cell != golden.cell || next.food.type != FoodType.GOLDEN || next.food.expiresAtTick != golden.expiresAtTick)
    }

    @Test fun selfCollisionEndsGame() {
        val body = listOf(
            Cell(2, 2), Cell(2, 3), Cell(2, 4), Cell(3, 4), Cell(3, 3), Cell(3, 2), Cell(4, 2),
        )
        val next = SnakeEngine.step(state(body, SnakeDir.RIGHT), Random(1))
        assertEquals(SnakeStatus.GAME_OVER, next.status)
    }

    @Test fun movingIntoVacatedTailCellIsAllowed() {
        val body = listOf(Cell(5, 5), Cell(5, 6), Cell(6, 6), Cell(6, 5))
        val next = SnakeEngine.step(state(body, SnakeDir.UP, nextDir = SnakeDir.RIGHT), Random(1))
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
        val base = MinesweeperEngine.newGame(3, 3, 1)
        val s = MinesweeperEngine.withMines(base, setOf(4))
        for (i in 0..8) {
            if (i == 4) assertTrue(s.cells[i].isMine)
            else assertEquals(1, s.cells[i].adjacent, "cell $i adjacency")
        }
    }

    @Test fun floodRevealExpandsThroughZeros() {
        val base = MinesweeperEngine.newGame(5, 5, 1)
        val s = MinesweeperEngine.withMines(base, setOf(0))
        val revealed = MinesweeperEngine.reveal(s, index = 24, random = Random(1))
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
        val s = MinesweeperEngine.withMines(base, setOf(0))
        var state = s
        for (i in 1..8) state = MinesweeperEngine.reveal(state, i, Random(1))
        assertEquals(MineStatus.WON, state.status)
    }

    @Test fun flaggingBlocksRevealAndCountsDown() {
        val base = MinesweeperEngine.newGame(5, 5, 3)
        val flagged = MinesweeperEngine.toggleFlag(base, 12)
        assertTrue(flagged.cells[12].flagged)
        assertEquals(2, flagged.minesRemaining)
        val afterReveal = MinesweeperEngine.reveal(flagged, 12, Random(1))
        assertFalse(afterReveal.cells[12].revealed)
        assertEquals(3, MinesweeperEngine.toggleFlag(flagged, 12).minesRemaining)
    }

    @Test fun neighborsAtCornerAndCentre() {
        assertEquals(setOf(1, 3, 4), MinesweeperEngine.neighbors(0, 3, 3).toSet())
        assertEquals(setOf(0, 1, 2, 3, 5, 6, 7, 8), MinesweeperEngine.neighbors(4, 3, 3).toSet())
    }
}
