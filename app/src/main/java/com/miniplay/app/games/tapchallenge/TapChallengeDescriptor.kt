package com.miniplay.app.games.tapchallenge

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Tap Challenge with the hub. */
object TapChallengeDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.TAP_CHALLENGE,
        titleRes = R.string.tap_title,
        descriptionRes = R.string.tap_desc,
        howToPlayRes = R.string.tap_how_to,
        iconEmoji = "🎯",
        category = GameCategory.REFLEX,
        difficulties = listOf(GameDifficulty.MEDIUM),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 72,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = TapScreen(onExit)
}
