package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.core.common.Clock
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.domain.model.DailyChallenge
import com.miniplay.app.domain.repository.DailyChallengeRepository
import com.miniplay.app.domain.usecase.DailyChallengeGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DailyChallengeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : DailyChallengeRepository {

    override fun observeToday(): Flow<DailyChallenge> = dataStore.data.map { prefs ->
        val today = clock.todayEpochDay()
        val storedDay = prefs[PrefKeys.DAILY_DAY]
        val playerResult = if (storedDay == today) prefs[PrefKeys.DAILY_RESULT] else null
        DailyChallengeGenerator.generate(epochDay = today, playerResult = playerResult)
    }

    override suspend fun submitResult(gameId: String, score: Long) {
        val today = clock.todayEpochDay()
        val challenge = DailyChallengeGenerator.generate(epochDay = today)
        if (challenge.gameId != gameId) return

        dataStore.edit { prefs ->
            val sameDay = prefs[PrefKeys.DAILY_DAY] == today
            val existing = if (sameDay) prefs[PrefKeys.DAILY_RESULT] else null
            val best = if (existing == null) {
                score
            } else {
                // Keep the attempt closest to "completing" the challenge.
                when (challenge.scoreDirection) {
                    com.miniplay.app.domain.model.ScoreDirection.HIGHER_IS_BETTER -> maxOf(existing, score)
                    com.miniplay.app.domain.model.ScoreDirection.LOWER_IS_BETTER -> minOf(existing, score)
                }
            }
            prefs[PrefKeys.DAILY_DAY] = today
            prefs[PrefKeys.DAILY_RESULT] = best
        }
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(PrefKeys.DAILY_DAY)
            prefs.remove(PrefKeys.DAILY_RESULT)
        }
    }
}
