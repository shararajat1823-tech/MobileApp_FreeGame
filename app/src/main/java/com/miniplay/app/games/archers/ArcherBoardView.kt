package com.miniplay.app.games.archers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val SKY_TOP = Color(0xFF87CEEB)
private val SKY_BOTTOM = Color(0xFFCDEFFF)
private val HILL = Color(0xFF8FD694)
private val GRASS = Color(0xFF66BB6A)
private val DIRT = Color(0xFF7A5230)
private val PLAYER = Color(0xFF3B82F6)
private val PLAYER_DARK = Color(0xFF1C4FA8)
private val OPPONENT = Color(0xFFE5484D)
private val OPPONENT_DARK = Color(0xFF9B1C2B)
private val ARROW_COLOR = Color(0xFF3A2A18)

/**
 * Renders the battlefield and runs the game's frame clock: each frame it advances
 * the authoritative [match] via [onFrame] and redraws from the live simulation.
 * World units (16×9) are letterboxed into the box, so geometry never distorts
 * across screen sizes. Drag anywhere to aim (slingshot pull-back) and release to
 * shoot; a trajectory preview shows while aiming.
 */
@Composable
fun ArcherBoardView(
    state: ArcherUiState,
    match: ArcherMatch,
    onFrame: (Long) -> Unit,
    onAim: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val scale = min(w / 16f, h / 9f)
        val ox = (w - 16f * scale) / 2f
        val oy = (h - 9f * scale) / 2f

        var frame by remember { mutableLongStateOf(0L) }
        LaunchedEffect(Unit) {
            while (true) {
                val now = withFrameNanos { it }
                onFrame(now)
                frame = now
            }
        }

        val shakeOn = state.settings.screenShake
        val canAim = state.canAim

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(canAim) {
                    if (!canAim) return@pointerInput
                    var startX = 0f
                    var startY = 0f
                    val maxDrag = size.width * 0.38f
                    detectDragGestures(
                        onDragStart = { o -> startX = o.x; startY = o.y },
                        onDragEnd = { onRelease() },
                        onDragCancel = { onRelease() },
                        onDrag = { change, _ ->
                            change.consume()
                            val cur = change.position
                            // Slingshot: launch opposite to the pull.
                            val lx = startX - cur.x
                            val ly = startY - cur.y
                            val dist = hypot(lx, ly)
                            if (dist < 1f) return@detectDragGestures
                            val angle = atan2(ly.toDouble(), lx.toDouble()).toFloat() // screen-y down -> up is +ly
                            val clamped = angle.coerceIn(rad(-8f), rad(110f))
                            val power = (dist / maxDrag).coerceIn(0.05f, 1f)
                            onAim(clamped, power)
                        },
                    )
                },
        ) {
            // read frame so Compose redraws each tick
            frame
            val shake = if (shakeOn) match.shake * scale * 0.5f else 0f
            val sxShake = if (shake > 0.01f) ((frame % 7) - 3) / 3f * shake else 0f
            val syShake = if (shake > 0.01f) ((frame % 5) - 2) / 2f * shake else 0f

            translate(ox + sxShake, oy + syShake) {
                drawWorld(match, scale, state)
            }
        }
    }
}

private fun rad(deg: Float) = (deg * Math.PI / 180.0).toFloat()

private fun DrawScope.drawWorld(match: ArcherMatch, scale: Float, state: ArcherUiState) {
    val cfg = match.config.world
    val groundPx = cfg.groundY * scale
    val worldW = 16f * scale
    val worldH = 9f * scale

    // Sky.
    drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_BOTTOM), startY = 0f, endY = groundPx), size = Size(worldW, groundPx))
    // Rolling hills.
    drawCircle(HILL, radius = 3.2f * scale, center = Offset(4.5f * scale, groundPx), alpha = 0.7f)
    drawCircle(HILL, radius = 4.0f * scale, center = Offset(11.5f * scale, groundPx), alpha = 0.6f)
    // Ground.
    drawRect(GRASS, topLeft = Offset(0f, groundPx), size = Size(worldW, (worldH - groundPx).coerceAtLeast(0f)))
    drawRect(DIRT, topLeft = Offset(0f, groundPx + 0.3f * scale), size = Size(worldW, (worldH - groundPx - 0.3f * scale).coerceAtLeast(0f)))

    fun px(wx: Float) = wx * scale
    fun py(wy: Float) = wy * scale

    // Embedded (stuck) arrows behind the bodies.
    for (e in match.embedded) {
        drawArrowShape(Offset(px(e.x), py(e.y)), e.angle, scale, dimmed = true)
    }

    // Archers.
    match.archers.forEachIndexed { i, archer ->
        val main = if (i == 0) PLAYER else OPPONENT
        val dark = if (i == 0) PLAYER_DARK else OPPONENT_DARK
        drawRagdoll(archer, main, dark, scale, defeated = !archer.ragdoll.balanced)
    }

    // Aiming preview (player only).
    if (state.aiming && state.preview.isNotEmpty()) {
        for ((idx, p) in state.preview.withIndex()) {
            val a = 1f - idx.toFloat() / state.preview.size
            drawCircle(Color.White.copy(alpha = 0.25f + a * 0.5f), radius = scale * 0.07f, center = Offset(px(p.first), py(p.second)))
        }
    }

    // Flying arrow + trail.
    match.arrow?.let { a ->
        val tip = Offset(px(a.x), py(a.y))
        val tail = Offset(px(a.x - a.vx * 3f), py(a.y - a.vy * 3f))
        drawLine(Color.White.copy(alpha = 0.35f), tail, tip, strokeWidth = scale * 0.06f, cap = StrokeCap.Round)
        drawArrowShape(tip, atan2(a.vy, a.vx), scale, dimmed = false)
    }
}

