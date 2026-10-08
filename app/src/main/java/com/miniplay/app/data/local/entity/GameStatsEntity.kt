package com.miniplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniplay.app.domain.model.GameStats

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val gameId: String,
    val bestScore: Long?,
    val playCount: Int,
    val wins: Int,
    val totalPlayTimeMillis: Long,
    val lastPlayedAt: Long?,
) {
    fun toDomain(): GameStats = GameStats(
        gameId = gameId,
        bestScore = bestScore,
        playCount = playCount,
        wins = wins,
        totalPlayTimeMillis = totalPlayTimeMillis,
        lastPlayedAt = lastPlayedAt,
    )

    companion object {
        fun from(stats: GameStats): GameStatsEntity = GameStatsEntity(
            gameId = stats.gameId,
            bestScore = stats.bestScore,
            playCount = stats.playCount,
            wins = stats.wins,
            totalPlayTimeMillis = stats.totalPlayTimeMillis,
            lastPlayedAt = stats.lastPlayedAt,
        )
    }
}
