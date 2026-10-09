package com.miniplay.app.games.ludo

import com.miniplay.app.domain.model.GameDifficulty

/** The offline modes offered in this build (no online multiplayer). */
enum class LudoMode { VS_COMPUTER, LOCAL, PRACTICE }

enum class SeatType { HUMAN, BOT }

/** One seat at the table: which colour, controlled by a human or a bot. */
data class Seat(
    val color: LudoColor,
    val type: SeatType,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
) {
    val isBot: Boolean get() = type == SeatType.BOT
}

/**
 * Describes a single token move for the board to animate. The authoritative
 * [LudoState] already reflects the final positions; this tells the view which
 * token to glide along which path, and where captures/finish happened so it can
 * flash them. Emitted on a SharedFlow so a dropped frame never desyncs state.
 */
data class LudoMoveAnim(
    val id: Long,
    val color: LudoColor,
    val tokenIndex: Int,
    val fromProgress: Int,
    val toProgress: Int,
    val captured: List<GridCell>,
    val entered: Boolean,
    val finished: Boolean,
)

/** Builds the standard seat layout for a given player count and mode. */
object LudoSetup {
    /** Colours used for N players: 2 = opposite corners, then add clockwise. */
    fun colorsFor(count: Int): List<LudoColor> = when (count.coerceIn(2, 4)) {
        2 -> listOf(LudoColor.RED, LudoColor.YELLOW)
        3 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW)
        else -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
    }

    fun seats(mode: LudoMode, playerCount: Int, humans: Int, difficulty: GameDifficulty): List<Seat> {
        val colors = colorsFor(playerCount)
        return colors.mapIndexed { index, color ->
            val type = if (index < humans) SeatType.HUMAN else SeatType.BOT
            Seat(color, type, difficulty)
        }
    }
}
