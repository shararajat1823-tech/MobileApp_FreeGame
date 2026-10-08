package com.miniplay.app.domain.model

import androidx.annotation.StringRes

/**
 * Describes a game to the rest of the app. This is the single contract the home
 * screen, catalogue and profile use to render any game, so adding a new game is
 * mostly a matter of supplying one of these (plus a screen — see
 * `core.game.GameDescriptor`).
 *
 * It intentionally holds [StringRes] ids rather than literal strings so copy
 * stays in `strings.xml` and localisable. The id is a stable key used
 * everywhere scores and stats are persisted — never reuse or rename it.
 */
data class GameMetadata(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val howToPlayRes: Int,
    val iconEmoji: String,
    val category: GameCategory,
    val difficulties: List<GameDifficulty>,
    val scoreDirection: ScoreDirection,
    /** Whether a per-game "best score" is meaningful (e.g. Tic-Tac-Toe tracks wins, not a score). */
    val tracksHighScore: Boolean = true,
    /** Relative ordering/weighting used to pick the "popular" rail on home. */
    val popularity: Int = 0,
) {
    val supportsDifficulty: Boolean get() = difficulties.size > 1
}
