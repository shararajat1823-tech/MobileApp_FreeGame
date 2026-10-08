package com.miniplay.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miniplay.app.R

/** One entry in a game's overflow menu. */
data class GameMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Shared chrome for every game screen: a slim top bar with Back, the game title
 * and an overflow menu (How to play + the supplied actions). Keeping this in one
 * place means all games navigate and surface help identically.
 */
@Composable
fun GameScaffold(
    title: String,
    onExit: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
    menuItems: List<GameMenuItem> = emptyList(),
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        GameTopBar(
            title = title,
            onExit = onExit,
            onHowToPlay = onHowToPlay,
            menuItems = menuItems,
        )
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun GameTopBar(
    title: String,
    onExit: () -> Unit,
    onHowToPlay: () -> Unit,
    menuItems: List<GameMenuItem>,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onExit) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.game_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.game_menu))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.game_how_to_play)) },
                    onClick = { menuOpen = false; onHowToPlay() },
                )
                menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        leadingIcon = { Icon(item.icon, contentDescription = null) },
                        onClick = { menuOpen = false; item.onClick() },
                    )
                }
            }
        }
    }
}
