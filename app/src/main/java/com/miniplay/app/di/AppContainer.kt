package com.miniplay.app.di

import android.content.Context
import com.miniplay.app.R
import com.miniplay.app.core.ads.AdManager
import com.miniplay.app.core.ads.NoOpAdManager
import com.miniplay.app.core.analytics.Analytics
import com.miniplay.app.core.analytics.LogcatAnalytics
import com.miniplay.app.core.audio.SoundManager
import com.miniplay.app.core.audio.ToneSoundManager
import com.miniplay.app.core.common.Clock
import com.miniplay.app.core.common.SystemClock
import com.miniplay.app.core.game.GameRegistry
import com.miniplay.app.core.haptics.HapticManager
import com.miniplay.app.core.haptics.SystemHapticManager
import com.miniplay.app.data.local.MiniPlayDatabase
import com.miniplay.app.data.local.datastore.miniPlayDataStore
import com.miniplay.app.data.repository.AchievementRepositoryImpl
import com.miniplay.app.data.repository.DailyChallengeRepositoryImpl
import com.miniplay.app.data.repository.DisabledCloudSyncRepository
import com.miniplay.app.data.repository.DisabledLeaderboardRepository
import com.miniplay.app.data.repository.DisabledUserRepository
import com.miniplay.app.data.repository.GameFactsRepositoryImpl
import com.miniplay.app.data.repository.GameStatsRepositoryImpl
import com.miniplay.app.data.repository.NoOpRemoteAnalyticsSink
import com.miniplay.app.data.repository.ProfileRepositoryImpl
import com.miniplay.app.data.repository.SettingsRepositoryImpl
import com.miniplay.app.data.repository.StreakRepositoryImpl
import com.miniplay.app.domain.repository.AchievementRepository
import com.miniplay.app.domain.repository.CloudSyncRepository
import com.miniplay.app.domain.repository.DailyChallengeRepository
import com.miniplay.app.domain.repository.GameFactsRepository
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.domain.repository.LeaderboardRepository
import com.miniplay.app.domain.repository.ProfileRepository
import com.miniplay.app.domain.repository.SettingsRepository
import com.miniplay.app.domain.repository.StreakRepository
import com.miniplay.app.domain.repository.UserRepository
import com.miniplay.app.domain.usecase.RecordGameResultUseCase
import com.miniplay.app.games.brickbreaker.BrickBreakerDescriptor
import com.miniplay.app.games.game2048.Game2048Descriptor
import com.miniplay.app.games.memorymatch.MemoryMatchDescriptor
import com.miniplay.app.games.minesweeper.MinesweeperDescriptor
import com.miniplay.app.games.numberpuzzle.NumberPuzzleDescriptor
import com.miniplay.app.games.reaction.ReactionDescriptor
import com.miniplay.app.games.snake.SnakeDescriptor
import com.miniplay.app.games.tapchallenge.TapChallengeDescriptor
import com.miniplay.app.games.tictactoe.TicTacToeDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency-injection container. One instance lives on the
 * [com.miniplay.app.MiniPlayApplication] and is handed to the UI via
 * [LocalAppContainer].
 *
 * A deliberate choice over Hilt: the graph is small and entirely local, and
 * manual wiring removes an annotation processor (and its Kotlin/KSP version
 * coupling) from the build — one fewer thing that can break a clean checkout.
 * Everything here is a lazily constructed singleton.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** App-lifetime scope for cross-cutting collectors (e.g. settings → services). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock: Clock = SystemClock()

    // --- Game catalogue (also the domain GameCatalog). Add new games here. ---
    val gameRegistry: GameRegistry = GameRegistry(
        listOf(
            TicTacToeDescriptor,
            MemoryMatchDescriptor,
            ReactionDescriptor,
            NumberPuzzleDescriptor,
            Game2048Descriptor,
            BrickBreakerDescriptor,
            TapChallengeDescriptor,
            SnakeDescriptor,
            MinesweeperDescriptor,
        ),
    )

    // --- Platform services ---
    val soundManager: SoundManager = ToneSoundManager()
    val hapticManager: HapticManager = SystemHapticManager(appContext)
    val adManager: AdManager = NoOpAdManager()
    val analytics: Analytics = LogcatAnalytics()

    // --- Persistence ---
    private val database: MiniPlayDatabase = MiniPlayDatabase.build(appContext)
    private val dataStore = appContext.miniPlayDataStore

    // --- Online features (disabled in v1) ---
    val leaderboardRepository: LeaderboardRepository = DisabledLeaderboardRepository()
    val userRepository: UserRepository = DisabledUserRepository()
    val cloudSyncRepository: CloudSyncRepository = DisabledCloudSyncRepository()
    private val remoteAnalyticsSink = NoOpRemoteAnalyticsSink()

    // --- Repositories ---
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(dataStore)
    val profileRepository: ProfileRepository = ProfileRepositoryImpl(
        dataStore = dataStore,
        clock = clock,
        defaultNickname = appContext.getString(R.string.profile_default_name),
    )
    val streakRepository: StreakRepository = StreakRepositoryImpl(dataStore)
    val gameFactsRepository: GameFactsRepository = GameFactsRepositoryImpl(dataStore)
    val dailyChallengeRepository: DailyChallengeRepository =
        DailyChallengeRepositoryImpl(dataStore, clock)
    val gameStatsRepository: GameStatsRepository =
        GameStatsRepositoryImpl(database.gameStatsDao(), dataStore, gameRegistry)
    val achievementRepository: AchievementRepository =
        AchievementRepositoryImpl(database.achievementStateDao())

    /** Snake-local settings + per-mode best scores (does not touch the global schema). */
    val snakePrefs = com.miniplay.app.games.snake.SnakePrefs(dataStore)

    // --- Use cases ---
    val recordGameResult: RecordGameResultUseCase = RecordGameResultUseCase(
        statsRepository = gameStatsRepository,
        streakRepository = streakRepository,
        dailyChallengeRepository = dailyChallengeRepository,
        achievementRepository = achievementRepository,
        gameFactsRepository = gameFactsRepository,
        analyticsSink = remoteAnalyticsSink,
        clock = clock,
    )

    /** Starts app-lifetime collectors. Called once from Application.onCreate. */
    fun start() {
        applicationScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                soundManager.setEnabled(settings.soundEnabled)
                hapticManager.setEnabled(settings.hapticsEnabled)
            }
        }
    }

    /** Wipes gameplay progress (keeps the local profile). Backs "Reset all progress". */
    suspend fun resetAllProgress() {
        gameStatsRepository.resetAll()
        achievementRepository.resetAll()
        streakRepository.reset()
        dailyChallengeRepository.reset()
        gameFactsRepository.reset()
    }
}
