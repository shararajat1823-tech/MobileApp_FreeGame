package com.miniplay.app.games.reaction

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers the Reaction Test with the hub. */
object ReactionDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.REACTION,
        titleRes = R.string.reaction_title,
        descriptionRes = R.string.reaction_desc,
        howToPlayRes = R.string.reaction_how_to,
        iconEmoji = "⚡",
        category = GameCategory.REFLEX,
        difficulties = listOf(GameDifficulty.MEDIUM),
        scoreDirection = ScoreDirection.LOWER_IS_BETTER,
        tracksHighScore = true,
        popularity = 60,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = ReactionScreen(onExit)
}
