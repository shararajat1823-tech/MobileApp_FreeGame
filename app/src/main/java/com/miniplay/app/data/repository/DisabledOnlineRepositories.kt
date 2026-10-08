package com.miniplay.app.data.repository

import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.repository.CloudSyncRepository
import com.miniplay.app.domain.repository.LeaderboardEntry
import com.miniplay.app.domain.repository.LeaderboardRepository
import com.miniplay.app.domain.repository.RemoteAnalyticsSink
import com.miniplay.app.domain.repository.SyncState
import com.miniplay.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * v1 no-op implementations of the online contracts. MiniPlay ships fully offline,
 * so each of these reports "disabled" and does nothing. When a backend is added,
 * these are the only classes that change — games and UI already code to the
 * interfaces.
 */

class DisabledLeaderboardRepository : LeaderboardRepository {
    override val isEnabled: Boolean = false
    override fun observeTopScores(gameId: String, limit: Int): Flow<List<LeaderboardEntry>> =
        flowOf(emptyList())
    override suspend fun submitScore(gameId: String, score: Long) = Unit
}

class DisabledUserRepository : UserRepository {
    override val isSignedIn: Boolean = false
    override suspend fun signInAnonymously(): Result<Unit> =
        Result.failure(IllegalStateException("Online accounts are not enabled in this version"))
    override suspend fun signOut() = Unit
}

class DisabledCloudSyncRepository : CloudSyncRepository {
    override val isEnabled: Boolean = false
    override suspend fun push(): Result<Unit> = Result.success(Unit)
    override suspend fun pull(): Result<Unit> = Result.success(Unit)
    override fun observeSyncState(): Flow<SyncState> = flowOf(SyncState.DISABLED)
}

class NoOpRemoteAnalyticsSink : RemoteAnalyticsSink {
    override val isEnabled: Boolean = false
    override suspend fun report(result: GameResult) = Unit
}
