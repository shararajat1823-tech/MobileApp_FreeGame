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
import kotlin.math.sin

private val SKY_TOP = Color(0xFF5EC2F0)
private val SKY_BOTTOM = Color(0xFFBDEBFF)
private val HILL_BACK = Color(0xFF8ED98F)
private val HILL_FRONT = Color(0xFF72C97A)
private val GRASS = Color(0xFF5BB85F)
private val GRASS_TOP = Color(0xFF7BD17F)
private val DIRT = Color(0xFF7A5230)
private val PLAYER = Color(0xFF3B82F6)
private val PLAYER_DARK = Color(0xFF1C4FA8)
private val OPPONENT = Color(0xFFE5484D)
private val OPPONENT_DARK = Color(0xFF9B1C2B)
private val ARROW_COLOR = Color(0xFF4A3420)
private val AIM_COLOR = Color(0xFFFFD54F)

// How much of the 16-unit-wide world is shown (zoomed in on the archers).
private const val VIEW_LEFT = 1.0f
private const val VIEW_WIDTH = 14f

/**
 * Renders the battlefield and runs the game's frame clock: each frame it advances
 * the authoritative [match] via [onFrame] and redraws from the live simulation.
 * The world is scaled to fill the box width (zoomed onto the archers) and the sky
 * extends to fill all vertical space, so there are no empty bars. Drag to aim
 * (slingshot pull-back); a gold aim line and a dotted arc preview the shot.
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
        val s = w / VIEW_WIDTH
        val ox = -VIEW_LEFT * s
        val groundScreenY = h * 0.80f
        val oy = groundScreenY - match.config.world.groundY * s

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
                    val maxDrag = size.width * 0.30f
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
                            val angle = atan2(ly.toDouble(), lx.toDouble()).toFloat()
                            val clamped = angle.coerceIn(rad(-10f), rad(100f))
                            val power = (dist / maxDrag).coerceIn(0.08f, 1f)
                            onAim(clamped, power)
                        },
                    )
                },
        ) {
            frame // read so Compose redraws each tick
            val shake = if (shakeOn) match.shake * s * 0.5f else 0f
            val sxShake = if (shake > 0.01f) ((frame % 7) - 3) / 3f * shake else 0f
            val syShake = if (shake > 0.01f) ((frame % 5) - 2) / 2f * shake else 0f

            translate(sxShake, syShake) {
                drawWorld(match, s, ox, oy, size.width, size.height, state)
            }
        }
    }
}

private fun rad(deg: Float) = (deg * Math.PI / 180.0).toFloat()

private fun DrawScope.drawWorld(match: ArcherMatch, s: Float, ox: Float, oy: Float, w: Float, h: Float, state: ArcherUiState) {
    val cfg = match.config.world
    val groundY = cfg.groundY * s + oy
    fun px(v: Float) = v * s + ox
    fun py(v: Float) = v * s + oy

    // Sky filling all the space above the ground.
    drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_BOTTOM), startY = 0f, endY = groundY), size = Size(w, groundY.coerceAtLeast(0f)))
    // Soft rolling hills behind the ground line.
    drawCircle(HILL_BACK, radius = 3.4f * s, center = Offset(px(4.5f), groundY + 0.9f * s), alpha = 0.8f)
    drawCircle(HILL_BACK, radius = 4.2f * s, center = Offset(px(11f), groundY + 1.1f * s), alpha = 0.75f)
    drawCircle(HILL_FRONT, radius = 2.6f * s, center = Offset(px(8f), groundY + 1.2f * s), alpha = 0.9f)
    // Ground.
    drawRect(GRASS_TOP, topLeft = Offset(0f, groundY), size = Size(w, (h - groundY).coerceAtLeast(0f)))
    drawRect(GRASS, topLeft = Offset(0f, groundY + 0.12f * s), size = Size(w, (h - groundY - 0.12f * s).coerceAtLeast(0f)))
    drawRect(DIRT, topLeft = Offset(0f, groundY + 0.55f * s), size = Size(w, (h - groundY - 0.55f * s).coerceAtLeast(0f)))

    // Embedded (stuck) arrows behind the bodies.
    for (e in match.embedded) {
        drawArrowShape(Offset(px(e.x), py(e.y)), e.angle, s, dimmed = true)
    }

    // Archers.
    match.archers.forEachIndexed { i, archer ->
        val main = if (i == 0) PLAYER else OPPONENT
        val dark = if (i == 0) PLAYER_DARK else OPPONENT_DARK
        drawRagdoll(archer, main, dark, s, ox, oy, defeated = !archer.ragdoll.balanced)
    }

    // Aim guide (player only): a bold line from the bow + dotted trajectory.
    if (state.aiming) {
        val hand = match.archers[0].ragdoll.hand
        val hx = px(hand.x)
        val hy = py(hand.y)
        val dx = cos(state.aimAngle.toDouble()).toFloat()
        val dy = -sin(state.aimAngle.toDouble()).toFloat()
        val len = (1.1f + state.aimPower * 2.4f) * s
        val tip = Offset(hx + dx * len, hy + dy * len)
        drawLine(AIM_COLOR.copy(alpha = 0.95f), Offset(hx, hy), tip, strokeWidth = s * 0.12f, cap = StrokeCap.Round)
        // Arrowhead on the aim line.
        val perp = Offset(-dy, dx)
        drawLine(AIM_COLOR, tip, Offset(tip.x - dx * s * 0.3f + perp.x * s * 0.2f, tip.y - dy * s * 0.3f + perp.y * s * 0.2f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
        drawLine(AIM_COLOR, tip, Offset(tip.x - dx * s * 0.3f - perp.x * s * 0.2f, tip.y - dy * s * 0.3f - perp.y * s * 0.2f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)

        for ((idx, p) in state.preview.withIndex()) {
            val a = 1f - idx.toFloat() / state.preview.size.coerceAtLeast(1)
            drawCircle(Color.White.copy(alpha = 0.25f + a * 0.55f), radius = s * 0.08f, center = Offset(px(p.first), py(p.second)))
        }
    }

    // Flying arrow + trail.
    match.arrow?.let { a ->
        val tip = Offset(px(a.x), py(a.y))
        val tail = Offset(px(a.x - a.vx * 4f), py(a.y - a.vy * 4f))
        drawLine(Color.White.copy(alpha = 0.45f), tail, tip, strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        drawArrowShape(tip, atan2(a.vy, a.vx), s, dimmed = false)
    }
}

private fun DrawScope.drawRagdoll(archer: Archer, main: Color, dark: Color, s: Float, ox: Float, oy: Float, defeated: Boolean) {
    val r = archer.ragdoll
    fun o(p: PhysicsPoint) = Offset(p.x * s + ox, p.y * s + oy)
    val limbW = s * 0.18f
    val torsoW = s * 0.26f

    val limbColor = if (defeated) dark.copy(alpha = 0.85f) else dark
    val bodyColor = if (defeated) main.copy(alpha = 0.85f) else main

    // Ground shadow.
    val feet = o(r.footFront)
    drawOval(Color.Black.copy(alpha = 0.16f), Offset(o(r.pelvis).x - s * 0.5f, feet.y - s * 0.08f), Size(s, s * 0.22f))

    // Legs, torso, bow arm.
    drawLine(limbColor, o(r.pelvis), o(r.footFront), strokeWidth = limbW, cap = StrokeCap.Round)
    drawLine(limbColor, o(r.pelvis), o(r.footBack), strokeWidth = limbW, cap = StrokeCap.Round)
    drawLine(bodyColor, o(r.pelvis), o(r.chest), strokeWidth = torsoW, cap = StrokeCap.Round)
    drawLine(bodyColor, o(r.chest), o(r.head), strokeWidth = torsoW, cap = StrokeCap.Round)
    drawLine(limbColor, o(r.chest), o(r.hand), strokeWidth = limbW, cap = StrokeCap.Round)

    // Bow: a semicircle facing the opponent, plus a string.
    val hand = o(r.hand)
    val br = s * 0.55f
    drawArc(
        color = ARROW_COLOR,
        startAngle = if (r.facingRight) -90f else 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(hand.x - br, hand.y - br),
        size = Size(br * 2, br * 2),
        style = Stroke(width = s * 0.09f),
    )
    drawLine(Color(0xFFDDDDDD), Offset(hand.x, hand.y - br), Offset(hand.x, hand.y + br), strokeWidth = s * 0.03f)

    // Head.
    val head = o(r.head)
    drawCircle(dark, r.head.radius * s + s * 0.03f, head)
    drawCircle(main, r.head.radius * s, head)
    val eyeDir = if (r.facingRight) 1f else -1f
    val eye = Offset(head.x + eyeDir * r.head.radius * s * 0.4f, head.y - r.head.radius * s * 0.12f)
    if (defeated) {
        val d = s * 0.09f
        drawLine(Color.White, Offset(eye.x - d, eye.y - d), Offset(eye.x + d, eye.y + d), strokeWidth = s * 0.045f)
        drawLine(Color.White, Offset(eye.x - d, eye.y + d), Offset(eye.x + d, eye.y - d), strokeWidth = s * 0.045f)
    } else {
        drawCircle(Color.White, s * 0.08f, eye)
        drawCircle(Color(0xFF17241B), s * 0.04f, Offset(eye.x + eyeDir * s * 0.02f, eye.y))
    }
}

private fun DrawScope.drawArrowShape(tip: Offset, angle: Float, s: Float, dimmed: Boolean) {
    val len = s * 0.95f
    val back = Offset(tip.x - cos(angle) * len, tip.y - sin(angle) * len)
    val shaft = if (dimmed) ARROW_COLOR.copy(alpha = 0.7f) else ARROW_COLOR
    drawLine(shaft, back, tip, strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    // Fletching.
    val perp = Offset(-sin(angle), cos(angle))
    val fx = cos(angle) * s * 0.16f
    val fy = sin(angle) * s * 0.16f
    drawLine(Color(0xFFE5484D), back, Offset(back.x + perp.x * s * 0.14f + fx, back.y + perp.y * s * 0.14f + fy), strokeWidth = s * 0.05f)
    drawLine(Color(0xFFE5484D), back, Offset(back.x - perp.x * s * 0.14f + fx, back.y - perp.y * s * 0.14f + fy), strokeWidth = s * 0.05f)
    // Metal head.
    drawCircle(Color(0xFF9E9E9E), s * 0.07f, tip)
}
