package com.miniplay.app.games.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameIds
import kotlin.math.abs

@Composable
fun SnakeScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        SnakeViewModel(container.recordGameResult, container.soundManager, container.hapticManager, container.gameStatsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }
    val accent = GameAccent.forId(GameIds.SNAKE)

    GameScaffold(
        title = stringResource(R.string.snake_title),
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
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MiniPlayCard(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InlineStat(state.game.score.toString(), stringResource(R.string.game_score))
                        InlineStat(state.bestScore.toString(), stringResource(R.string.game_best))
                        InlineStat(state.game.length.toString(), stringResource(R.string.snake_length))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    SnakeBoard(
                        state = state.game,
                        accent = accent,
                        onTurn = viewModel::turn,
                        onTap = viewModel::start,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (!state.started && !state.gameOver) {
                        Text(
                            text = stringResource(R.string.snake_tap_to_start),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            GameResultOverlay(
                visible = state.gameOver,
                emoji = "🐍",
                title = stringResource(R.string.result_you_lose),
                primaryValue = state.game.score.toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                isNewBest = state.game.score.toLong() >= state.bestScore && state.bestScore > 0,
                onPlayAgain = viewModel::newGame,
                onExit = onExit,
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🐍",
            title = stringResource(R.string.snake_title),
            body = stringResource(R.string.snake_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun SnakeBoard(
    state: SnakeState,
    accent: GameAccent,
    onTurn: (SnakeDir) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardColor = MiniPlayTheme.colors.surfaceSunken
    val gridLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    val snakeBody = accent.start
    val snakeHead = accent.end
    val foodColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(boardColor)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, drag -> change.consume(); dx += drag.x; dy += drag.y },
                    onDragEnd = {
                        if (abs(dx) > abs(dy)) {
                            onTurn(if (dx > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                        } else if (dy != 0f) {
                            onTurn(if (dy > 0) SnakeDir.DOWN else SnakeDir.UP)
                        }
                    },
                )
            },
    ) {
        val cell = size.minDimension / state.cols
        val inset = cell * 0.08f
        val radius = CornerRadius(cell * 0.25f)

        // Subtle grid.
        for (i in 1 until state.cols) {
            drawLine(gridLine, Offset(i * cell, 0f), Offset(i * cell, size.height), strokeWidth = 1f)
            drawLine(gridLine, Offset(0f, i * cell), Offset(size.width, i * cell), strokeWidth = 1f)
        }

        // Food.
        drawCircle(
            color = foodColor,
            radius = cell * 0.32f,
            center = Offset(state.food.x * cell + cell / 2, state.food.y * cell + cell / 2),
        )

        // Snake (head brighter).
        state.snake.forEachIndexed { i, c ->
            drawRoundRect(
                color = if (i == 0) snakeHead else snakeBody,
                topLeft = Offset(c.x * cell + inset, c.y * cell + inset),
                size = Size(cell - inset * 2, cell - inset * 2),
                cornerRadius = radius,
            )
        }
    }
}
