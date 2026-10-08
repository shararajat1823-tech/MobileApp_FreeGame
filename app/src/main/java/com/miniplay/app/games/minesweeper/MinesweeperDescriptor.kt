package com.miniplay.app.games.minesweeper

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

object MinesweeperDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.MINESWEEPER,
        titleRes = R.string.mine_title,
        descriptionRes = R.string.mine_desc,
        howToPlayRes = R.string.mine_how_to,
        iconEmoji = "💣",
        category = GameCategory.PUZZLE,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = true,
        popularity = 68,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = MinesweeperScreen(onExit)
}