private fun DrawScope.drawRagdoll(archer: Archer, main: Color, dark: Color, scale: Float, defeated: Boolean) {
    val r = archer.ragdoll
    fun o(p: PhysicsPoint) = Offset(p.x * scale, p.y * scale)
    val limbW = scale * 0.16f
    val torsoW = scale * 0.22f

    val limbColor = if (defeated) dark.copy(alpha = 0.85f) else dark
    val bodyColor = if (defeated) main.copy(alpha = 0.85f) else main

    // Legs.
    drawLine(limbColor, o(r.pelvis), o(r.footFront), strokeWidth = limbW, cap = StrokeCap.Round)
    drawLine(limbColor, o(r.pelvis), o(r.footBack), strokeWidth = limbW, cap = StrokeCap.Round)
    // Torso.
    drawLine(bodyColor, o(r.pelvis), o(r.chest), strokeWidth = torsoW, cap = StrokeCap.Round)
    drawLine(bodyColor, o(r.chest), o(r.head), strokeWidth = torsoW, cap = StrokeCap.Round)
    // Bow arm.
    drawLine(limbColor, o(r.chest), o(r.hand), strokeWidth = limbW, cap = StrokeCap.Round)
    // Bow (a simple arc/crescent at the hand).
    val hand = o(r.hand)
    drawArc(
        color = dark,
        startAngle = if (r.facingRight) -70f else 110f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(hand.x - scale * 0.5f, hand.y - scale * 0.5f),
        size = Size(scale, scale),
        style = Stroke(width = scale * 0.07f),
    )
    // Head.
    val head = o(r.head)
    drawCircle(dark, r.head.radius * scale + scale * 0.03f, head)
    drawCircle(main, r.head.radius * scale, head)
    // Eyes (X when defeated).
    val eyeDir = if (r.facingRight) 1f else -1f
    val eye = Offset(head.x + eyeDir * r.head.radius * scale * 0.4f, head.y - r.head.radius * scale * 0.15f)
    if (defeated) {
        val s = scale * 0.08f
        drawLine(dark, Offset(eye.x - s, eye.y - s), Offset(eye.x + s, eye.y + s), strokeWidth = scale * 0.04f)
        drawLine(dark, Offset(eye.x - s, eye.y + s), Offset(eye.x + s, eye.y - s), strokeWidth = scale * 0.04f)
    } else {
        drawCircle(Color.White, scale * 0.07f, eye)
        drawCircle(dark, scale * 0.035f, Offset(eye.x + eyeDir * scale * 0.02f, eye.y))
    }
}

private fun DrawScope.drawArrowShape(tip: Offset, angle: Float, scale: Float, dimmed: Boolean) {
    val len = scale * 0.7f
    val back = Offset(tip.x - cos(angle) * len, tip.y - sin(angle) * len)
    val color = if (dimmed) ARROW_COLOR.copy(alpha = 0.7f) else ARROW_COLOR
    drawLine(color, back, tip, strokeWidth = scale * 0.06f, cap = StrokeCap.Round)
    // Fletching.
    val perp = Offset(-sin(angle), cos(angle))
    drawLine(Color(0xFFE5484D), back, Offset(back.x + perp.x * scale * 0.12f + cos(angle) * scale * 0.12f, back.y + perp.y * scale * 0.12f + sin(angle) * scale * 0.12f), strokeWidth = scale * 0.04f)
    drawLine(Color(0xFFE5484D), back, Offset(back.x - perp.x * scale * 0.12f + cos(angle) * scale * 0.12f, back.y - perp.y * scale * 0.12f + sin(angle) * scale * 0.12f), strokeWidth = scale * 0.04f)
    // Head.
    drawCircle(Color(0xFF444444), scale * 0.05f, tip)
}
