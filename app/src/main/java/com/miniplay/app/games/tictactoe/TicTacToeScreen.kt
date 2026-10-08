package com.miniplay.app.games.tictactoe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameDifficulty
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TicTacToeScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        TicTacToeViewModel(container.recordGameResult, container.soundManager, container.hapticManager)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.ttt_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::newGame),
            GameMenuItem(stringResource(R.string.ttt_reset_scores), Icons.Rounded.Replay, viewModel::resetScores),
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
                ModeToggle(state.mode, onModeSelected = viewModel::setMode)

                if (state.mode == TicTacToeMode.VS_COMPUTER) {
                    DifficultySelector(
                        options = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
                        selected = state.difficulty,
                        onSelected = viewModel::setDifficulty,
                    )
                }

                Scoreboard(state)
                StatusText(state)

                Board(
                    state = state,
                    onCellClick = viewModel::onCellClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .aspectRatio(1f),
                )

                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    text = stringResource(R.string.game_new_game),
                    onClick = viewModel::newGame,
                    leadingIcon = Icons.Rounded.Refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(bottom = 20.dp),
                )
            }

            ResultOverlay(state, onPlayAgain = viewModel::newGame, onExit = onExit)
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "⭕",
            title = stringResource(R.string.ttt_title),
            body = stringResource(R.string.ttt_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun ModeToggle(mode: TicTacToeMode, onModeSelected: (TicTacToeMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeChip(stringResource(R.string.ttt_mode_pvp), mode == TicTacToeMode.TWO_PLAYER, Modifier.weight(1f)) {
            onModeSelected(TicTacToeMode.TWO_PLAYER)
        }
        ModeChip(stringResource(R.string.ttt_mode_ai), mode == TicTacToeMode.VS_COMPUTER, Modifier.weight(1f)) {
            onModeSelected(TicTacToeMode.VS_COMPUTER)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "modeBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "modeFg",
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
private fun Scoreboard(state: TicTacToeUiState) {
    MiniPlayCard(modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            InlineStat(state.xWins.toString(), stringResource(R.string.ttt_wins_x))
            InlineStat(state.draws.toString(), stringResource(R.string.ttt_draws))
            InlineStat(state.oWins.toString(), stringResource(R.string.ttt_wins_o))
        }
    }
}

@Composable
private fun StatusText(state: TicTacToeUiState) {
    val text = when {
        state.aiThinking -> stringResource(R.string.ttt_thinking)
        state.game.isFinished -> ""
        state.game.currentPlayer == Player.X -> stringResource(R.string.ttt_turn_x)
        else -> stringResource(R.string.ttt_turn_o)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Board(
    state: TicTacToeUiState,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val winning = state.game.winningLine.orEmpty().toSet()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in 0 until 3) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Cell(
                        player = state.game.board[index],
                        isWinning = index in winning,
                        enabled = !state.game.isFinished && !state.aiThinking,
                        row = row,
                        col = col,
                        onClick = { onCellClick(index) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(
    player: Player?,
    isWinning: Boolean,
    enabled: Boolean,
    row: Int,
    col: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val xColor = MaterialTheme.colorScheme.primary
    val oColor = MaterialTheme.colorScheme.tertiary
    val background by animateColorAsState(
        if (isWinning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "cellBg",
    )
    val markLabel = when (player) {
        Player.X -> "X"; Player.O -> "O"; null -> stringResource(R.string.ttt_cell_empty)
    }
    val cellDesc = stringResource(R.string.ttt_cell_description, row + 1, col + 1, markLabel)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .bounceClick(enabled = enabled && player == null, onClick = onClick)
            .semantics { contentDescription = cellDesc },
        contentAlignment = Alignment.Center,
    ) {
        if (player != null) {
            Mark(player = player, color = if (player == Player.X) xColor else oColor)
        }
    }
}

/** Draws an X or O with a one-shot stroke-draw animation. */
@Composable
private fun Mark(player: Player, color: Color) {
    val progress = remember(player) { Animatable(0f) }
    LaunchedEffect(player) { progress.animateTo(1f, tween(260)) }
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize(0.62f)
            .graphicsLayer { },
    ) {
        val stroke = size.minDimension * 0.14f
        val p = progress.value
        if (player == Player.X) {
            val first = (p * 2f).coerceAtMost(1f)
            val second = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
            drawLine(
                color,
                Offset(0f, 0f),
                Offset(size.width * first, size.height * first),
                strokeWidth = stroke, cap = StrokeCap.Round,
            )
            if (second > 0f) {
                drawLine(
                    color,
                    Offset(size.width, 0f),
                    Offset(size.width - size.width * second, size.height * second),
                    strokeWidth = stroke, cap = StrokeCap.Round,
                )
            }
        } else {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun ResultOverlay(state: TicTacToeUiState, onPlayAgain: () -> Unit, onExit: () -> Unit) {
    val status = state.game.status
    val (emoji, title) = when (status) {
        is MatchStatus.Win -> when {
            state.mode == TicTacToeMode.VS_COMPUTER && status.player == Player.X -> "🏆" to stringResource(R.string.result_you_win)
            state.mode == TicTacToeMode.VS_COMPUTER -> "🤖" to stringResource(R.string.result_you_lose)
            status.player == Player.X -> "❌" to stringResource(R.string.ttt_wins_x)
            else -> "⭕" to stringResource(R.string.ttt_wins_o)
        }
        MatchStatus.Draw -> "🤝" to stringResource(R.string.result_draw)
        MatchStatus.InProgress -> "" to ""
    }
    GameResultOverlay(
        visible = state.showResult,
        emoji = emoji,
        title = title,
        primaryValue = "${state.xWins} · ${state.oWins}",
        primaryLabel = stringResource(R.string.ttt_wins_x) + " · " + stringResource(R.string.ttt_wins_o),
        onPlayAgain = onPlayAgain,
        onExit = onExit,
    )
}
