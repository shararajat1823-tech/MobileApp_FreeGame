package com.miniplay.app.core.game

import androidx.compose.runtime.Composable
import com.miniplay.app.domain.model.GameMetadata

/**
 * The contract every game implements to plug into the hub. A descriptor couples
 * a game's [metadata] (what the catalogue shows) with its [Screen] (how it
 * plays). The registry discovers games purely through this interface, so adding
 * a game = create one `GameDescriptor` + register it. Nothing else in the app
 * needs to change.
 */
interface GameDescriptor {
    val metadata: GameMetadata

    /**
     * The game's full-screen UI. [onExit] returns to wherever the player came
     * from. Screens obtain their dependencies from
     * [com.miniplay.app.di.LocalAppContainer], keeping this signature stable.
     */
    @Composable
    fun Screen(onExit: () -> Unit)
}
