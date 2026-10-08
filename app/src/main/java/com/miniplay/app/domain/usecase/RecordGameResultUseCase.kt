package com.miniplay.app.domain.usecase

import com.miniplay.app.core.common.Clock
import com.miniplay.app.domain.model.AchievementSnapshot
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.repository.AchievementRepository
import com.miniplay.app.domain.repository.DailyChallengeRepository
import com.miniplay.app.domain.repository.GameFactsRepository
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.domain.repository.RemoteAnalyticsSink
import com.miniplay.app.domain.repository.StreakRepository

/**
 * The single entry point games use when a session ends. It fans a [GameResult]
 * out to every subsystem — stats, last-played, streak, daily challenge,
 * achievement facts — then re-evaluates achievements. Games stay fully decoupled
 * from persistence: they only build and submit a result.
 *
 * Ordering matters: facts/stats/streak are updated *before* achievements are
 * evaluated so the snapshot reflects this very result.
 */
class RecordGameResultUseCase(
    private val statsRepository: GameStatsRepository,
    private val streakRepository: StreakRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val achievementRepository: AchievementRepository,
    private val gameFactsRepository: GameFactsRepository,
    private val analyticsSink: RemoteAnalyticsSink,
    private val clock: Clock,
) {

    suspend operator fun invoke(result: GameResult) {
        // 1. Core stats + "continue playing" pointer.
        statsRepository.recordResult(result)
        statsRepository.setLastPlayed(result.gameId)

        // 2. Non-score facts that back achievements.
        if (result.completed && result.difficulty == GameDifficulty.HARD) {
            gameFactsRepository.markHardCompleted(result.gameId)
        }
        if (result.flawless) {
            gameFactsRepository.markFlawless()
        }

        // 3. Streak + daily challenge.
        val streak = streakRepository.registerActivity(clock.todayEpochDay())
        dailyChallengeRepository.submitResult(result.gameId, result.score)

        // 4. Re-evaluate achievements against the fresh snapshot.
        val snapshot = buildSnapshot(currentStreakDays = streak.currentStreakDays)
        val previous = achievementRepository.getStates().associateBy { it.id }
        val evaluation = AchievementEngine.evaluate(
            definitions = AchievementCatalog.all,
            snapshot = snapshot,
            previous = previous,
            now = clock.nowMillis(),
        )
        achievementRepository.applyStates(evaluation.states)

        // 5. Best-effort analytics (no-op offline).
        if (analyticsSink.isEnabled) analyticsSink.report(result)
    }

    private suspend fun buildSnapshot(currentStreakDays: Int): AchievementSnapshot {
        val stats = statsRepository.getAllStats()
        val facts = gameFactsRepository.getFacts()
        val bestScores = stats
            .mapNotNull { s -> s.bestScore?.let { s.gameId to it } }
            .toMap()
        // Reaction stores the best time in milliseconds as its "best score"
        // (lower is better), so the raw best doubles as the reaction metric.
        val bestReaction = bestScores[GameIds.REACTION]?.toInt()

        return AchievementSnapshot(
            totalGamesPlayed = stats.sumOf { it.playCount },
            totalWins = stats.sumOf { it.wins },
            bestReactionMs = bestReaction,
            completedHardGameIds = facts.completedHardGameIds,
            hasAnyFlawless = facts.hasAnyFlawless,
            currentStreakDays = currentStreakDays,
            bestScoresByGame = bestScores,
        )
    }
}
