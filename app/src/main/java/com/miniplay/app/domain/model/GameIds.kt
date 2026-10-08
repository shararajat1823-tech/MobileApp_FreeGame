package com.miniplay.app.domain.model

/**
 * Canonical game id constants. These strings are persistence keys (scores,
 * stats, last-played), so they must never change once shipped. Each game's
 * descriptor uses its constant from here; nothing hard-codes the raw string.
 */
object GameIds {
    const val TIC_TAC_TOE = "tictactoe"
    const val MEMORY_MATCH = "memorymatch"
    const val REACTION = "reaction"
    const val NUMBER_PUZZLE = "numberpuzzle"
    const val GAME_2048 = "game2048"
    const val BRICK_BREAKER = "brickbreaker"
    const val TAP_CHALLENGE = "tapchallenge"
    const val SNAKE = "snake"
    const val MINESWEEPER = "minesweeper"
}
