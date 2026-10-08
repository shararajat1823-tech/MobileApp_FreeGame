package com.miniplay.app.games.tapchallenge

/**
 * Pure Tap-Challenge scoring and difficulty ramp. The ViewModel drives the clock
 * and spawns targets; this object owns every number so scoring, combos and the
 * "gets harder over time" curve are unit-testable.
 */
data class TapStats(
    val score: Int = 0,
    val hits: Int = 0,
    val misses: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
) {
    val taps: Int get() = hits + misses
    val accuracyPercent: Int get() = if (taps == 0) 0 else (hits * 100) / taps
    val flawless: Boolean get() = hits > 0 && misses == 0
}

object TapEngine {

    const val DURATION_MS = 30_000L
    const val BASE_POINTS = 10
    const val MAX_MULTIPLIER = 5
    const val MISS_PENALTY = 5

    /** Combo multiplier grows every 4 consecutive hits, capped. */
    fun multiplier(combo: Int): Int = (1 + combo / 4).coerceAtMost(MAX_MULTIPLIER)

    fun onHit(stats: TapStats): TapStats {
        val gained = BASE_POINTS * multiplier(stats.combo)
        val newCombo = stats.combo + 1
        return stats.copy(
            score = stats.score + gained,
            hits = stats.hits + 1,
            combo = newCombo,
            bestCombo = maxOf(stats.bestCombo, newCombo),
        )
    }

    fun onMiss(stats: TapStats): TapStats = stats.copy(
        score = (stats.score - MISS_PENALTY).coerceAtLeast(0),
        misses = stats.misses + 1,
        combo = 0,
    )

    /** [elapsedFraction] in 0..1. Target radius shrinks as time runs out. */
    fun targetRadiusFraction(elapsedFraction: Float): Float {
        val f = elapsedFraction.coerceIn(0f, 1f)
        return lerp(START_RADIUS_FRACTION, END_RADIUS_FRACTION, f)
    }

    /** Target on-screen lifetime shortens as time runs out. */
    fun targetLifetimeMs(elapsedFraction: Float): Long {
        val f = elapsedFraction.coerceIn(0f, 1f)
        return lerp(START_LIFETIME_MS.toFloat(), END_LIFETIME_MS.toFloat(), f).toLong()
    }

    private const val START_RADIUS_FRACTION = 0.14f
    private const val END_RADIUS_FRACTION = 0.07f
    private const val START_LIFETIME_MS = 1150
    private const val END_LIFETIME_MS = 560

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
}
