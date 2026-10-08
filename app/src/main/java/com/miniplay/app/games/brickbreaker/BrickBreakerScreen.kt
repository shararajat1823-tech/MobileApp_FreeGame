package com.miniplay.app.games.brickbreaker

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameIds
import kotlinx.coroutines.delay

@Composable
fun BrickBreakerScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        BrickBreakerViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.gameStatsRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    val world = state.world
    val status = world.status
    val exit = {
        viewModel.recordIfNeeded()
        onExit()
    }

    // Frame-clock game loop: tick only while the ball is in play. Keyed on status
    // so it (re)starts when play begins and stops the instant play ends.
    LaunchedEffect(status) {
        if (status == BrickStatus.RUNNING) {
            var last = withFrameNanos { it }
            while (viewModel.uiState.value.world.status == BrickStatus.RUNNING) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.033f)
                last = now
                viewModel.tick(dt)
            }
        }
    }

    // After a life is lost, re-arm on the paddle so the player can relaunch.
    LaunchedEffect(status) {
        if (status == BrickStatus.LIFE_LOST) {
            delay(600)
            viewModel.rearmAfterLifeLost()
        }
    }

    GameScaffold(
        title = stringResource(R.string.brick_title),
        onExit = exit,
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        InlineStat(world.score.toString(), stringResource(R.string.game_score))
                        InlineStat(world.lives.toString(), stringResource(R.string.game_lives))
                        InlineStat(world.level.toString(), stringResource(R.string.game_level))
                    }
                }

                PlayField(
                    world = world,
                    onPaddleMove = viewModel::onPaddleMove,
                    onLaunch = viewModel::launch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                )
            }

            when (status) {
                BrickStatus.LEVEL_CLEARED -> GameResultOverlay(
                    visible = true,
                    emoji = "🎉",
                    title = stringResource(R.string.brick_level_complete, world.level),
                    primaryValue = world.score.toString(),
                    primaryLabel = stringResource(R.string.result_score_label),
                    onPlayAgain = viewModel::newGame,
                    onExit = exit,
                    secondary = {
                        PrimaryButton(
                            text = stringResource(R.string.brick_next_level),
                            onClick = viewModel::nextLevel,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
                BrickStatus.GAME_OVER -> GameResultOverlay(
                    visible = true,
                    emoji = "💥",
                    title = stringResource(R.string.result_you_lose),
                    primaryValue = world.score.toString(),
                    primaryLabel = stringResource(R.string.result_score_label),
                    isNewBest = world.score.toLong() >= state.bestScore && state.bestScore > 0,
                    onPlayAgain = viewModel::newGame,
                    onExit = exit,
                )
                else -> Unit
            }
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🧱",
            title = stringResource(R.string.brick_title),
            body = stringResource(R.string.brick_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun PlayField(
    world: BrickWorld,
    onPaddleMove: (Float) -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(GameIds.BRICK_BREAKER)
    // A small set of accent tones so higher brick rows read distinctly.
    val brickTones = listOf(
        accent.start,
        accent.end,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    val paddleColor = MaterialTheme.colorScheme.primary
    val ballColor = MaterialTheme.colorScheme.onSurface
    val fieldColor = MiniPlayTheme.colors.surfaceSunken
    val status = world.status

    Box(
        modifier = modifier
            .aspectRatio(BrickBreaker.WORLD_WIDTH / BrickBreaker.WORLD_HEIGHT)
            .clip(RoundedCornerShape(24.dp))
            .background(fieldColor)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val worldX = change.position.x / size.width * BrickBreaker.WORLD_WIDTH
                    onPaddleMove(worldX)
                }
            }
            .pointerInput(status) {
                detectTapGestures {
                    if (status == BrickStatus.READY) onLaunch()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val scaleX = size.width / BrickBreaker.WORLD_WIDTH
            val scaleY = size.height / BrickBreaker.WORLD_HEIGHT

            world.bricks.forEach { brick ->
                if (!brick.alive) return@forEach
                val tone = brickTones[(brick.points / 10) % brickTones.size]
                drawRoundRect(
                    color = tone,
                    topLeft = Offset(brick.x * scaleX, brick.y * scaleY),
                    size = Size(brick.width * scaleX, brick.height * scaleY),
                    cornerRadius = CornerRadius(2.5f),
                )
            }

            val paddle = world.paddle
            drawRoundRect(
                color = paddleColor,
                topLeft = Offset(paddle.left * scaleX, paddle.y * scaleY),
                size = Size(paddle.width * scaleX, paddle.height * scaleY),
                cornerRadius = CornerRadius(paddle.height * scaleY / 2f),
            )

            val ball = world.ball
            drawCircle(
                color = ballColor,
                radius = ball.radius * scaleX,
                center = Offset(ball.x * scaleX, ball.y * scaleY),
            )
        }

        if (status == BrickStatus.READY || status == BrickStatus.LIFE_LOST) {
            Text(
                text = stringResource(R.string.brick_tap_to_launch),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
