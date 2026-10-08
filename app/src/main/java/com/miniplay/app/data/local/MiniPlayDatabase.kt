package com.miniplay.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.miniplay.app.data.local.dao.AchievementStateDao
import com.miniplay.app.data.local.dao.GameStatsDao
import com.miniplay.app.data.local.entity.AchievementStateEntity
import com.miniplay.app.data.local.entity.GameStatsEntity

/**
 * Room database holding the list-shaped persistent state: per-game stats and
 * achievement unlock state. Scalar preferences (settings, profile, streak, daily
 * challenge) live in DataStore instead.
 */
@Database(
    entities = [GameStatsEntity::class, AchievementStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MiniPlayDatabase : RoomDatabase() {
    abstract fun gameStatsDao(): GameStatsDao
    abstract fun achievementStateDao(): AchievementStateDao

    companion object {
        private const val DB_NAME = "miniplay.db"

        fun build(context: Context): MiniPlayDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MiniPlayDatabase::class.java,
                DB_NAME,
            )
                // Pre-1.0 we have a single schema; destructive fallback keeps dev
                // installs from crashing. Replace with real migrations before launch.
                .fallbackToDestructiveMigration()
                .build()
    }
}
