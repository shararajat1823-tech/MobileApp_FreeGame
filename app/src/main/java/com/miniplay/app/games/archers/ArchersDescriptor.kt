package com.miniplay.app.games.archers

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Ragdoll Archers with the hub. */
object ArchersDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.ARCHERS,
        titleRes = R.string.archers_title,
        descriptionRes = R.string.archers_desc,
        howToPlayRes = R.string.archers_how_to,
        iconEmoji = "🏹",
        category = GameCategory.ARCADE,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = false, // win/loss, not a numeric score
        popularity = 90,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = ArchersScreen(onExit)
}
