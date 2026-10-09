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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpF
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SnakeScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        SnakeViewModel(container.recordGameResult, container.soundManager, container.hapticManager, container.gameStatsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

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
                        uiState = state,
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
    uiState: SnakeUiState,
    onTurn: (SnakeDir) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = uiState.game
    val boardColor = MiniPlayTheme.colors.surfaceSunken
    val grassTint = Color(0xFF2E9E4F)

    // Per-frame clock that drives smooth interpolation between engine ticks.
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos = it }
    }

    // Snapshot the snake at each tick so we can glide from the previous layout
    // to the current one instead of snapping cell-to-cell.
    val prevSnake = remember { mutableStateOf(game.snake) }
    val curSnake = remember { mutableStateOf(game.snake) }
    var tickStartNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(uiState.tick) {
        prevSnake.value = curSnake.value
        curSnake.value = game.snake
        tickStartNanos = frameNanos
    }

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
        val cols = game.cols
        val rows = game.rows
        val cell = size.width / cols
        val timeSec = frameNanos / 1_000_000_000f

        val intervalNs = (uiState.tickIntervalMillis.coerceAtLeast(1)) * 1_000_000f
        val t = if (!uiState.started || game.gameOver) {
            1f
        } else {
            ((frameNanos - tickStartNanos) / intervalNs).coerceIn(0f, 1f)
        }

        // Grass field: subtle green checker over the themed board.
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if ((x + y) % 2 == 0) {
                    drawRect(
                        color = grassTint.copy(alpha = 0.07f),
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }

        // Food apple with a gentle pulse.
        drawApple(
            center = Offset((game.food.x + 0.5f) * cell, (game.food.y + 0.5f) * cell),
            baseRadius = cell * 0.34f,
            timeSec = timeSec,
        )

        // Interpolated snake body.
        val cur = curSnake.value
        val prev = prevSnake.value
        if (cur.isNotEmpty()) {
            val points = cur.indices.map { i ->
                val to = Offset((cur[i].x + 0.5f) * cell, (cur[i].y + 0.5f) * cell)
                val from = if (i < prev.size) {
                    Offset((prev[i].x + 0.5f) * cell, (prev[i].y + 0.5f) * cell)
                } else {
                    to
                }
                Offset(lerpF(from.x, to.x, t), lerpF(from.y, to.y, t))
            }
            drawSnake(points = points, cell = cell, dir = game.dir, timeSec = timeSec, gameOver = game.gameOver)
        }
    }
}

/** Draws a smooth, tapering snake with a detailed head, eyes and a flicking tongue. */
private fun DrawScope.drawSnake(
    points: List<Offset>,
    cell: Float,
    dir: SnakeDir,
    timeSec: Float,
    gameOver: Boolean,
) {
    val n = points.size
    if (n == 0) return

    val headR = cell * 0.46f
    val tailR = cell * 0.24f
    val headColor = if (gameOver) Color(0xFFD64545) else Color(0xFF72D873)
    val tailColor = if (gameOver) Color(0xFF9E2B2B) else Color(0xFF2E9E4F)
    val outline = if (gameOver) Color(0xFF6B1B1B) else Color(0xFF1B6B36)

    fun frac(i: Int) = if (n <= 1) 0f else i.toFloat() / (n - 1)
    fun radiusAt(i: Int) = lerpF(headR, tailR, frac(i))
    fun colorAt(i: Int) = lerp(headColor, tailColor, frac(i))

    // Dark outline pass for depth.
    for (i in 1 until n) {
        drawLine(outline, points[i - 1], points[i], strokeWidth = radiusAt(i) * 2 + cell * 0.08f, cap = StrokeCap.Round)
    }
    for (i in 0 until n) drawCircle(outline, radiusAt(i) + cell * 0.04f, points[i])

    // Body pass.
    for (i in 1 until n) {
        drawLine(colorAt(i), points[i - 1], points[i], strokeWidth = radiusAt(i - 1) + radiusAt(i), cap = StrokeCap.Round)
    }
    for (i in 0 until n) drawCircle(colorAt(i), radiusAt(i), points[i])

    // Glossy highlight running along the back.
    for (i in 0 until n) {
        drawCircle(Color.White.copy(alpha = 0.12f), radiusAt(i) * 0.5f, points[i] + Offset(0f, -radiusAt(i) * 0.35f))
    }

    // --- Head details ---
    val head = points[0]
    val d = Offset(dir.dx.toFloat(), dir.dy.toFloat())
    val perp = Offset(-d.y, d.x)

    val eyeBase = head + d * (headR * 0.2f)
    val eyeR = headR * 0.27f
    val pupilR = headR * 0.14f
    for (s in listOf(-1f, 1f)) {
        val ec = eyeBase + perp * (headR * 0.5f * s)
        drawCircle(Color.White, eyeR, ec)
        drawCircle(Color(0xFF17241B), pupilR, ec + d * (eyeR * 0.35f))
    }

    // Forked tongue, flicking in and out.
    if (!gameOver) {
        val flick = (sin(timeSec * 9.0).toFloat() * 0.5f + 0.5f)
        if (flick > 0.55f) {
            val tongue = Color(0xFFE5484D)
            val base = head + d * (headR * 0.95f)
            val tip = base + d * (cell * 0.45f * flick)
            val fork = perp * (cell * 0.12f)
            drawLine(tongue, base, tip, strokeWidth = cell * 0.06f, cap = StrokeCap.Round)
            drawLine(tongue, tip, tip + d * (cell * 0.12f) + fork, strokeWidth = cell * 0.05f, cap = StrokeCap.Round)
            drawLine(tongue, tip, tip + d * (cell * 0.12f) - fork, strokeWidth = cell * 0.05f, cap = StrokeCap.Round)
        }
    }
}

/** A glossy apple that gently pulses. */
private fun DrawScope.drawApple(center: Offset, baseRadius: Float, timeSec: Float) {
    val pulse = 1f + 0.08f * sin(timeSec * 4.0).toFloat()
    val r = baseRadius * pulse
    drawCircle(Color(0xFFB4242A), r, center)
    drawCircle(Color(0xFFE5484D), r * 0.86f, center)
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.22f, center + Offset(-r * 0.3f, -r * 0.32f))
    drawLine(
        Color(0xFF6D4C41),
        center + Offset(0f, -r * 0.8f),
        center + Offset(r * 0.14f, -r * 1.3f),
        strokeWidth = r * 0.14f,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFF66BB6A), r * 0.22f, center + Offset(r * 0.36f, -r * 1.15f))
}
