package com.miniplay.app.domain.usecase

import com.miniplay.app.domain.model.StreakInfo
import kotlin.math.max

/**
 * Pure streak arithmetic. Separated from persistence so it can be unit-tested
 * exhaustively (first day, same day, consecutive day, gap).
 */
object StreakCalculator {

    /** Updates the streak given activity on [epochDay]. Idempotent for the same day. */
    fun onActivity(current: StreakInfo, epochDay: Long): StreakInfo {
        if (current.lastActiveEpochDay == epochDay) return current // already counted today

        val newStreak = when {
            current.lastActiveEpochDay < 0L -> 1
            epochDay == current.lastActiveEpochDay + 1 -> current.currentStreakDays + 1
            epochDay > current.lastActiveEpochDay -> 1 // missed one or more days
            else -> current.currentStreakDays // clock moved backwards; keep as-is
        }
        return StreakInfo(
            currentStreakDays = newStreak,
            bestStreakDays = max(current.bestStreakDays, newStreak),
            lastActiveEpochDay = maxOf(current.lastActiveEpochDay, epochDay),
        )
    }

    /**
     * The streak to *display* on [today]. A streak stays "alive" through the day
     * after the last activity; once two days have passed with no play it reads 0.
     */
    fun displayStreak(info: StreakInfo, today: Long): Int = when {
        info.lastActiveEpochDay < 0L -> 0
        today <= info.lastActiveEpochDay -> info.currentStreakDays
        today == info.lastActiveEpochDay + 1 -> info.currentStreakDays
        else -> 0
    }
}
