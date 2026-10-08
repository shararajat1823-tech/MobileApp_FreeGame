package com.miniplay.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.common.Clock
import com.miniplay.app.core.game.GameRegistry
import com.miniplay.app.domain.model.AppSettings
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.PlayerProfile
import com.miniplay.app.domain.model.PlayerSummary
import com.miniplay.app.domain.model.ThemeMode
import com.miniplay.app.domain.repository.AchievementRepository
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.domain.repository.ProfileRepository
import com.miniplay.app.domain.repository.SettingsRepository
import com.miniplay.app.domain.repository.StreakRepository
import com.miniplay.app.domain.usecase.StreakCalculator
import com.miniplay.app.feature.home.GameCardUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: PlayerProfile = PlayerProfile("Player", 0, 0L),
    val settings: AppSettings = AppSettings.DEFAULT,
    val summary: PlayerSummary = PlayerSummary(),
    val favouriteGame: GameMetadata? = null,
    val perGame: List<GameCardUi> = emptyList(),
)

/**
 * Profile + settings screen state. Aggregates all per-game stats into a
 * [PlayerSummary], exposes editing of the local identity, and mediates settings
 * writes and the "reset all progress" action.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    statsRepository: GameStatsRepository,
    streakRepository: StreakRepository,
    achievementRepository: AchievementRepository,
    private val registry: GameRegistry,
    private val clock: Clock,
    private val resetAllProgress: suspend () -> Unit,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        settingsRepository.observeSettings(),
        statsRepository.observeAllStats(),
        streakRepository.observeStreak(),
        achievementRepository.observeAchievements(),
    ) { profile, settings, stats, streak, achievements ->
        val played = stats.filter { it.hasBeenPlayed }
        val favouriteId = played.maxByOrNull { it.playCount }?.gameId
        val summary = PlayerSummary(
            gamesPlayed = stats.sumOf { it.playCount },
            totalWins = stats.sumOf { it.wins },
            bestScore = stats.mapNotNull { it.bestScore }.maxOrNull() ?: 0L,
            totalPlayTimeMillis = stats.sumOf { it.totalPlayTimeMillis },
            favouriteGameId = favouriteId,
            currentStreakDays = StreakCalculator.displayStreak(streak, clock.todayEpochDay()),
            bestStreakDays = streak.bestStreakDays,
            achievementsUnlocked = achievements.count { it.unlocked },
            achievementsTotal = achievements.size,
        )
        val perGame = played
            .sortedByDescending { it.playCount }
            .mapNotNull { s -> registry.metadata(s.gameId)?.let { GameCardUi(it, s.bestScore) } }

        ProfileUiState(
            loading = false,
            profile = profile,
            settings = settings,
            summary = summary,
            favouriteGame = favouriteId?.let { registry.metadata(it) },
            perGame = perGame,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun updateNickname(name: String) = viewModelScope.launch { profileRepository.updateNickname(name) }
    fun updateAvatar(id: Int) = viewModelScope.launch { profileRepository.updateAvatar(id) }

    fun setSound(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    fun setMusic(enabled: Boolean) = viewModelScope.launch { settingsRepository.setMusicEnabled(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }

    fun resetProgress() = viewModelScope.launch { resetAllProgress() }
}
