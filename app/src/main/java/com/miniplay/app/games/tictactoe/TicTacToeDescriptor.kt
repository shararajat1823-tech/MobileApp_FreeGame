package com.miniplay.app.games.tictactoe

import androidx.compose.runtime.Composable
import com.miniplay.app.R
import com.miniplay.app.core.game.GameDescriptor
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.model.ScoreDirection

/** Registers Tic-Tac-Toe with the hub. */
object TicTacToeDescriptor : GameDescriptor {
    override val metadata = GameMetadata(
        id = GameIds.TIC_TAC_TOE,
        titleRes = R.string.ttt_title,
        descriptionRes = R.string.ttt_desc,
        howToPlayRes = R.string.ttt_how_to,
        iconEmoji = "⭕",
        category = GameCategory.STRATEGY,
        difficulties = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
        scoreDirection = ScoreDirection.HIGHER_IS_BETTER,
        tracksHighScore = false, // win/draw counters, not a numeric score
        popularity = 80,
    )

    @Composable
    override fun Screen(onExit: () -> Unit) = TicTacToeScreen(onExit)
}
