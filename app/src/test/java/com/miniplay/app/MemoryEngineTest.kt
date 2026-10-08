package com.miniplay.app

import com.miniplay.app.games.memorymatch.FlipResult
import com.miniplay.app.games.memorymatch.MemoryEngine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryEngineTest {

    private val symbols = listOf("🍎", "🍊", "🍋", "🍇", "🍓", "🍐", "🥝", "🍒")

    @Test fun newGameBuildsBalancedShuffledDeck() {
        val state = MemoryEngine.newGame(pairs = 6, symbols = symbols, random = Random(1))
        assertEquals(12, state.cards.size)
        // Exactly two of each chosen symbol.
        state.cards.groupingBy { it.symbol }.eachCount().values.forEach { assertEquals(2, it) }
        assertTrue(state.cards.all { !it.faceUp && !it.matched })
    }

    @Test fun firstFlipSetsFirstPick() {
        val state = MemoryEngine.newGame(6, symbols, Random(2))
        val outcome = MemoryEngine.flip(state, 0)
        assertEquals(FlipResult.FLIPPED_FIRST, outcome.result)
        assertEquals(0, outcome.state.firstPick)
        assertTrue(outcome.state.cards[0].faceUp)
        assertEquals(0, outcome.state.moves)
    }

    @Test fun matchKeepsCardsUpAndClearsPick() {
        // Build a deterministic deck by finding two indices with the same symbol.
        val state = MemoryEngine.newGame(6, symbols, Random(3))
        val a = 0
        val b = state.cards.indices.first { it != a && state.cards[it].symbol == state.cards[a].symbol }
        val afterFirst = MemoryEngine.flip(state, a).state
        val outcome = MemoryEngine.flip(afterFirst, b)
        assertEquals(FlipResult.MATCH, outcome.result)
        assertTrue(outcome.state.cards[a].matched)
        assertTrue(outcome.state.cards[b].matched)
        assertEquals(1, outcome.state.matchedPairs)
        assertEquals(1, outcome.state.moves)
        assertNull(outcome.state.firstPick)
    }

    @Test fun mismatchFlagsPendingAndResolveFlipsBack() {
        val state = MemoryEngine.newGame(6, symbols, Random(4))
        val a = 0
        val b = state.cards.indices.first { it != a && state.cards[it].symbol != state.cards[a].symbol }
        val afterFirst = MemoryEngine.flip(state, a).state
        val outcome = MemoryEngine.flip(afterFirst, b)
        assertEquals(FlipResult.MISMATCH, outcome.result)
        assertEquals(a to b, outcome.state.pendingMismatch)
        // Further flips are blocked until resolution.
        val blocked = MemoryEngine.flip(outcome.state, 3)
        assertEquals(FlipResult.IGNORED, blocked.result)
        // Resolution flips them back down.
        val resolved = MemoryEngine.resolveMismatch(outcome.state)
        assertFalse(resolved.cards[a].faceUp)
        assertFalse(resolved.cards[b].faceUp)
        assertNull(resolved.pendingMismatch)
    }

    @Test fun flippingMatchedOrSameCardIsIgnored() {
        val state = MemoryEngine.newGame(6, symbols, Random(5))
        val afterFirst = MemoryEngine.flip(state, 0)
        // Re-flipping the same face-up card is ignored.
        assertEquals(FlipResult.IGNORED, MemoryEngine.flip(afterFirst.state, 0).result)
    }

    @Test fun completingAllPairsMarksComplete() {
        var state = MemoryEngine.newGame(2, symbols, Random(6))
        // Greedily match every pair.
        val remaining = state.cards.indices.toMutableList()
        while (!state.isComplete) {
            val first = remaining.first { !state.cards[it].matched }
            val second = state.cards.indices.first {
                it != first && !state.cards[it].matched && state.cards[it].symbol == state.cards[first].symbol
            }
            state = MemoryEngine.flip(state, first).state
            state = MemoryEngine.flip(state, second).state
        }
        assertTrue(state.isComplete)
        assertEquals(2, state.matchedPairs)
        assertTrue(state.flawless) // matched every pair with no wasted move
    }

    @Test fun scoreRewardsFewerMovesAndLessTime() {
        val fast = MemoryEngine.score(pairs = 8, moves = 8, elapsedMillis = 10_000, difficultyBonus = 100)
        val slow = MemoryEngine.score(pairs = 8, moves = 20, elapsedMillis = 60_000, difficultyBonus = 100)
        assertTrue(fast > slow)
        assertTrue(MemoryEngine.score(0, 999, 999_000, 0) >= 0) // never negative
    }
}
