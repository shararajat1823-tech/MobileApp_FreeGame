package com.miniplay.app.games.ludo

import com.miniplay.app.domain.model.GameDifficulty
import kotlin.random.Random

/**
 * Computer opponent. It chooses among the engine's own [LudoState.legalMoves],
 * so a bot can never make an illegal move, and it only sees the current state —
 * never future dice. Difficulty scales how much look-ahead/threat analysis it does.
 */
object LudoBot {

    /** Picks a legal token index for the current player. Returns -1 if none (shouldn't happen in AWAIT_MOVE). */
    fun chooseMove(state: LudoState, difficulty: GameDifficulty, random: Random = Random.Default): Int {
        val moves = state.legalMoves
        if (moves.isEmpty()) return -1
        if (moves.size == 1) return moves.first()
        return when (difficulty) {
            GameDifficulty.EASY -> moves[random.nextInt(moves.size)]
            GameDifficulty.MEDIUM -> moves.maxBy { scoreMedium(state, it) }
            GameDifficulty.HARD -> moves.maxBy { scoreHard(state, it) }
        }
    }

    private fun scoreMedium(state: LudoState, tokenIndex: Int): Double {
        val color = state.currentColor
        val die = state.dice ?: 0
        val from = state.tokens.getValue(color)[tokenIndex]
        val to = if (from < 0) 0 else from + die
        var s = 0.0
        if (from < 0) s += 50.0                         // bring a token out
        if (to == LudoBoard.FINISH) s += 100.0          // finish a token
        if (to in 0..LudoBoard.LAST_RING && LudoBoard.ringIndex(color, to) in LudoBoard.safeRingIndices) s += 20.0
        if (willCapture(state, color, to)) s += 40.0
        s += to.coerceAtLeast(0) * 0.5                  // general progress
        return s
    }

    private fun scoreHard(state: LudoState, tokenIndex: Int): Double {
        val color = state.currentColor
        val die = state.dice ?: 0
        val from = state.tokens.getValue(color)[tokenIndex]
        val to = if (from < 0) 0 else from + die
        var s = 0.0

        if (to == LudoBoard.FINISH) s += 120.0
        else if (to in 51..55) s += 60.0 + (to - 50) * 3  // advancing up the safe home column
        if (from < 0) s += 45.0
        if (willCapture(state, color, to)) s += 70.0

        val landingSafe = to in 0..LudoBoard.LAST_RING &&
            LudoBoard.ringIndex(color, to) in LudoBoard.safeRingIndices
        if (landingSafe || to >= 51) s += 25.0

        // Escape danger: reward leaving a square an opponent could hit next.
        if (from in 0..LudoBoard.LAST_RING && underThreat(state, color, from)) s += 30.0
        // Penalise moving INTO danger (unless safe or it captures).
        if (!landingSafe && to in 0..LudoBoard.LAST_RING &&
            underThreat(state, color, to) && !willCapture(state, color, to)
        ) {
            s -= 35.0
        }
        s += to.coerceAtLeast(0) * 0.8
        return s
    }

    /** Would a token of [color] landing at [to] capture at least one opponent? */
    private fun willCapture(state: LudoState, color: LudoColor, to: Int): Boolean {
        if (to !in 0..LudoBoard.LAST_RING) return false
        val abs = LudoBoard.ringIndex(color, to)
        if (abs in LudoBoard.safeRingIndices) return false
        for (other in state.players) {
            if (other == color) continue
            for (op in state.tokens.getValue(other)) {
                if (op in 0..LudoBoard.LAST_RING && LudoBoard.ringIndex(other, op) == abs) return true
            }
        }
        return false
    }

    /** Could any opponent capture a [color] token standing at ring [progress] with a 1..6 roll? */
    private fun underThreat(state: LudoState, color: LudoColor, progress: Int): Boolean {
        if (progress !in 0..LudoBoard.LAST_RING) return false
        val myAbs = LudoBoard.ringIndex(color, progress)
        if (myAbs in LudoBoard.safeRingIndices) return false
        for (other in state.players) {
            if (other == color) continue
            for (op in state.tokens.getValue(other)) {
                if (op !in 0..LudoBoard.LAST_RING) continue
                val opAbs = LudoBoard.ringIndex(other, op)
                // Distance ahead for the opponent to reach my square (ring is forward-only).
                val dist = Math.floorMod(myAbs - opAbs, LudoBoard.RING)
                if (dist in 1..6) return true
            }
        }
        return false
    }
}
