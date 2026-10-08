package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.GameMetadata

/**
 * Read-only directory of all games' metadata. The presentation-layer game
 * registry supplies the concrete list; domain/data code depends only on this
 * interface so it never reaches "up" into Compose screens.
 */
interface GameCatalog {
    val all: List<GameMetadata>
    fun metadata(gameId: String): GameMetadata?
}
