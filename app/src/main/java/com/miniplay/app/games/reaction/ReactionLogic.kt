package com.miniplay.app.games.reaction

import kotlin.random.Random

enum class ReactionRating { LIGHTNING, EXCELLENT, GOOD, PRACTICE }

/** Running stats across attempts within a session. */
data class ReactionStats(
    val attempts: Int = 0,
    val bestMs: Int? = null,
    val lastMs: Int? = null,
    internal val totalMs: Long = 0L,
) {
    val averageMs: Int? get() = if (attempts == 0) null else (totalMs / attempts).toInt()
}

/**
 * Pure reaction-test scoring: rating thresholds and incremental stats. Kept
 * separate from the timing coroutine so the thresholds and averaging are
 * unit-testable.
 */
object ReactionLogic {

    const val MIN_DELAY_MS = 1200L
    const val MAX_DELAY_MS = 3500L

    fun rate(ms: Int): ReactionRating = when {
        ms < 200 -> ReactionRating.LIGHTNING
        ms < 300 -> ReactionRating.EXCELLENT
        ms < 400 -> ReactionRating.GOOD
        else -> ReactionRating.PRACTICE
    }

    fun record(stats: ReactionStats, ms: Int): ReactionStats = ReactionStats(
        attempts = stats.attempts + 1,
        bestMs = if (stats.bestMs == null) ms else minOf(stats.bestMs, ms),
        lastMs = ms,
        totalMs = stats.totalMs + ms,
    )

    fun randomDelayMillis(random: Random = Random.Default): Long =
        random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS)
}
