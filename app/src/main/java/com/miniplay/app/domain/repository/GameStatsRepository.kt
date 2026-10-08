package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.model.GameStats
import kotlinx.coroutines.flow.Flow

/**
 * Persistence of per-game statistics, scores and the "last played" pointer.
 * Implemented over Room. All reads are observable so the UI updates live.
 */
interface GameStatsRepository {

    fun observeAllStats(): Flow<List<GameStats>>

    fun observeStats(gameId: String): Flow<GameStats>

    suspend fun getStats(gameId: String): GameStats

    suspend fun getAllStats(): List<GameStats>

    /** Applies a result: bumps play count/wins/time and updates the best score. */
    suspend fun recordResult(result: GameResult)

    fun observeLastPlayedGameId(): Flow<String?>

    suspend fun setLastPlayed(gameId: String)

    /** Wipes every stat and score (used by "Reset all progress"). */
    suspend fun resetAll()
}
