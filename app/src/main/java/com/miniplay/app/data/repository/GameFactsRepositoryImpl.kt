package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.domain.repository.GameFacts
import com.miniplay.app.domain.repository.GameFactsRepository
import kotlinx.coroutines.flow.first

class GameFactsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : GameFactsRepository {

    override suspend fun getFacts(): GameFacts {
        val prefs = dataStore.data.first()
        return GameFacts(
            completedHardGameIds = prefs[PrefKeys.FACT_HARD_COMPLETED] ?: emptySet(),
            hasAnyFlawless = prefs[PrefKeys.FACT_ANY_FLAWLESS] ?: false,
        )
    }

    override suspend fun markHardCompleted(gameId: String) {
        dataStore.edit { prefs ->
            val current = prefs[PrefKeys.FACT_HARD_COMPLETED] ?: emptySet()
            prefs[PrefKeys.FACT_HARD_COMPLETED] = current + gameId
        }
    }

    override suspend fun markFlawless() {
        dataStore.edit { prefs -> prefs[PrefKeys.FACT_ANY_FLAWLESS] = true }
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(PrefKeys.FACT_HARD_COMPLETED)
            prefs.remove(PrefKeys.FACT_ANY_FLAWLESS)
        }
    }
}
