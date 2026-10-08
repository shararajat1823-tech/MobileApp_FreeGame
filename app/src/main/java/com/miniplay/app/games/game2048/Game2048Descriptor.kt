package com.miniplay.app.games.game2048

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers 2048 with the hub. */
object Game2048Descriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.GAME_2048,
        titleRes = R.string.g2048_title,
        descriptionRes = R.string.g2048_desc,
        howToPlayRes = R.string.g2048_how_to,
        iconEmoji = "🔢",
        category = GameCategory.PUZZLE,
        difficulties = listOf(GameDifficulty.MEDIUM),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 90,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = Game2048Screen(onExit)
}
