package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.GameResult
import kotlinx.coroutines.flow.Flow

/**
 * Forward-looking contracts for online features. They are intentionally defined
 * but NOT wired into the app in v1 — MiniPlay is fully offline. No-op
 * implementations live in `data.repository.DisabledOnlineRepositories`, so the
 * day a backend arrives it can be swapped in behind these same interfaces
 * without touching the games or the UI.
 */

/** Global / friends leaderboards. */
interface LeaderboardRepository {
    val isEnabled: Boolean
    fun observeTopScores(gameId: String, limit: Int): Flow<List<LeaderboardEntry>>
    suspend fun submitScore(gameId: String, score: Long)
}

data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val score: Long,
    val isCurrentPlayer: Boolean,
)

/** Account / authentication, for cloud features. */
interface UserRepository {
    val isSignedIn: Boolean
    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signOut()
}

/** Cloud save / cross-device sync. */
interface CloudSyncRepository {
    val isEnabled: Boolean
    suspend fun push(): Result<Unit>
    suspend fun pull(): Result<Unit>
    fun observeSyncState(): Flow<SyncState>
}

enum class SyncState { DISABLED, IDLE, SYNCING, ERROR }

/** Server-authoritative analytics sink (local no-op today). */
interface RemoteAnalyticsSink {
    val isEnabled: Boolean
    suspend fun report(result: GameResult)
}
