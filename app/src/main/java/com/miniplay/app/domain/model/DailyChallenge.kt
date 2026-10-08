package com.miniplay.app.domain.model

/**
 * One day's featured challenge. Generated locally and deterministically from the
 * calendar day (see `domain.usecase.DailyChallengeGenerator`), but modelled so a
 * backend-supplied challenge can replace the generator without UI changes.
 *
 * [target] is interpreted with [GameMetadata.scoreDirection]: for a
 * lower-is-better game (Reaction) the player must score at or below it; for a
 * higher-is-better game, at or above it.
 */
data class DailyChallenge(
    val epochDay: Long,
    val gameId: String,
    val difficulty: GameDifficulty,
    val target: Long,
    val scoreDirection: ScoreDirection,
    /** The player's best attempt today, or null if not attempted. */
    val playerResult: Long? = null,
) {
    val attempted: Boolean get() = playerResult != null

    val completed: Boolean
        get() {
            val result = playerResult ?: return false
            return when (scoreDirection) {
                ScoreDirection.HIGHER_IS_BETTER -> result >= target
                ScoreDirection.LOWER_IS_BETTER -> result <= target
            }
        }
}
