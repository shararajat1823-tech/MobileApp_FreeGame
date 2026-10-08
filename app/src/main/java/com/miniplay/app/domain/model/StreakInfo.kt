package com.miniplay.app.domain.model

/**
 * Daily-activity streak. [lastActiveEpochDay] is the local day index
 * (`epochMillis / 86_400_000` adjusted for the zone) of the last day the player
 * finished at least one game. The weekly checkmarks on the home screen are
 * derived from [currentStreakDays] relative to today.
 */
data class StreakInfo(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastActiveEpochDay: Long = -1L,
) {
    val isActive: Boolean get() = currentStreakDays > 0
}
