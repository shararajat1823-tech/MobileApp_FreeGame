package com.miniplay.app

import com.miniplay.app.domain.model.AchievementCondition
import com.miniplay.app.domain.model.AchievementDefinition
import com.miniplay.app.domain.model.AchievementSnapshot
import com.miniplay.app.domain.model.AchievementState
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.ScoreDirection
import com.miniplay.app.domain.model.StreakInfo
import com.miniplay.app.domain.usecase.AchievementEngine
import com.miniplay.app.domain.usecase.DailyChallengeGenerator
import com.miniplay.app.domain.usecase.StreakCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakCalculatorTest {

    @Test fun firstActivityStartsStreakAtOne() {
        val s = StreakCalculator.onActivity(StreakInfo(), epochDay = 100)
        assertEquals(1, s.currentStreakDays)
        assertEquals(1, s.bestStreakDays)
        assertEquals(100, s.lastActiveEpochDay)
    }

    @Test fun consecutiveDaysIncrement() {
        var s = StreakCalculator.onActivity(StreakInfo(), 100)
        s = StreakCalculator.onActivity(s, 101)
        s = StreakCalculator.onActivity(s, 102)
        assertEquals(3, s.currentStreakDays)
        assertEquals(3, s.bestStreakDays)
    }

    @Test fun sameDayIsIdempotent() {
        var s = StreakCalculator.onActivity(StreakInfo(), 100)
        s = StreakCalculator.onActivity(s, 100)
        assertEquals(1, s.currentStreakDays)
    }

    @Test fun gapResetsStreakButKeepsBest() {
        var s = StreakCalculator.onActivity(StreakInfo(), 100)
        s = StreakCalculator.onActivity(s, 101) // best = 2
        s = StreakCalculator.onActivity(s, 105) // gap -> reset to 1
        assertEquals(1, s.currentStreakDays)
        assertEquals(2, s.bestStreakDays)
    }

    @Test fun displayStreakExpiresAfterTwoMissedDays() {
        val info = StreakInfo(currentStreakDays = 5, bestStreakDays = 5, lastActiveEpochDay = 100)
        assertEquals(5, StreakCalculator.displayStreak(info, today = 100)) // today
        assertEquals(5, StreakCalculator.displayStreak(info, today = 101)) // still alive tomorrow
        assertEquals(0, StreakCalculator.displayStreak(info, today = 102)) // missed a day
    }
}

class AchievementEngineTest {

    private fun def(id: String, target: Int, cond: AchievementCondition) =
        AchievementDefinition(id, 0, 0, "⭐", target, cond)

    private val catalogue = listOf(
        def("first", 1, AchievementCondition.TotalGamesPlayed(1)),
        def("ten", 10, AchievementCondition.TotalGamesPlayed(10)),
        def("wins", 10, AchievementCondition.TotalWins(10)),
        def("lightning", 1, AchievementCondition.ReactionUnderMs(200)),
        def("hardmem", 1, AchievementCondition.HardCompleted(GameIds.MEMORY_MATCH)),
        def("perfect", 1, AchievementCondition.AnyFlawless),
        def("streak", 5, AchievementCondition.StreakReached(5)),
        def("t2048", 1, AchievementCondition.ScoreReached(GameIds.GAME_2048, 2048)),
    )

    @Test fun unlocksWhenConditionMet() {
        val snap = AchievementSnapshot(
            totalGamesPlayed = 12,
            totalWins = 10,
            bestReactionMs = 180,
            completedHardGameIds = setOf(GameIds.MEMORY_MATCH),
            hasAnyFlawless = true,
            currentStreakDays = 6,
            bestScoresByGame = mapOf(GameIds.GAME_2048 to 2048L),
        )
        val eval = AchievementEngine.evaluate(catalogue, snap, emptyMap(), now = 1_000L)
        val unlocked = eval.states.filter { it.unlocked }.map { it.id }.toSet()
        assertEquals(catalogue.map { it.id }.toSet(), unlocked)
        assertEquals(catalogue.size, eval.newlyUnlocked.size)
        assertTrue(eval.states.all { it.unlockedAt == 1_000L })
    }

    @Test fun progressTracksTowardTarget() {
        val snap = AchievementSnapshot(totalGamesPlayed = 4)
        val eval = AchievementEngine.evaluate(catalogue, snap, emptyMap(), now = 0)
        val ten = eval.states.first { it.id == "ten" }
        assertFalse(ten.unlocked)
        assertEquals(4, ten.progress)
    }

    @Test fun unlocksAreMonotonicAndNotReEmitted() {
        val first = AchievementEngine.evaluate(
            catalogue,
            AchievementSnapshot(totalGamesPlayed = 1),
            emptyMap(),
            now = 10,
        )
        assertTrue(first.newlyUnlocked.contains("first"))
        val previous = first.states.associateBy { it.id }

        // Next snapshot would no longer satisfy "first" (0 games) — stays unlocked.
        val second = AchievementEngine.evaluate(
            catalogue,
            AchievementSnapshot(totalGamesPlayed = 0),
            previous,
            now = 20,
        )
        val firstState = second.states.first { it.id == "first" }
        assertTrue(firstState.unlocked)
        assertEquals(10L, firstState.unlockedAt) // original unlock time preserved
        assertFalse(second.newlyUnlocked.contains("first")) // not emitted again
    }

    @Test fun reactionUnlockNeedsFastEnoughTime() {
        val slow = AchievementEngine.evaluate(
            catalogue, AchievementSnapshot(bestReactionMs = 250), emptyMap(), 0,
        ).states.first { it.id == "lightning" }
        assertFalse(slow.unlocked)
    }
}

class DailyChallengeGeneratorTest {

    @Test fun isDeterministicPerDay() {
        val a = DailyChallengeGenerator.generate(epochDay = 20000)
        val b = DailyChallengeGenerator.generate(epochDay = 20000)
        assertEquals(a.gameId, b.gameId)
        assertEquals(a.target, b.target)
        assertEquals(a.difficulty, b.difficulty)
    }

    @Test fun differentDaysVaryAndTargetsAreInRange() {
        val gameIds = (0 until 60).map { DailyChallengeGenerator.generate(20000L + it).gameId }.toSet()
        assertTrue(gameIds.size > 1, "challenge game should vary across days")

        (0 until 100).forEach { day ->
            val c = DailyChallengeGenerator.generate(20000L + day)
            val template = DailyChallengeGenerator.defaultTemplates.first { it.gameId == c.gameId }
            assertTrue(c.target in template.minTarget..template.maxTarget)
        }
    }

    @Test fun completionRespectsScoreDirection() {
        val higher = DailyChallengeGenerator
            .generate(epochDay = 1, templates = DailyChallengeGenerator.defaultTemplates)
            .copy(scoreDirection = ScoreDirection.HIGHER_IS_BETTER, target = 1000, playerResult = 1200)
        assertTrue(higher.completed)

        val lower = higher.copy(scoreDirection = ScoreDirection.LOWER_IS_BETTER, target = 300, playerResult = 280)
        assertTrue(lower.completed)
        assertFalse(lower.copy(playerResult = 320).completed)
    }
}
