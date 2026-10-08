package com.miniplay.app.domain.model

/**
 * Whether a higher or lower score is "better" for a game. Reaction time and
 * puzzle completion time are lower-is-better; everything else is higher-is-better.
 * The stats repository uses this to decide when a new result beats the record.
 */
enum class ScoreDirection {
    HIGHER_IS_BETTER,
    LOWER_IS_BETTER;

    /** True if [candidate] is a strictly better score than [current] (nullable = no record yet). */
    fun isBetter(candidate: Long, current: Long?): Boolean {
        if (current == null) return true
        return when (this) {
            HIGHER_IS_BETTER -> candidate > current
            LOWER_IS_BETTER -> candidate < current
        }
    }
}
