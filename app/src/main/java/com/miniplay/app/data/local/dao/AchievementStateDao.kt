package com.miniplay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.miniplay.app.data.local.entity.AchievementStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementStateDao {

    @Query("SELECT * FROM achievement_state")
    fun observeAll(): Flow<List<AchievementStateEntity>>

    @Query("SELECT * FROM achievement_state")
    suspend fun getAll(): List<AchievementStateEntity>

    @Upsert
    suspend fun upsertAll(entities: List<AchievementStateEntity>)

    @Query("DELETE FROM achievement_state")
    suspend fun clear()
}
