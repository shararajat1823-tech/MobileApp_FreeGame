package com.miniplay.app.core.common

import java.util.TimeZone

/**
 * Time source, injected so streak/daily-challenge logic is testable. [todayEpochDay]
 * is computed without java.time to stay compatible with minSdk 24 and no desugaring.
 */
interface Clock {
    fun nowMillis(): Long
    fun todayEpochDay(): Long
}

class SystemClock(
    private val timeZoneProvider: () -> TimeZone = { TimeZone.getDefault() },
) : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun todayEpochDay(): Long {
        val now = nowMillis()
        val offset = timeZoneProvider().getOffset(now)
        return Math.floorDiv(now + offset, MILLIS_PER_DAY)
    }

    companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
