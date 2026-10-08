package com.miniplay.app.games.numberpuzzle

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers the sliding Number Puzzle with the hub. */
object NumberPuzzleDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.NUMBER_PUZZLE,
        titleRes = R.string.puzzle_title,
        descriptionRes = R.string.puzzle_desc,
        howToPlayRes = R.string.puzzle_how_to,
        iconEmoji = "🧩",
        category = GameCategory.PUZZLE,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.LOWER_IS_BETTER, // best = fewest moves
        tracksHighScore = true,
        popularity = 50,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = PuzzleScreen(onExit)
}
