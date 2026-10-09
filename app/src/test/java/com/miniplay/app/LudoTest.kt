package com.miniplay.app

import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.games.ludo.Dice
import com.miniplay.app.games.ludo.LudoBoard
import com.miniplay.app.games.ludo.LudoBot
import com.miniplay.app.games.ludo.LudoColor
import com.miniplay.app.games.ludo.LudoEngine
import com.miniplay.app.games.ludo.LudoPhase
import com.miniplay.app.games.ludo.LudoRules
import com.miniplay.app.games.ludo.LudoSerialization
import com.miniplay.app.games.ludo.LudoState
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Geometry correctness: the shared ring and every colour's path. */
class LudoGeometryTest {

    @Test fun ringHas52UniqueInBoundsCells() {
        assertEquals(52, LudoBoard.ring.size)
        assertEquals(52, LudoBoard.ring.toSet().size, "ring cells must be unique")
        LudoBoard.ring.forEach {
            assertTrue(it.x in 0 until LudoBoard.GRID && it.y in 0 until LudoBoard.GRID, "in bounds: $it")
        }
    }

    @Test fun ringIsContinuousWithExactlyFourDiagonalShoulders() {
        var diagonals = 0
        for (i in LudoBoard.ring.indices) {
            val a = LudoBoard.ring[i]
            val b = LudoBoard.ring[(i + 1) % LudoBoard.ring.size]
            val dx = abs(a.x - b.x)
            val dy = abs(a.y - b.y)
            val orthogonal = (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
            val diagonal = dx == 1 && dy == 1
            assertTrue(orthogonal || diagonal, "consecutive ring cells must touch: $a -> $b")
            if (diagonal) diagonals++
        }
        assertEquals(4, diagonals, "a Ludo ring turns four corners")
    }

    @Test fun fourStartsSpacedThirteenApartAndSafe() {
        val starts = LudoColor.entries.map { LudoBoard.startOffset.getValue(it) }.sorted()
        assertEquals(listOf(8, 21, 34, 47), starts)
        starts.forEach { assertTrue(it in LudoBoard.safeRingIndices, "start $it must be safe") }
    }

    @Test fun eachColorHasFiveHomeColumnCellsAttachedToTheRing() {
        for (color in LudoColor.entries) {
            val col = LudoBoard.homeColumn.getValue(color)
            assertEquals(5, col.size)
            // The last ring cell (progress 50) must be orthogonally adjacent to the first home cell.
            val lastRing = LudoBoard.cellFor(color, LudoBoard.LAST_RING)
            val firstHome = col.first()
            val d = abs(lastRing.x - firstHome.x) + abs(lastRing.y - firstHome.y)
            assertEquals(1, d, "$color home column must connect to the ring")
            // Home column marches toward the centre, one step at a time.
            for (i in 1 until col.size) {
                val step = abs(col[i].x - col[i - 1].x) + abs(col[i].y - col[i - 1].y)
                assertEquals(1, step, "$color home column must be contiguous")
            }
        }
    }

    @Test fun everyColorPathIs57DistinctCellsFromStartToHome() {
        for (color in LudoColor.entries) {
            val cells = (0..LudoBoard.FINISH - 1).map { LudoBoard.cellFor(color, it) }
            assertEquals(56, cells.size)
            assertEquals(56, cells.toSet().size, "$color path cells must be distinct")
        }
    }

    @Test fun eightSafeSquares() {
        assertEquals(8, LudoBoard.safeRingIndices.size)
    }
}

/** Core rules. */
class LudoEngineTest {

    private fun game(vararg colors: LudoColor, rules: LudoRules = LudoRules()) =
        LudoEngine.newGame(colors.toList(), rules)

