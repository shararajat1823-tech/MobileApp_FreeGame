package com.miniplay.app.games.ludo

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Ludo with the hub. */
object LudoDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.LUDO,
        titleRes = R.string.ludo_title,
        descriptionRes = R.string.ludo_desc,
        howToPlayRes = R.string.ludo_how_to,
        iconEmoji = "🎲",
        category = GameCategory.STRATEGY,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = false, // win/loss, not a numeric score
        popularity = 88,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = LudoScreen(onExit)
}
