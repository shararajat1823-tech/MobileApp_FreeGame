package com.miniplay.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.EmptyState
import com.miniplay.app.di.LocalAppContainer

/**
 * Resolves a game id to its [com.miniplay.app.core.game.GameDescriptor] and shows
 * the game. Unknown ids fall back to a friendly empty state instead of crashing,
 * so a stale deep link can never take the app down.
 */
@Composable
fun GameHost(gameId: String, onExit: () -> Unit) {
    val container = LocalAppContainer.current
    val descriptor = container.gameRegistry.descriptor(gameId)
    if (descriptor != null) {
        descriptor.Screen(onExit)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                emoji = "🕹️",
                title = stringResource(R.string.game_unknown_title),
                message = stringResource(R.string.game_not_found),
            )
        }
    }
}
