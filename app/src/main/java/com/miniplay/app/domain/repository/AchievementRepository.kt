package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.Achievement
import com.miniplay.app.domain.model.AchievementState
import kotlinx.coroutines.flow.Flow

/**
 * Stores achievement unlock state and exposes the merged [Achievement] list
 * (catalogue definition + persisted state). [newlyUnlocked] is a hot stream the
 * UI observes to play the unlock animation exactly once per unlock.
 */
interface AchievementRepository {

    fun observeAchievements(): Flow<List<Achievement>>

    suspend fun getStates(): List<AchievementState>

    /** Persists computed states, returning the ids that transitioned to unlocked. */
    suspend fun applyStates(states: List<AchievementState>): List<String>

    /** Emits each achievement id the moment it is unlocked, for a one-shot animation. */
    val newlyUnlocked: Flow<String>

    suspend fun resetAll()
}
