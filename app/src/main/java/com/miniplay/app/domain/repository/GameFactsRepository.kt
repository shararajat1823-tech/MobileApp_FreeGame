package com.miniplay.app.domain.repository

/**
 * Small boolean/set facts that feed achievements but are not scores: which games
 * have been completed on Hard, and whether the player has ever had a flawless
 * run. Kept apart from [GameStatsRepository] so the stats table stays numeric.
 */
interface GameFactsRepository {
    suspend fun getFacts(): GameFacts
    suspend fun markHardCompleted(gameId: String)
    suspend fun markFlawless()
    suspend fun reset()
}

data class GameFacts(
    val completedHardGameIds: Set<String> = emptySet(),
    val hasAnyFlawless: Boolean = false,
)
