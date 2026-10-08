package com.miniplay.app.games.memorymatch

import kotlin.random.Random

data class Card(
    val id: Int,
    val symbol: String,
    val faceUp: Boolean = false,
    val matched: Boolean = false,
)

data class MemoryState(
    val cards: List<Card>,
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    /** Index of the first card of the current turn, or null if none is up. */
    val firstPick: Int? = null,
    /** Two mismatched cards awaiting flip-back (resolved after a UI delay). */
    val pendingMismatch: Pair<Int, Int>? = null,
) {
    val totalPairs: Int get() = cards.size / 2
    val isComplete: Boolean get() = matchedPairs == totalPairs
    /** No mismatch ever made == a flawless clear. */
    val flawless: Boolean get() = isComplete && moves == totalPairs
}

enum class FlipResult { IGNORED, FLIPPED_FIRST, MATCH, MISMATCH }

data class FlipOutcome(val state: MemoryState, val result: FlipResult)

/**
 * Pure Memory-Match rules: deck building, the two-card flip turn, match
 * detection, completion and scoring. The mismatch flip-back is a separate call
 * ([resolveMismatch]) so the UI can insert a "look at the cards" delay while the
 * rules stay synchronous and testable.
 */
object MemoryEngine {

    fun newGame(pairs: Int, symbols: List<String>, random: Random = Random.Default): MemoryState {
        require(symbols.size >= pairs) { "Need at least $pairs symbols, got ${symbols.size}" }
        val chosen = symbols.shuffled(random).take(pairs)
        val deck = (chosen + chosen)
            .shuffled(random)
            .mapIndexed { index, symbol -> Card(id = index, symbol = symbol) }
        return MemoryState(cards = deck)
    }

    fun flip(state: MemoryState, index: Int): FlipOutcome {
        val card = state.cards.getOrNull(index)
        val blocked = card == null ||
            card.matched ||
            card.faceUp ||
            state.pendingMismatch != null
        if (blocked) return FlipOutcome(state, FlipResult.IGNORED)

        val faceUp = state.cards.setFaceUp(index)

        val first = state.firstPick
            ?: return FlipOutcome(state.copy(cards = faceUp, firstPick = index), FlipResult.FLIPPED_FIRST)

        // Second pick: this completes a move.
        val moves = state.moves + 1
        return if (faceUp[first].symbol == faceUp[index].symbol) {
            val matched = faceUp.setMatched(first).setMatched(index)
            FlipOutcome(
                state.copy(
                    cards = matched,
                    moves = moves,
                    matchedPairs = state.matchedPairs + 1,
                    firstPick = null,
                ),
                FlipResult.MATCH,
            )
        } else {
            FlipOutcome(
                state.copy(
                    cards = faceUp,
                    moves = moves,
                    firstPick = null,
                    pendingMismatch = first to index,
                ),
                FlipResult.MISMATCH,
            )
        }
    }

    /** Flips the two mismatched cards back down. No-op if nothing is pending. */
    fun resolveMismatch(state: MemoryState): MemoryState {
        val (a, b) = state.pendingMismatch ?: return state
        val reset = state.cards.setFaceDown(a).setFaceDown(b)
        return state.copy(cards = reset, pendingMismatch = null)
    }

    /**
     * Score rewards efficiency and speed. A flawless, fast clear scores highest;
     * extra moves and elapsed time chip away at it. Never negative.
     */
    fun score(pairs: Int, moves: Int, elapsedMillis: Long, difficultyBonus: Int): Int {
        val base = pairs * 100
        val extraMoves = (moves - pairs).coerceAtLeast(0)
        val movePenalty = extraMoves * 12
        val timePenalty = (elapsedMillis / 1000L).toInt() * 2
        return (base + difficultyBonus - movePenalty - timePenalty).coerceAtLeast(0)
    }

    private fun List<Card>.setFaceUp(index: Int) =
        mapIndexed { i, c -> if (i == index) c.copy(faceUp = true) else c }

    private fun List<Card>.setFaceDown(index: Int) =
        mapIndexed { i, c -> if (i == index) c.copy(faceUp = false) else c }

    private fun List<Card>.setMatched(index: Int) =
        mapIndexed { i, c -> if (i == index) c.copy(matched = true, faceUp = true) else c }
}
