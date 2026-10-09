package com.miniplay.app.games.ludo

import kotlin.random.Random

/** Configurable rule variations. The defaults are the well-known standard ruleset. */
data class LudoRules(
    /** A yard token may only enter the board on a 6. */
    val enterOnSix: Boolean = true,
    /** Rolling a 6 grants another turn. */
    val extraTurnOnSix: Boolean = true,
    /** Capturing an opponent grants another turn. */
    val extraTurnOnCapture: Boolean = true,
    /** Sending one of your tokens home grants another turn. */
    val extraTurnOnFinish: Boolean = true,
    /** Three consecutive 6s forfeits the turn and voids the third 6. */
    val threeSixesForfeits: Boolean = true,
    /** Tokens a player must bring home to win (4 = all). */
    val tokensToWin: Int = LudoBoard.TOKENS,
)

enum class LudoPhase { AWAIT_ROLL, AWAIT_MOVE, GAME_OVER }

enum class LudoEventType { NONE, MOVE, ROLL_NO_MOVE, THREE_SIXES_FORFEIT }

data class CapturedToken(val color: LudoColor, val tokenIndex: Int)

/**
 * A description of what the last [LudoEngine.roll]/[LudoEngine.applyMove]
 * produced, so the UI can animate the correct token along the correct path and
 * play the right sound without re-deriving it.
 */
data class LudoEvent(
    val type: LudoEventType = LudoEventType.NONE,
    val color: LudoColor? = null,
    val tokenIndex: Int = -1,
    val fromProgress: Int = 0,
    val toProgress: Int = 0,
    val captured: List<CapturedToken> = emptyList(),
    val finishedToken: Boolean = false,
    val enteredBoard: Boolean = false,
    val extraTurn: Boolean = false,
)

/**
 * Immutable Ludo match state. [tokens] maps each colour to its four tokens'
 * `progress` values (see [LudoBoard]). The engine is pure and deterministic:
 * dice values are passed in (randomness lives in [Dice]/the ViewModel), so every
 * rule is unit-testable. [turnId] increments on every roll/move, giving the UI a
 * cheap idempotency token so repeated taps can't replay a move.
 */
data class LudoState(
    val players: List<LudoColor>,
    val tokens: Map<LudoColor, List<Int>>,
    val current: Int,
    val phase: LudoPhase,
    val dice: Int? = null,
    val consecutiveSixes: Int = 0,
    val legalMoves: List<Int> = emptyList(),
    val winner: LudoColor? = null,
    val rules: LudoRules = LudoRules(),
    val lastEvent: LudoEvent? = null,
    val turnId: Int = 0,
) {
    val currentColor: LudoColor get() = players[current]
    fun progressOf(color: LudoColor): List<Int> = tokens.getValue(color)
    fun tokensHome(color: LudoColor): Int = tokens.getValue(color).count { it == LudoBoard.FINISH }
    fun tokensInYard(color: LudoColor): Int = tokens.getValue(color).count { it < 0 }
    fun tokensOnBoard(color: LudoColor): Int = tokens.getValue(color).count { it in 0..55 }
    val isOver: Boolean get() = phase == LudoPhase.GAME_OVER
}

/**
 * The pure Ludo rules engine: dice resolution, legal-move calculation, movement
 * along each colour's exact path, capturing, safe squares, extra turns, the
 * three-sixes rule and win detection. No Android, no rendering, no randomness.
 */
object LudoEngine {

    fun newGame(
        players: List<LudoColor>,
        rules: LudoRules = LudoRules(),
    ): LudoState {
        require(players.size in 2..4) { "Ludo supports 2-4 players" }
        require(players.distinct().size == players.size) { "Duplicate colours" }
        val tokens = players.associateWith { List(LudoBoard.TOKENS) { -1 } }
        return LudoState(
            players = players,
            tokens = tokens,
            current = 0,
            phase = LudoPhase.AWAIT_ROLL,
            rules = rules,
        )
    }

    /** Token indices (0..3) of [color] that can legally move with [die]. */
    fun legalMovesFor(state: LudoState, color: LudoColor, die: Int): List<Int> {
        val progresses = state.tokens.getValue(color)
        val result = ArrayList<Int>(LudoBoard.TOKENS)
        for (i in progresses.indices) {
            val p = progresses[i]
            when {
                p < 0 -> if (die == 6 || !state.rules.enterOnSix) result.add(i)
                p in 0..55 -> if (p + die <= LudoBoard.FINISH) result.add(i)
                // p == FINISH: already home, cannot move
            }
        }
        return result
    }

