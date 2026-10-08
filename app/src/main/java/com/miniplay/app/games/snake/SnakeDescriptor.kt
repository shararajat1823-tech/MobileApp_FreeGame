package com.miniplay.app.games.snake

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

object SnakeDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.SNAKE,
        titleRes = R.string.snake_title,
        descriptionRes = R.string.snake_desc,
        howToPlayRes = R.string.snake_how_to,
        iconEmoji = "🐍",
        category = GameCategory.ARCADE,
        difficulties = listOf(GameDifficulty.MEDIUM),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 85,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = SnakeScreen(onExit)
}
