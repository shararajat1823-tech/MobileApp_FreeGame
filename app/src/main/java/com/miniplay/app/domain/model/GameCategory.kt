package com.miniplay.app.domain.model

/**
 * Broad grouping used for filtering in the games catalogue. Pure domain — the
 * UI maps each entry to a localised label and icon.
 */
enum class GameCategory {
    PUZZLE,
    ARCADE,
    STRATEGY,
    REFLEX,
    MEMORY,
}
