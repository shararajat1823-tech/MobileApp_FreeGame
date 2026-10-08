package com.miniplay.app.games.minesweeper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.DifficultySelector
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.formatDuration
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameDifficulty

@Composable
fun MinesweeperScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        MinesweeperViewModel(container.recordGameResult, container.soundManager, container.hapticManager, container.gameStatsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.mine_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::newGame),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                DifficultySelector(
                    options = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
                    selected = state.difficulty,
                    onSelected = viewModel::setDifficulty,
                )
                MiniPlayCard(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InlineStat("💣 ${state.game.minesRemaining}", stringResource(R.string.mine_remaining))
                        InlineStat(formatDuration(state.elapsedMillis), stringResource(R.string.game_time))
                    }
                }
                Board(
                    state = state.game,
                    onReveal = viewModel::onReveal,
                    onFlag = viewModel::onFlag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            GameResultOverlay(
                visible = state.finished,
                emoji = if (state.won) "🎉" else "💣",
                title = stringResource(if (state.won) R.string.mine_cleared else R.string.mine_boom),
                primaryValue = (state.finalScore ?: 0).toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                isNewBest = state.won && (state.finalScore ?: 0).toLong() >= state.bestScore && state.bestScore > 0,
                onPlayAgain = viewModel::newGame,
                onExit = onExit,
                secondary = {
                    Text(
                        formatDuration(state.elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "💣",
            title = stringResource(R.string.mine_title),
            body = stringResource(R.string.mine_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun Board(
    state: MinesweeperState,
    onReveal: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (r in 0 until state.rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0 until state.cols) {
                    val index = state.index(r, c)
                    MineTile(
                        cell = state.cells[index],
                        lost = state.status == MineStatus.LOST,
                        row = r,
                        col = c,
                        onReveal = { onReveal(index) },
                        onFlag = { onFlag(index) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MineTile(
    cell: MineCell,
    lost: Boolean,
    row: Int,
    col: Int,
    onReveal: () -> Unit,
    onFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hiddenColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val revealedColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val mineColor = MaterialTheme.colorScheme.errorContainer

    val stateDesc = when {
        cell.flagged -> stringResource(R.string.mine_cell_flagged)
        !cell.revealed -> stringResource(R.string.mine_cell_hidden)
        cell.isMine -> stringResource(R.string.mine_boom)
        cell.adjacent == 0 -> stringResource(R.string.mine_cell_empty)
        else -> stringResource(R.string.mine_cell_number, cell.adjacent)
    }
    val desc = stringResource(R.string.mine_cell_description, row + 1, col + 1, stateDesc)

    val background = when {
        !cell.revealed -> hiddenColor
        cell.isMine -> mineColor
        else -> revealedColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .then(
                if (!cell.revealed) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                else Modifier,
            )
            .combinedClickable(
                enabled = !cell.revealed,
                onClick = { if (!cell.flagged) onReveal() },
                onLongClick = onFlag,
            )
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.flagged && !cell.revealed -> Text("🚩", style = MaterialTheme.typography.titleMedium)
            !cell.revealed -> Unit
            cell.isMine -> Text("💣", style = MaterialTheme.typography.titleMedium)
            cell.adjacent > 0 -> Text(
                cell.adjacent.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = numberColor(cell.adjacent),
            )
            else -> Unit
        }
    }
}

/** Classic Minesweeper number colours, tuned to read in light and dark. */
@Composable
private fun numberColor(n: Int): Color = when (n) {
    1 -> Color(0xFF3B82F6)
    2 -> Color(0xFF16A34A)
    3 -> Color(0xFFEF4444)
    4 -> Color(0xFF7C3AED)
    5 -> Color(0xFFF59E0B)
    6 -> Color(0xFF0D9488)
    7 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
