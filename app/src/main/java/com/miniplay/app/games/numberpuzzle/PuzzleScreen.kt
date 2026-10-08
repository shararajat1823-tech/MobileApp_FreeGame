package com.miniplay.app.games.numberpuzzle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.formatDuration
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameIds

@Composable
fun PuzzleScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        PuzzleViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.gameStatsRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.puzzle_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.puzzle_shuffle), Icons.Rounded.Shuffle, viewModel::shuffle),
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::newGame),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SizeToggle(size = state.size, onSizeSelected = viewModel::setSize)

                StatsRow(state)

                Board(
                    state = state,
                    onTileClick = viewModel::onTileClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .aspectRatio(1f),
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GhostButton(
                        text = stringResource(R.string.puzzle_shuffle),
                        onClick = viewModel::shuffle,
                        leadingIcon = Icons.Rounded.Shuffle,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.game_new_game),
                        onClick = viewModel::newGame,
                        leadingIcon = Icons.Rounded.Refresh,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ResultOverlay(state, onPlayAgain = viewModel::newGame, onExit = onExit)
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🧩",
            title = stringResource(R.string.puzzle_title),
            body = stringResource(R.string.puzzle_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun SizeToggle(size: Int, onSizeSelected: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SizeChip("3 × 3", size == 3, Modifier.weight(1f)) { onSizeSelected(3) }
        SizeChip("4 × 4", size == 4, Modifier.weight(1f)) { onSizeSelected(4) }
    }
}

@Composable
private fun SizeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "sizeBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "sizeFg",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .bounceClick(pressedScale = 0.97f, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun StatsRow(state: PuzzleUiState) {
    MiniPlayCard(modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            InlineStat(state.moves.toString(), stringResource(R.string.game_moves))
            InlineStat(formatDuration(state.elapsedMillis), stringResource(R.string.game_time))
            InlineStat(state.bestMoves?.toString() ?: "–", stringResource(R.string.game_best))
        }
    }
}

@Composable
private fun Board(
    state: PuzzleUiState,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(GameIds.NUMBER_PUZZLE)
    val size = state.size
    val tiles = state.state.tiles
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(8.dp),
    ) {
        val cell = maxWidth / size
        // Render each tile by its stable VALUE so Compose animates a moving tile
        // to its new cell rather than swapping two tiles in place.
        for (value in 1 until size * size) {
            key(value) {
                val index = tiles.indexOf(value)
                val row = index / size
                val col = index % size
                val x by animateDpAsState(
                    targetValue = cell * col,
                    animationSpec = Motion.gentleSpring(),
                    label = "tileX",
                )
                val y by animateDpAsState(
                    targetValue = cell * row,
                    animationSpec = Motion.gentleSpring(),
                    label = "tileY",
                )
                Box(
                    Modifier
                        .offset(x = x, y = y)
                        .size(cell)
                        .padding(4.dp),
                ) {
                    Tile(
                        value = value,
                        accent = accent,
                        enabled = !state.solved,
                        onClick = { onTileClick(state.state.tiles.indexOf(value)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(
    value: Int,
    accent: GameAccent,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.puzzle_tile_description, value)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.brush())
            .bounceClick(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = accent.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultOverlay(state: PuzzleUiState, onPlayAgain: () -> Unit, onExit: () -> Unit) {
    val best = state.bestMoves
    GameResultOverlay(
        visible = state.solved,
        emoji = "🧩",
        title = stringResource(R.string.puzzle_solved),
        primaryValue = state.moves.toString(),
        primaryLabel = stringResource(R.string.game_moves),
        onPlayAgain = onPlayAgain,
        onExit = onExit,
        isNewBest = best != null && state.moves.toLong() <= best,
        secondary = {
            InlineStat(formatDuration(state.elapsedMillis), stringResource(R.string.game_time))
        },
    )
}
