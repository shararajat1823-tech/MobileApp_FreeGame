package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.domain.model.StreakInfo
import com.miniplay.app.domain.repository.StreakRepository
import com.miniplay.app.domain.usecase.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StreakRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : StreakRepository {

    override fun observeStreak(): Flow<StreakInfo> = dataStore.data.map { it.readStreak() }

    override suspend fun registerActivity(epochDay: Long): StreakInfo {
        var updated = StreakInfo()
        dataStore.edit { prefs ->
            val current = prefs.readStreak()
            updated = StreakCalculator.onActivity(current, epochDay)
            prefs[PrefKeys.STREAK_CURRENT] = updated.currentStreakDays
            prefs[PrefKeys.STREAK_BEST] = updated.bestStreakDays
            prefs[PrefKeys.STREAK_LAST_DAY] = updated.lastActiveEpochDay
        }
        return updated
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(PrefKeys.STREAK_CURRENT)
            prefs.remove(PrefKeys.STREAK_BEST)
            prefs.remove(PrefKeys.STREAK_LAST_DAY)
        }
    }

    private fun Preferences.readStreak(): StreakInfo = StreakInfo(
        currentStreakDays = this[PrefKeys.STREAK_CURRENT] ?: 0,
        bestStreakDays = this[PrefKeys.STREAK_BEST] ?: 0,
        lastActiveEpochDay = this[PrefKeys.STREAK_LAST_DAY] ?: -1L,
    )
}
