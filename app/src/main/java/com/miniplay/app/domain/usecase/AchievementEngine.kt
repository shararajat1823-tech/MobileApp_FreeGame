package com.miniplay.app.domain.usecase

import com.miniplay.app.domain.model.AchievementCondition
import com.miniplay.app.domain.model.AchievementDefinition
import com.miniplay.app.domain.model.AchievementSnapshot
import com.miniplay.app.domain.model.AchievementState

/**
 * Pure evaluator: given the achievement catalogue and a snapshot of the player's
 * aggregates, computes each achievement's progress and unlock state. Unlocks are
 * monotonic — once unlocked, an achievement never re-locks even if a later
 * snapshot would no longer satisfy it.
 *
 * No Android, repository or coroutine dependencies, so it is exhaustively
 * unit-testable.
 */
object AchievementEngine {

    data class Evaluation(
        val states: List<AchievementState>,
        /** Ids that were locked before and are unlocked now. */
        val newlyUnlocked: List<String>,
    )

    fun evaluate(
        definitions: List<AchievementDefinition>,
        snapshot: AchievementSnapshot,
        previous: Map<String, AchievementState>,
        now: Long,
    ): Evaluation {
        val states = ArrayList<AchievementState>(definitions.size)
        val unlockedNow = ArrayList<String>()

        for (def in definitions) {
            val prev = previous[def.id]
            val progress = progressFor(def.condition, snapshot).coerceIn(0, def.target)
            val wasUnlocked = prev?.unlocked == true
            val nowUnlocked = wasUnlocked || progress >= def.target

            if (nowUnlocked && !wasUnlocked) unlockedNow += def.id

            states += AchievementState(
                id = def.id,
                unlocked = nowUnlocked,
                // Keep the bar full once unlocked.
                progress = if (nowUnlocked) def.target else progress,
                unlockedAt = when {
                    wasUnlocked -> prev?.unlockedAt
                    nowUnlocked -> now
                    else -> null
                },
            )
        }
        return Evaluation(states, unlockedNow)
    }

    /** Current progress (in target units) toward a condition. */
    private fun progressFor(condition: AchievementCondition, s: AchievementSnapshot): Int =
        when (condition) {
            is AchievementCondition.TotalGamesPlayed -> s.totalGamesPlayed
            is AchievementCondition.TotalWins -> s.totalWins
            is AchievementCondition.ReactionUnderMs ->
                if (s.bestReactionMs != null && s.bestReactionMs <= condition.ms) 1 else 0
            is AchievementCondition.HardCompleted ->
                if (condition.gameId in s.completedHardGameIds) 1 else 0
            AchievementCondition.AnyFlawless -> if (s.hasAnyFlawless) 1 else 0
            is AchievementCondition.StreakReached -> s.currentStreakDays
            is AchievementCondition.ScoreReached ->
                if ((s.bestScoresByGame[condition.gameId] ?: Long.MIN_VALUE) >= condition.score) 1 else 0
        }
}