    @Test fun newGameGivesEveryPlayerFourTokensInYard() {
        val s = game(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        for (c in s.players) {
            assertEquals(4, s.tokens.getValue(c).size)
            assertTrue(s.tokens.getValue(c).all { it == -1 })
        }
        assertEquals(LudoPhase.AWAIT_ROLL, s.phase)
    }

    @Test fun diceValuesAlwaysOneToSix() {
        val dice = Dice(Random(1))
        repeat(5000) {
            val v = dice.roll()
            assertTrue(v in 1..6, "die out of range: $v")
        }
    }

    @Test fun tokenCannotLeaveYardWithoutASix() {
        val s = game(LudoColor.RED, LudoColor.GREEN)
        for (v in 1..5) {
            val rolled = LudoEngine.roll(s, v)
            // No legal moves (all in yard, not a 6) -> turn passed to next player.
            assertTrue(rolled.legalMoves.isEmpty())
            assertEquals(1, rolled.current)
        }
        val six = LudoEngine.roll(s, 6)
        assertEquals(listOf(0, 1, 2, 3), six.legalMoves)
        assertEquals(LudoPhase.AWAIT_MOVE, six.phase)
    }

    @Test fun enteringBoardLandsOnTheStartSquare() {
        val s = game(LudoColor.RED, LudoColor.GREEN)
        val afterRoll = LudoEngine.roll(s, 6)
        val afterMove = LudoEngine.applyMove(afterRoll, 0)
        assertEquals(0, afterMove.tokens.getValue(LudoColor.RED)[0])
        val cell = LudoBoard.cellFor(LudoColor.RED, 0)
        assertEquals(LudoBoard.ring[LudoBoard.startOffset.getValue(LudoColor.RED)], cell)
    }

    @Test fun movementUsesExactDiceValue() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(0, -1, -1, -1)) })
        val rolled = LudoEngine.roll(s, 4)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(4, moved.tokens.getValue(LudoColor.RED)[0])
    }

    @Test fun illegalTokenTapIsRejected() {
        val s = game(LudoColor.RED, LudoColor.GREEN)
        val rolled = LudoEngine.roll(s, 3) // all in yard, no legal move -> passes
        // Trying to move anyway is a no-op (phase is AWAIT_ROLL for next player).
        val after = LudoEngine.applyMove(rolled, 0)
        assertEquals(rolled, after)
    }

    @Test fun cannotMovePastHome() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        // RED token two steps short of home (progress 54).
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(54, -1, -1, -1)) })
        val rollTooBig = LudoEngine.roll(s, 5) // 54+5=59 > 56 -> illegal, no moves -> pass
        assertTrue(rollTooBig.legalMoves.isEmpty())
        val rollExact = LudoEngine.roll(s, 2)  // 54+2 = 56 exact -> legal
        assertEquals(listOf(0), rollExact.legalMoves)
        val finished = LudoEngine.applyMove(rollExact, 0)
        assertEquals(LudoBoard.FINISH, finished.tokens.getValue(LudoColor.RED)[0])
    }

    @Test fun captureSendsOpponentHomeAndGrantsExtraTurn() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        // Place a GREEN token on an UNSAFE ring square, and RED three behind it (same absolute cell reachable).
        // GREEN start offset 8; put green token at green-progress so its absolute index is a non-safe cell.
        // Choose absolute ring index 10 (not safe). GREEN progress = (10 - 8) = 2.
        val greenAbsTarget = 10
        assertFalse(greenAbsTarget in LudoBoard.safeRingIndices)
        val greenProg = Math.floorMod(greenAbsTarget - LudoBoard.startOffset.getValue(LudoColor.GREEN), 52)
        // RED needs red-progress p with (47 + p) % 52 == 10 -> p = 15. Put RED three behind at p=12, roll 3.
        val redProg = Math.floorMod(greenAbsTarget - LudoBoard.startOffset.getValue(LudoColor.RED), 52)
        s = s.copy(
            tokens = s.tokens.toMutableMap().apply {
                put(LudoColor.RED, listOf(redProg - 3, -1, -1, -1))
                put(LudoColor.GREEN, listOf(greenProg, -1, -1, -1))
            },
        )
        val rolled = LudoEngine.roll(s, 3)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(-1, moved.tokens.getValue(LudoColor.GREEN)[0], "captured token returns to yard")
        assertEquals(1, moved.lastEvent?.captured?.size)
        assertEquals(LudoColor.RED, moved.currentColor, "capture grants an extra turn (same player)")
        assertEquals(LudoPhase.AWAIT_ROLL, moved.phase)
    }

    @Test fun safeSquarePreventsCapture() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        // Land RED on a safe star square occupied by GREEN -> no capture.
        val safeAbs = 16 // a star safe square
        assertTrue(safeAbs in LudoBoard.safeRingIndices)
        val greenProg = Math.floorMod(safeAbs - LudoBoard.startOffset.getValue(LudoColor.GREEN), 52)
        val redTargetProg = Math.floorMod(safeAbs - LudoBoard.startOffset.getValue(LudoColor.RED), 52)
        s = s.copy(
            tokens = s.tokens.toMutableMap().apply {
                put(LudoColor.RED, listOf(redTargetProg - 2, -1, -1, -1))
                put(LudoColor.GREEN, listOf(greenProg, -1, -1, -1))
            },
        )
        val rolled = LudoEngine.roll(s, 2)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(greenProg, moved.tokens.getValue(LudoColor.GREEN)[0], "token on a safe square is not captured")
        assertTrue(moved.lastEvent?.captured.isNullOrEmpty())
    }

    @Test fun rollingSixGrantsAnExtraTurn() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(3, -1, -1, -1)) })
        val rolled = LudoEngine.roll(s, 6)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(LudoColor.RED, moved.currentColor)
        assertEquals(LudoPhase.AWAIT_ROLL, moved.phase)
    }

    @Test fun threeConsecutiveSixesForfeitTheTurn() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(3, -1, -1, -1)) })
        // First six -> move -> still RED.
        s = LudoEngine.applyMove(LudoEngine.roll(s, 6), 0)
        assertEquals(LudoColor.RED, s.currentColor)
        assertEquals(1, s.consecutiveSixes)
        // Second six -> move -> still RED.
        s = LudoEngine.applyMove(LudoEngine.roll(s, 6), 0)
        assertEquals(LudoColor.RED, s.currentColor)
        assertEquals(2, s.consecutiveSixes)
        // Third six -> forfeits, turn passes, six voided.
        s = LudoEngine.roll(s, 6)
        assertEquals(LudoColor.GREEN, s.currentColor)
        assertEquals(0, s.consecutiveSixes)
    }

    @Test fun winningRequiresAllFourTokensHome() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(
            tokens = s.tokens.toMutableMap().apply {
                put(LudoColor.RED, listOf(56, 56, 56, 54)) // three home, one at 54
            },
        )
        assertNull(s.winner)
        val rolled = LudoEngine.roll(s, 2) // 54 -> 56 (exact) finishes the 4th
        val moved = LudoEngine.applyMove(rolled, 3)
        assertEquals(LudoColor.RED, moved.winner)
        assertEquals(LudoPhase.GAME_OVER, moved.phase)
        // Game is over: further rolls do nothing.
        assertEquals(moved, LudoEngine.roll(moved, 6))
    }

    @Test fun turnOrderRotatesAcrossAllPlayers() {
        var s = game(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        val seen = mutableListOf<LudoColor>()
        repeat(4) {
            seen.add(s.currentColor)
            s = LudoEngine.roll(s, 1) // no legal move for anyone in yard -> passes
        }
        assertEquals(listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE), seen)
    }

    @Test fun rapidTapsCannotMoveTwice() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(3, -1, -1, -1)) })
        val rolled = LudoEngine.roll(s, 4)
        val once = LudoEngine.applyMove(rolled, 0)
        val twice = LudoEngine.applyMove(once, 0) // second tap: phase no longer AWAIT_MOVE
        assertEquals(once, twice)
        assertEquals(7, once.tokens.getValue(LudoColor.RED)[0]) // 3 + 4, not 3 + 8
    }

    @Test fun restartProducesACleanMatch() {
        val fresh = game(LudoColor.RED, LudoColor.GREEN)
        assertTrue(fresh.players.all { c -> fresh.tokens.getValue(c).all { it == -1 } })
        assertEquals(0, fresh.current)
        assertNull(fresh.winner)
        assertEquals(LudoPhase.AWAIT_ROLL, fresh.phase)
    }

    @Test fun noLegalMoveWhenAllTokensWouldOvershoot() {
        var s = game(LudoColor.RED, LudoColor.GREEN)
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(55, 56, 56, 56)) })
        val rolled = LudoEngine.roll(s, 6) // 55 + 6 overshoots; only a 1 would work
        assertTrue(rolled.legalMoves.isEmpty())
        assertEquals(LudoColor.GREEN, rolled.currentColor)
    }
}

