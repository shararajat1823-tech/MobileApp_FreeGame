package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.DailyChallenge
import kotlinx.coroutines.flow.Flow

/**
 * Supplies today's challenge and records attempts against it. The default
 * implementation generates challenges locally; the interface is shaped so a
 * remote/backend source can be dropped in later with no UI change.
 */
interface DailyChallengeRepository {
    fun observeToday(): Flow<DailyChallenge>

    /** Records a play result against today's challenge if it belongs to that game. */
    suspend fun submitResult(gameId: String, score: Long)

    suspend fun reset()
}
