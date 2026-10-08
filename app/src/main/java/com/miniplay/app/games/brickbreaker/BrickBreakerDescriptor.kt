package com.miniplay.app.games.brickbreaker

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Brick Breaker with the hub. */
object BrickBreakerDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.BRICK_BREAKER,
        titleRes = R.string.brick_title,
        descriptionRes = R.string.brick_desc,
        howToPlayRes = R.string.brick_how_to,
        iconEmoji = "🧱",
        category = GameCategory.ARCADE,
        difficulties = listOf(GameDifficulty.MEDIUM),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 65,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = BrickBreakerScreen(onExit)
}
