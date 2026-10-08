package com.miniplay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.miniplay.app.data.local.entity.GameStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {

    @Query("SELECT * FROM game_stats")
    fun observeAll(): Flow<List<GameStatsEntity>>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId LIMIT 1")
    fun observe(gameId: String): Flow<GameStatsEntity?>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId LIMIT 1")
    suspend fun get(gameId: String): GameStatsEntity?

    @Query("SELECT * FROM game_stats")
    suspend fun getAll(): List<GameStatsEntity>

    @Upsert
    suspend fun upsert(entity: GameStatsEntity)

    @Query("DELETE FROM game_stats")
    suspend fun clear()
}
