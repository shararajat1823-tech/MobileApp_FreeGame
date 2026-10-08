package com.miniplay.app.domain.model

/**
 * Difficulty levels shared across games. Not every game supports every level —
 * each game declares the subset it offers in its [GameMetadata].
 */
enum class GameDifficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        val DEFAULT = MEDIUM
    }
}
