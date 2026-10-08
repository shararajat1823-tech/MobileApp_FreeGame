package com.miniplay.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.common.Clock
import com.miniplay.app.core.game.GameRegistry
import com.miniplay.app.domain.model.DailyChallenge
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.GameStats
import com.miniplay.app.domain.model.StreakInfo
import com.miniplay.app.domain.repository.DailyChallengeRepository
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.domain.repository.StreakRepository
import com.miniplay.app.domain.usecase.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A game plus its best score, ready for a card. */
data class GameCardUi(
    val metadata: GameMetadata,
    val bestScore: Long?,
)

/** One day cell in the weekly streak strip. */
data class StreakDay(
    val label: String,
    val active: Boolean,
    val isToday: Boolean,
)

data class HomeUiState(
    val loading: Boolean = true,
    val allGames: List<GameCardUi> = emptyList(),
    val popular: List<GameCardUi> = emptyList(),
    val lastPlayed: GameCardUi? = null,
    val daily: DailyChallenge? = null,
    val dailyGame: GameMetadata? = null,
    val streakDays: Int = 0,
    val bestStreak: Int = 0,
    val week: List<StreakDay> = emptyList(),
)

/**
 * Backs the home dashboard by merging the game catalogue with live stats, the
 * last-played pointer, today's challenge and the streak. Everything is derived
 * reactively so the dashboard updates the instant a game is played.
 */
class HomeViewModel(
    private val registry: GameRegistry,
    statsRepository: GameStatsRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    streakRepository: StreakRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        statsRepository.observeAllStats(),
        statsRepository.observeLastPlayedGameId(),
        dailyChallengeRepository.observeToday(),
        streakRepository.observeStreak(),
    ) { stats, lastPlayedId, daily, streak ->
        buildState(stats, lastPlayedId, daily, streak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun buildState(
        stats: List<GameStats>,
        lastPlayedId: String?,
        daily: DailyChallenge,
        streak: StreakInfo,
    ): HomeUiState {
        val bestById = stats.associate { it.gameId to it.bestScore }
        fun cardOf(meta: GameMetadata) = GameCardUi(meta, bestById[meta.id])

        val allGames = registry.all.map(::cardOf)
        val popular = registry.popular(limit = 5).map(::cardOf)
        val lastPlayed = lastPlayedId
            ?.let { registry.metadata(it) }
            ?.let(::cardOf)

        val today = clock.todayEpochDay()
        val displayStreak = StreakCalculator.displayStreak(streak, today)

        return HomeUiState(
            loading = false,
            allGames = allGames,
            popular = popular,
            lastPlayed = lastPlayed,
            daily = daily,
            dailyGame = registry.metadata(daily.gameId),
            streakDays = displayStreak,
            bestStreak = streak.bestStreak(displayStreak),
            week = weekStrip(streak, displayStreak, today),
        )
    }

    private fun StreakInfo.bestStreak(display: Int): Int = maxOf(bestStreakDays, display)

    /** Builds the last-7-days strip ending today, marking days inside the live streak. */
    private fun weekStrip(streak: StreakInfo, displayStreak: Int, today: Long): List<StreakDay> {
        val activeFrom = if (displayStreak > 0) streak.lastActiveEpochDay - (displayStreak - 1) else Long.MAX_VALUE
        val activeTo = streak.lastActiveEpochDay
        return (6 downTo 0).map { offset ->
            val day = today - offset
            StreakDay(
                label = WEEKDAY_LETTERS[dayOfWeekMonday0(day)],
                active = displayStreak > 0 && day in activeFrom..activeTo,
                isToday = day == today,
            )
        }
    }

    private fun dayOfWeekMonday0(epochDay: Long): Int = (((epochDay + 3) % 7) + 7).toInt() % 7

    private companion object {
        val WEEKDAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
    }
}
