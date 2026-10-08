package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.data.local.dao.GameStatsDao
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.data.local.entity.GameStatsEntity
import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.model.GameStats
import com.miniplay.app.domain.model.ScoreDirection
import com.miniplay.app.domain.repository.GameCatalog
import com.miniplay.app.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameStatsRepositoryImpl(
    private val dao: GameStatsDao,
    private val dataStore: DataStore<Preferences>,
    private val catalog: GameCatalog,
) : GameStatsRepository {

    override fun observeAllStats(): Flow<List<GameStats>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeStats(gameId: String): Flow<GameStats> =
        dao.observe(gameId).map { it?.toDomain() ?: GameStats(gameId) }

    override suspend fun getStats(gameId: String): GameStats =
        dao.get(gameId)?.toDomain() ?: GameStats(gameId)

    override suspend fun getAllStats(): List<GameStats> =
        dao.getAll().map { it.toDomain() }

    override suspend fun recordResult(result: GameResult) {
        val existing = dao.get(result.gameId)?.toDomain() ?: GameStats(result.gameId)
        val meta = catalog.metadata(result.gameId)
        val direction = meta?.scoreDirection ?: ScoreDirection.HIGHER_IS_BETTER
        val tracksHighScore = meta?.tracksHighScore ?: true

        val newBest = if (tracksHighScore && direction.isBetter(result.score, existing.bestScore)) {
            result.score
        } else {
            existing.bestScore
        }

        val updated = existing.copy(
            bestScore = newBest,
            playCount = existing.playCount + 1,
            wins = existing.wins + if (result.won == true) 1 else 0,
            totalPlayTimeMillis = existing.totalPlayTimeMillis + result.durationMillis.coerceAtLeast(0),
            lastPlayedAt = result.timestamp,
        )
        dao.upsert(GameStatsEntity.from(updated))
    }

    override fun observeLastPlayedGameId(): Flow<String?> =
        dataStore.data.map { it[PrefKeys.LAST_PLAYED] }

    override suspend fun setLastPlayed(gameId: String) {
        dataStore.edit { it[PrefKeys.LAST_PLAYED] = gameId }
    }

    override suspend fun resetAll() {
        dao.clear()
        dataStore.edit { it.remove(PrefKeys.LAST_PLAYED) }
    }
}
