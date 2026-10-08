package com.miniplay.app.domain.model

import androidx.annotation.StringRes

/**
 * Static description of an achievement. The catalogue of these lives in
 * [AchievementCatalog]; unlock state is tracked separately and combined into an
 * [Achievement] for display.
 */
data class AchievementDefinition(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val emoji: String,
    /** Number of units needed to unlock (used for the progress bar; 1 = binary). */
    val target: Int,
    val condition: AchievementCondition,
)

/**
 * The measurable requirement behind an achievement. Evaluated purely by
 * [com.miniplay.app.domain.usecase.AchievementEngine] against an
 * [AchievementSnapshot], so it is fully unit-testable with no Android deps.
 */
sealed interface AchievementCondition {
    data class TotalGamesPlayed(val count: Int) : AchievementCondition
    data class TotalWins(val count: Int) : AchievementCondition
    data class ReactionUnderMs(val ms: Int) : AchievementCondition
    data class HardCompleted(val gameId: String) : AchievementCondition
    data object AnyFlawless : AchievementCondition
    data class StreakReached(val days: Int) : AchievementCondition
    data class ScoreReached(val gameId: String, val score: Long) : AchievementCondition
}

/**
 * Everything the achievement evaluator needs, assembled from the various
 * repositories. Pure data so evaluation stays testable.
 */
data class AchievementSnapshot(
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val bestReactionMs: Int? = null,
    val completedHardGameIds: Set<String> = emptySet(),
    val hasAnyFlawless: Boolean = false,
    val currentStreakDays: Int = 0,
    val bestScoresByGame: Map<String, Long> = emptyMap(),
)

/** Unlock state for a single achievement id. */
data class AchievementState(
    val id: String,
    val unlocked: Boolean,
    val progress: Int,
    val unlockedAt: Long? = null,
)

/** Definition + live state, ready for the UI. */
data class Achievement(
    val definition: AchievementDefinition,
    val state: AchievementState,
) {
    val id: String get() = definition.id
    val unlocked: Boolean get() = state.unlocked
    val progress: Int get() = state.progress.coerceAtMost(definition.target)
    val target: Int get() = definition.target
    val fractionComplete: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}
