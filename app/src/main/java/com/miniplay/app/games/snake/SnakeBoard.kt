package com.miniplay.app.games.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpF
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
)

/**
 * The Snake board: a frame-interpolated, animated Canvas with a realistic snake
 * (per selected skin), typed food, obstacles, and an eat-particle system.
 * Swipe sensitivity is honoured by firing a turn as soon as the drag crosses the
 * configured threshold, so controls feel immediate.
 */
@Composable
fun SnakeBoard(
    state: SnakeUiState,
    eatEvents: SharedFlow<SnakeEatEvent>,
    onTurn: (SnakeDir) -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    val skin = state.skin
    val boardColor = MiniPlayTheme.colors.surfaceSunken
    val grassTint = Color(0xFF2E9E4F)
    val obstacleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val obstacleEdge = MaterialTheme.colorScheme.error

    val thresholdDp = state.settings.sensitivity.thresholdDp

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val sizePx = with(density) { maxWidth.toPx() }
        val cell = sizePx / game.cols

        var frameNanos by remember { mutableLongStateOf(0L) }
        val particles = remember { mutableStateListOf<Particle>() }
        val rng = remember { Random(System.nanoTime()) }

        // Single frame loop: advances particles and drives Canvas redraws.
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                frameNanos = now
                if (particles.isNotEmpty()) {
                    particles.forEach { p ->
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.vy += 220f * dt // gravity
                        p.life -= dt
                    }
                    particles.removeAll { it.life <= 0f }
                }
            }
        }

        // Spawn a burst when food is eaten.
        LaunchedEffect(Unit) {
            eatEvents.collect { ev ->
                val cx = (ev.cell.x + 0.5f) * cell
                val cy = (ev.cell.y + 0.5f) * cell
                val color = foodColor(ev.type)
                repeat(14) {
                    val ang = rng.nextFloat() * (2 * Math.PI).toFloat()
                    val speed = cell * (2.5f + rng.nextFloat() * 3.5f)
                    particles.add(
                        Particle(
                            x = cx, y = cy,
                            vx = cos(ang) * speed, vy = sin(ang) * speed,
                            life = 0.35f + rng.nextFloat() * 0.35f, maxLife = 0.7f,
                            color = color,
                        ),
                    )
                }
            }
        }

        // Tick snapshots for smooth sliding.
        val prevSnake = remember { mutableStateOf(game.snake) }
        val curSnake = remember { mutableStateOf(game.snake) }
        var tickStartNanos by remember { mutableLongStateOf(0L) }
        LaunchedEffect(state.tick) {
            prevSnake.value = curSnake.value
            curSnake.value = game.snake
            tickStartNanos = frameNanos
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(boardColor)
                .pointerInput(thresholdDp) {
                    val threshold = thresholdDp.dp.toPx()
                    var ax = 0f
                    var ay = 0f
                    detectDragGestures(
                        onDragStart = { ax = 0f; ay = 0f },
                        onDrag = { change, drag ->
                            change.consume()
                            ax += drag.x
                            ay += drag.y
                            if (abs(ax) >= threshold || abs(ay) >= threshold) {
                                if (abs(ax) > abs(ay)) {
                                    onTurn(if (ax > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                                } else {
                                    onTurn(if (ay > 0) SnakeDir.DOWN else SnakeDir.UP)
                                }
                                ax = 0f; ay = 0f
                            }
                        },
                    )
                },
        ) {
            val c = size.width / game.cols
            val timeSec = frameNanos / 1_000_000_000f
            val intervalNs = (state.tickIntervalMillis.coerceAtLeast(1)) * 1_000_000f
            val t = if (!state.running) 1f else ((frameNanos - tickStartNanos) / intervalNs).coerceIn(0f, 1f)

            // Grass checker.
            for (y in 0 until game.rows) for (x in 0 until game.cols) {
                if ((x + y) % 2 == 0) {
                    drawRect(grassTint.copy(alpha = 0.07f), Offset(x * c, y * c), Size(c, c))
                }
            }

            // Obstacles.
            for (o in game.config.obstacles) {
                val tl = Offset(o.x * c + c * 0.08f, o.y * c + c * 0.08f)
                val sz = Size(c * 0.84f, c * 0.84f)
                drawRoundRect(obstacleColor, tl, sz, CornerRadius(c * 0.2f))
                drawRoundRect(obstacleEdge.copy(alpha = 0.5f), tl, sz, CornerRadius(c * 0.2f), style = Stroke(width = c * 0.08f))
            }

            // Food.
            drawFood(
                center = Offset((game.food.cell.x + 0.5f) * c, (game.food.cell.y + 0.5f) * c),
                cell = c,
                food = game.food,
                currentTick = game.tick,
                timeSec = timeSec,
            )

            // Snake.
            val cur = curSnake.value
            val prev = prevSnake.value
            if (cur.isNotEmpty()) {
                val points = cur.indices.map { i ->
                    val to = Offset((cur[i].x + 0.5f) * c, (cur[i].y + 0.5f) * c)
                    val from = if (i < prev.size) Offset((prev[i].x + 0.5f) * c, (prev[i].y + 0.5f) * c) else to
                    // Don't interpolate across a wrap jump (big distance) — snap instead.
                    if (abs(from.x - to.x) > c * 2f || abs(from.y - to.y) > c * 2f) to
                    else Offset(lerpF(from.x, to.x, t), lerpF(from.y, to.y, t))
                }
                drawSnake(points, c, game.dir, state.gameOver, skin)
            }

            // Particles.
            for (p in particles) {
                val a = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(p.color.copy(alpha = a), radius = c * 0.14f * a + c * 0.04f, center = Offset(p.x, p.y))
            }
        }
    }
}