/** Bot behaviour. */
class LudoBotTest {

    @Test fun botAlwaysPicksALegalMove() {
        val rng = Random(42)
        for (diff in GameDifficulty.entries) {
            var s: LudoState = LudoEngine.newGame(listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE))
            repeat(400) {
                if (s.isOver) return@repeat
                val value = rng.nextInt(1, 7)
                s = LudoEngine.roll(s, value)
                if (s.phase == LudoPhase.AWAIT_MOVE) {
                    val choice = LudoBot.chooseMove(s, diff, rng)
                    assertTrue(choice in s.legalMoves, "$diff bot chose an illegal move: $choice from ${s.legalMoves}")
                    s = LudoEngine.applyMove(s, choice)
                }
            }
        }
    }

    @Test fun hardBotPrefersFinishingAToken() {
        var s = LudoEngine.newGame(listOf(LudoColor.RED, LudoColor.GREEN))
        // Token 0 can finish with a 2 (54 -> 56); token 1 can only advance on the ring.
        s = s.copy(tokens = s.tokens.toMutableMap().apply { put(LudoColor.RED, listOf(54, 10, -1, -1)) })
        val rolled = LudoEngine.roll(s, 2)
        assertNotNull(rolled.legalMoves)
        val choice = LudoBot.chooseMove(rolled, GameDifficulty.HARD, Random(0))
        assertEquals(0, choice, "hard bot should finish the token that can reach home")
    }
}

