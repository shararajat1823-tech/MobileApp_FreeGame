package com.miniplay.app.domain.usecase

import com.miniplay.app.R
import com.miniplay.app.domain.model.AchievementCondition
import com.miniplay.app.domain.model.AchievementDefinition
import com.miniplay.app.domain.model.GameIds

/**
 * The master list of achievements. Adding one is a single entry here — the
 * engine, repository and UI all discover it from this list. Ids are stable
 * persistence keys.
 */
object AchievementCatalog {

    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_game",
            titleRes = R.string.ach_first_game_title,
            descriptionRes = R.string.ach_first_game_desc,
            emoji = "🎮",
            target = 1,
            condition = AchievementCondition.TotalGamesPlayed(1),
        ),
        AchievementDefinition(
            id = "ten_games",
            titleRes = R.string.ach_ten_games_title,
            descriptionRes = R.string.ach_ten_games_desc,
            emoji = "🔥",
            target = 10,
            condition = AchievementCondition.TotalGamesPlayed(10),
        ),
        AchievementDefinition(
            id = "hundred_games",
            titleRes = R.string.ach_hundred_games_title,
            descriptionRes = R.string.ach_hundred_games_desc,
            emoji = "💯",
            target = 100,
            condition = AchievementCondition.TotalGamesPlayed(100),
        ),
        AchievementDefinition(
            id = "champion",
            titleRes = R.string.ach_champion_title,
            descriptionRes = R.string.ach_champion_desc,
            emoji = "👑",
            target = 10,
            condition = AchievementCondition.TotalWins(10),
        ),
        AchievementDefinition(
            id = "lightning",
            titleRes = R.string.ach_lightning_title,
            descriptionRes = R.string.ach_lightning_desc,
            emoji = "⚡",
            target = 1,
            condition = AchievementCondition.ReactionUnderMs(200),
        ),
        AchievementDefinition(
            id = "memory_master",
            titleRes = R.string.ach_memory_master_title,
            descriptionRes = R.string.ach_memory_master_desc,
            emoji = "🧠",
            target = 1,
            condition = AchievementCondition.HardCompleted(GameIds.MEMORY_MATCH),
        ),
        AchievementDefinition(
            id = "perfect",
            titleRes = R.string.ach_perfect_title,
            descriptionRes = R.string.ach_perfect_desc,
            emoji = "🎯",
            target = 1,
            condition = AchievementCondition.AnyFlawless,
        ),
        AchievementDefinition(
            id = "streak_5",
            titleRes = R.string.ach_streak_title,
            descriptionRes = R.string.ach_streak_desc,
            emoji = "📅",
            target = 5,
            condition = AchievementCondition.StreakReached(5),
        ),
        AchievementDefinition(
            id = "reach_2048",
            titleRes = R.string.ach_2048_title,
            descriptionRes = R.string.ach_2048_desc,
            emoji = "🏆",
            target = 1,
            condition = AchievementCondition.ScoreReached(GameIds.GAME_2048, 2048),
        ),
    )

    val total: Int get() = all.size
}
