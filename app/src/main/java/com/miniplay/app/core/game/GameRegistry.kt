package com.miniplay.app.core.game

import com.miniplay.app.domain.model.GameMetadata
import com.miniplay.app.domain.repository.GameCatalog

/**
 * The single source of truth for which games exist. [GameRegistry] both drives
 * the UI (via [descriptors]) and satisfies the domain [GameCatalog] contract
 * (via metadata), so data-layer code can look up score direction without
 * depending on Compose.
 *
 * To add a game: implement a [GameDescriptor] in its own package and add it to
 * the list passed here (see `di.AppContainer`). That is the only wiring change.
 */
class GameRegistry(
    val descriptors: List<GameDescriptor>,
) : GameCatalog {

    private val byId: Map<String, GameDescriptor> =
        descriptors.associateBy { it.metadata.id }

    override val all: List<GameMetadata> = descriptors.map { it.metadata }

    override fun metadata(gameId: String): GameMetadata? = byId[gameId]?.metadata

    fun descriptor(gameId: String): GameDescriptor? = byId[gameId]

    /** The games shown on the home "Popular" rail, highest popularity first. */
    fun popular(limit: Int): List<GameMetadata> =
        all.sortedByDescending { it.popularity }.take(limit)
}