/** Saving and restoring an in-progress local match. */
class LudoSerializationTest {

    @Test fun midGameStateRoundTrips() {
        var s = LudoEngine.newGame(
            listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW),
            LudoRules(threeSixesForfeits = false, tokensToWin = 2),
        )
        s = s.copy(
            tokens = s.tokens.toMutableMap().apply {
                put(LudoColor.RED, listOf(0, 14, 56, -1))
                put(LudoColor.GREEN, listOf(-1, 7, -1, 30))
                put(LudoColor.YELLOW, listOf(55, 56, -1, 3))
            },
        )
        val roll = LudoEngine.roll(s, 4) // leaves it mid-move with legal moves set
        val restored = LudoSerialization.decode(LudoSerialization.encode(roll))
        assertNotNull(restored)
        assertEquals(roll.players, restored.players)
        assertEquals(roll.tokens, restored.tokens)
        assertEquals(roll.current, restored.current)
        assertEquals(roll.phase, restored.phase)
        assertEquals(roll.dice, restored.dice)
        assertEquals(roll.legalMoves, restored.legalMoves)
        assertEquals(roll.rules, restored.rules)
        assertEquals(roll.turnId, restored.turnId)
    }

    @Test fun garbageDecodesToNull() {
        assertNull(LudoSerialization.decode(null))
        assertNull(LudoSerialization.decode(""))
        assertNull(LudoSerialization.decode("not a real state"))
        assertNull(LudoSerialization.decode("v1\nplayers=RED\ntokens=RED:-1,-1,-1,-1\nrules=1,1,1,1,1,4"))
    }
}
