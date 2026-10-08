package com.miniplay.app.domain.model

/**
 * Aggregate, persisted statistics for a single game. Rebuilt incrementally as
 * [GameResult]s arrive; the home screen and profile read it.
 */
data class GameStats(
    val gameId: String,
    val bestScore: Long? = null,
    val playCount: Int = 0,
    val wins: Int = 0,
    val totalPlayTimeMillis: Long = 0L,
    val lastPlayedAt: Long? = null,
) {
    val hasBeenPlayed: Boolean get() = playCount > 0
}
