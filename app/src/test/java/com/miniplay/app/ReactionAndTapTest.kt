package com.miniplay.app

import com.miniplay.app.games.reaction.ReactionLogic
import com.miniplay.app.games.reaction.ReactionRating
import com.miniplay.app.games.reaction.ReactionStats
import com.miniplay.app.games.tapchallenge.TapEngine
import com.miniplay.app.games.tapchallenge.TapStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReactionLogicTest {

    @Test fun ratingThresholds() {
        assertEquals(ReactionRating.LIGHTNING, ReactionLogic.rate(150))
        assertEquals(ReactionRating.LIGHTNING, ReactionLogic.rate(199))
        assertEquals(ReactionRating.EXCELLENT, ReactionLogic.rate(200))
        assertEquals(ReactionRating.EXCELLENT, ReactionLogic.rate(299))
        assertEquals(ReactionRating.GOOD, ReactionLogic.rate(300))
        assertEquals(ReactionRating.GOOD, ReactionLogic.rate(399))
        assertEquals(ReactionRating.PRACTICE, ReactionLogic.rate(400))
        assertEquals(ReactionRating.PRACTICE, ReactionLogic.rate(900))
    }

    @Test fun recordUpdatesBestAverageAndAttempts() {
        var stats = ReactionStats()
        stats = ReactionLogic.record(stats, 300)
        stats = ReactionLogic.record(stats, 200)
        stats = ReactionLogic.record(stats, 400)
        assertEquals(3, stats.attempts)
        assertEquals(200, stats.bestMs)
        assertEquals(400, stats.lastMs)
        assertEquals(300, stats.averageMs) // (300+200+400)/3
    }

    @Test fun emptyStatsHaveNoAverageOrBest() {
        val stats = ReactionStats()
        assertEquals(null, stats.averageMs)
        assertEquals(null, stats.bestMs)
    }
}

class TapEngineTest {

    @Test fun multiplierGrowsEveryFourHitsAndCaps() {
        assertEquals(1, TapEngine.multiplier(0))
        assertEquals(1, TapEngine.multiplier(3))
        assertEquals(2, TapEngine.multiplier(4))
        assertEquals(3, TapEngine.multiplier(8))
        assertEquals(TapEngine.MAX_MULTIPLIER, TapEngine.multiplier(1000))
    }

    @Test fun hitsAccumulateScoreWithCombo() {
        var s = TapStats()
        repeat(4) { s = TapEngine.onHit(s) } // 4 hits at x1 = 40
        assertEquals(40, s.score)
        assertEquals(4, s.combo)
        s = TapEngine.onHit(s) // 5th hit now at x2 = +20
        assertEquals(60, s.score)
        assertEquals(5, s.bestCombo)
    }

    @Test fun missResetsComboAndPenalisesButNotBelowZero() {
        var s = TapStats()
        s = TapEngine.onHit(s)
        s = TapEngine.onMiss(s)
        assertEquals(0, s.combo)
        assertEquals(1, s.misses)
        assertTrue(s.score >= 0)
        // Starting from zero, a miss cannot push the score negative.
        assertEquals(0, TapEngine.onMiss(TapStats()).score)
    }

    @Test fun accuracyAndFlawless() {
        var s = TapStats()
        repeat(3) { s = TapEngine.onHit(s) }
        assertEquals(100, s.accuracyPercent)
        assertTrue(s.flawless)
        s = TapEngine.onMiss(s)
        assertEquals(75, s.accuracyPercent) // 3 of 4
        assertTrue(!s.flawless)
    }

    @Test fun difficultyRampShrinksTargetsAndLifetime() {
        assertTrue(TapEngine.targetRadiusFraction(0f) > TapEngine.targetRadiusFraction(1f))
        assertTrue(TapEngine.targetLifetimeMs(0f) > TapEngine.targetLifetimeMs(1f))
        // Clamped outside 0..1.
        assertEquals(TapEngine.targetRadiusFraction(0f), TapEngine.targetRadiusFraction(-5f))
        assertEquals(TapEngine.targetRadiusFraction(1f), TapEngine.targetRadiusFraction(5f))
    }
}
