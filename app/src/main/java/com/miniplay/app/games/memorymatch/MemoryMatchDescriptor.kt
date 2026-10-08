package com.miniplay.app.games.memorymatch

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Memory Match with the hub. */
object MemoryMatchDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.MEMORY_MATCH,
        titleRes = R.string.memory_title,
        descriptionRes = R.string.memory_desc,
        howToPlayRes = R.string.memory_how_to,
        iconEmoji = "🧠",
        category = GameCategory.MEMORY,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 70,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = MemoryScreen(onExit)
}
