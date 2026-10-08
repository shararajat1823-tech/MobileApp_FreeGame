package com.miniplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniplay.app.domain.model.AchievementState

@Entity(tableName = "achievement_state")
data class AchievementStateEntity(
    @PrimaryKey val id: String,
    val unlocked: Boolean,
    val progress: Int,
    val unlockedAt: Long?,
) {
    fun toDomain(): AchievementState = AchievementState(
        id = id,
        unlocked = unlocked,
        progress = progress,
        unlockedAt = unlockedAt,
    )

    companion object {
        fun from(state: AchievementState): AchievementStateEntity = AchievementStateEntity(
            id = state.id,
            unlocked = state.unlocked,
            progress = state.progress,
            unlockedAt = state.unlockedAt,
        )
    }
}