private fun foodColor(type: FoodType): Color = when (type) {
    FoodType.NORMAL -> Color(0xFFE5484D)
    FoodType.BONUS -> Color(0xFF3BA7FF)
    FoodType.GOLDEN -> Color(0xFFFFC53D)
}

private fun DrawScope.drawFood(center: Offset, cell: Float, food: Food, currentTick: Int, timeSec: Float) {
    when (food.type) {
        FoodType.NORMAL -> {
            val pulse = 1f + 0.08f * sin(timeSec * 4.0).toFloat()
            val r = cell * 0.34f * pulse
            drawCircle(Color(0xFFB4242A), r, center)
            drawCircle(Color(0xFFE5484D), r * 0.86f, center)
            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.22f, center + Offset(-r * 0.3f, -r * 0.32f))
            drawLine(Color(0xFF6D4C41), center + Offset(0f, -r * 0.8f), center + Offset(r * 0.14f, -r * 1.3f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
            drawCircle(Color(0xFF66BB6A), r * 0.22f, center + Offset(r * 0.36f, -r * 1.15f))
        }
        FoodType.BONUS -> {
            val pulse = 1f + 0.1f * sin(timeSec * 5.0).toFloat()
            val r = cell * 0.34f * pulse
            drawCircle(Color(0xFF3BA7FF).copy(alpha = 0.25f), r * 1.4f, center) // glow
            val gem = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            drawPath(gem, Color(0xFF1E63E9))
            val inner = Path().apply {
                moveTo(center.x, center.y - r * 0.6f)
                lineTo(center.x + r * 0.6f, center.y)
                lineTo(center.x, center.y + r * 0.6f)
                lineTo(center.x - r * 0.6f, center.y)
                close()
            }
            drawPath(inner, Color(0xFF59C2FF))
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.14f, center + Offset(-r * 0.2f, -r * 0.3f))
        }
        FoodType.GOLDEN -> {
            val pulse = 1f + 0.12f * sin(timeSec * 7.0).toFloat()
            val r = cell * 0.36f * pulse
            drawCircle(Color(0xFFFFC53D).copy(alpha = 0.3f), r * 1.6f, center) // glow
            drawCircle(Color(0xFFCC8E00), r, center)
            drawCircle(Color(0xFFFFD35B), r * 0.84f, center)
            drawCircle(Color.White.copy(alpha = 0.6f), r * 0.2f, center + Offset(-r * 0.28f, -r * 0.3f))
            // Countdown ring for the timed special.
            food.expiresAtTick?.let { expiry ->
                val remaining = (expiry - currentTick).toFloat()
                val frac = (remaining / SnakeEngine.GOLDEN_LIFETIME_TICKS).coerceIn(0f, 1f)
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * frac,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.35f, center.y - r * 1.35f),
                    size = Size(r * 2.7f, r * 2.7f),
                    style = Stroke(width = cell * 0.06f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun DrawScope.drawSnake(
    points: List<Offset>,
    cell: Float,
    dir: SnakeDir,
    gameOver: Boolean,
    skin: SnakeSkin,
) {
    val n = points.size
    if (n == 0) return
    val headR = cell * 0.46f
    val tailR = cell * 0.24f
    val headColor = if (gameOver) Color(0xFFD64545) else skin.bodyHead
    val tailColor = if (gameOver) Color(0xFF9E2B2B) else skin.bodyTail
    val outline = if (gameOver) Color(0xFF6B1B1B) else skin.outline

    fun frac(i: Int) = if (n <= 1) 0f else i.toFloat() / (n - 1)
    fun radiusAt(i: Int) = lerpF(headR, tailR, frac(i))
    fun colorAt(i: Int) = lerp(headColor, tailColor, frac(i))

    for (i in 1 until n) {
        drawLine(outline, points[i - 1], points[i], strokeWidth = radiusAt(i) * 2 + cell * 0.08f, cap = StrokeCap.Round)
    }
    for (i in 0 until n) drawCircle(outline, radiusAt(i) + cell * 0.04f, points[i])
    for (i in 1 until n) {
        drawLine(colorAt(i), points[i - 1], points[i], strokeWidth = radiusAt(i - 1) + radiusAt(i), cap = StrokeCap.Round)
    }
    for (i in 0 until n) drawCircle(colorAt(i), radiusAt(i), points[i])
    for (i in 0 until n) {
        drawCircle(Color.White.copy(alpha = 0.12f), radiusAt(i) * 0.5f, points[i] + Offset(0f, -radiusAt(i) * 0.35f))
    }

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
    if (!gameOver) {
        val flick = (sin((head.x + head.y).toDouble() * 0.05).toFloat() * 0.5f + 0.5f)
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
