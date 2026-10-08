package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.StreakInfo
import kotlinx.coroutines.flow.Flow

/** Tracks the daily-play streak. */
interface StreakRepository {
    fun observeStreak(): Flow<StreakInfo>

    /**
     * Records that the player was active on [epochDay] and returns the updated
     * streak. Idempotent within the same day.
     */
    suspend fun registerActivity(epochDay: Long): StreakInfo

    suspend fun reset()
}