    /**
     * Applies a rolled [value] (1..6) for the current player. Resolves the
     * three-sixes rule, then either moves to [LudoPhase.AWAIT_MOVE] (legal moves
     * exist) or passes the turn (none exist).
     */
    fun roll(state: LudoState, value: Int): LudoState {
        require(value in 1..6)
        if (state.phase != LudoPhase.AWAIT_ROLL) return state

        val color = state.currentColor
        val sixes = if (value == 6) state.consecutiveSixes + 1 else 0

        if (value == 6 && state.rules.threeSixesForfeits && sixes >= 3) {
            // The third six is voided and the turn passes.
            return advanceTurn(
                state.copy(
                    dice = null,
                    consecutiveSixes = 0,
                    legalMoves = emptyList(),
                    lastEvent = LudoEvent(type = LudoEventType.THREE_SIXES_FORFEIT, color = color),
                    turnId = state.turnId + 1,
                ),
            )
        }

        val moves = legalMovesFor(state, color, value)
        if (moves.isEmpty()) {
            // Nothing to do with this roll; the turn passes (even on a 6).
            return advanceTurn(
                state.copy(
                    dice = value,
                    consecutiveSixes = sixes,
                    legalMoves = emptyList(),
                    lastEvent = LudoEvent(type = LudoEventType.ROLL_NO_MOVE, color = color),
                    turnId = state.turnId + 1,
                ),
            )
        }

        return state.copy(
            phase = LudoPhase.AWAIT_MOVE,
            dice = value,
            consecutiveSixes = sixes,
            legalMoves = moves,
            lastEvent = null,
            turnId = state.turnId + 1,
        )
    }

    /**
     * Moves [tokenIndex] of the current player by the current dice value. No-op
     * (returns the same state) if it isn't a legal move, which makes repeated
     * taps safe. Resolves captures, finishing, extra turns and win detection.
     */
    fun applyMove(state: LudoState, tokenIndex: Int): LudoState {
        if (state.phase != LudoPhase.AWAIT_MOVE) return state
        val die = state.dice ?: return state
        if (tokenIndex !in state.legalMoves) return state

        val color = state.currentColor
        val progresses = state.tokens.getValue(color).toMutableList()
        val from = progresses[tokenIndex]
        val entered = from < 0
        val to = if (entered) 0 else from + die
        progresses[tokenIndex] = to

        val mutableTokens = state.tokens.mapValues { it.value.toMutableList() }.toMutableMap()
        mutableTokens[color] = progresses

        // Resolve captures when landing on a non-safe shared ring square.
        val captured = ArrayList<CapturedToken>()
        if (to in 0..LudoBoard.LAST_RING) {
            val landingAbs = LudoBoard.ringIndex(color, to)
            val safe = landingAbs in LudoBoard.safeRingIndices
            if (!safe) {
                for (other in state.players) {
                    if (other == color) continue
                    val list = mutableTokens.getValue(other)
                    for (j in list.indices) {
                        val op = list[j]
                        if (op in 0..LudoBoard.LAST_RING && LudoBoard.ringIndex(other, op) == landingAbs) {
                            list[j] = -1
                            captured.add(CapturedToken(other, j))
                        }
                    }
                }
            }
        }

        val finished = to == LudoBoard.FINISH
        val frozenTokens = mutableTokens.mapValues { it.value.toList() }
        val homeCount = frozenTokens.getValue(color).count { it == LudoBoard.FINISH }
        val won = homeCount >= state.rules.tokensToWin

        val extra = !won && (
            (die == 6 && state.rules.extraTurnOnSix) ||
                (captured.isNotEmpty() && state.rules.extraTurnOnCapture) ||
                (finished && state.rules.extraTurnOnFinish)
            )

        val event = LudoEvent(
            type = LudoEventType.MOVE,
            color = color,
            tokenIndex = tokenIndex,
            fromProgress = from,
            toProgress = to,
            captured = captured,
            finishedToken = finished,
            enteredBoard = entered,
            extraTurn = extra,
        )

        val moved = state.copy(
            tokens = frozenTokens,
            legalMoves = emptyList(),
            lastEvent = event,
            turnId = state.turnId + 1,
        )

        return when {
            won -> moved.copy(phase = LudoPhase.GAME_OVER, winner = color, dice = null)
            extra -> moved.copy(phase = LudoPhase.AWAIT_ROLL, dice = null) // keep consecutiveSixes
            else -> advanceTurn(moved)
        }
    }

    /** Advances to the next player, clearing dice and the six-streak. */
    private fun advanceTurn(state: LudoState): LudoState = state.copy(
        current = (state.current + 1) % state.players.size,
        phase = LudoPhase.AWAIT_ROLL,
        dice = null,
        consecutiveSixes = 0,
        legalMoves = emptyList(),
    )
}

/** Fair 1..6 dice. Kept separate from the engine so results stay injectable/testable. */
class Dice(private val random: Random = Random.Default) {
    fun roll(): Int = random.nextInt(1, 7)
}
