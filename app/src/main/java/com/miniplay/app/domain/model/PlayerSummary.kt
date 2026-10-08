package com.miniplay.app.domain.model

/**
 * Cross-game aggregate shown on the Profile screen. Derived from all [GameStats]
 * plus streak and achievement counts.
 */
data class PlayerSummary(
    val gamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val bestScore: Long = 0L,
    val totalPlayTimeMillis: Long = 0L,
    val favouriteGameId: String? = null,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val achievementsUnlocked: Int = 0,
    val achievementsTotal: Int = 0,
)
