package com.miniplay.app.domain.model

/**
 * The outcome of a single play session, reported by a game when it ends. This is
 * the one object games hand back to the app; the stats, achievement, streak and
 * daily-challenge systems all react to it. Games never touch repositories
 * directly for recording — they emit a [GameResult].
 */
data class GameResult(
    val gameId: String,
    /** Primary score. Interpreted via [GameMetadata.scoreDirection]. 0 when the game is unscored. */
    val score: Long,
    /** Win/loss where meaningful; null for games that are not win/lose (e.g. Reaction, Puzzle). */
    val won: Boolean?,
    val difficulty: GameDifficulty,
    val durationMillis: Long,
    /** True if the player finished with a clean run (feeds the "Perfect" achievement). */
    val flawless: Boolean = false,
    val completed: Boolean = true,
    /** Game-specific extra metrics (e.g. "reactionMs", "moves") for achievements/daily challenge. */
    val metrics: Map<String, Long> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)
