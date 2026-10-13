package com.miniplay.app.games.ludo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpF
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private class LudoParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val color: Color)

private data class TokenAnim(
    val color: LudoColor,
    val tokenIndex: Int,
    val path: List<Offset>, // cell-unit centres (x+0.5, y+0.5)
    val startNanos: Long,
    val durationNanos: Long,
)

/**
 * The Ludo board: a frame-interpolated Canvas drawing the four yards, the shared
 * ring, coloured home columns, safe stars, the centre home and every token from
 * authoritative [LudoState]. Legal tokens pulse; the just-moved token glides
 * along its real path; captures spawn a particle flash. Taps hit-test against the
 * current player's legal tokens.
 */
@Composable
fun LudoBoardView(
    state: LudoUiState,
    moveAnims: SharedFlow<LudoMoveAnim>,
    onTokenTap: (LudoColor, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = state.game ?: return
    val boardColor = LudoColors.boardBackdrop
    val reduced = state.settings.reducedEffects

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val sizePx = with(density) { maxWidth.toPx() }
        val cell = sizePx / LudoBoard.GRID

        var frameNanos by remember { mutableLongStateOf(0L) }
        val particles = remember { mutableStateListOf<LudoParticle>() }
        var anim by remember { mutableStateOf<TokenAnim?>(null) }

        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                val now = withFrameNanos { it }
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                frameNanos = now
                if (particles.isNotEmpty()) {
                    particles.forEach { p ->
                        p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 220f * dt; p.life -= dt
                    }
                    particles.removeAll { it.life <= 0f }
                }
                anim?.let { if (now - it.startNanos >= it.durationNanos) anim = null }
            }
        }

        LaunchedEffect(Unit) {
            moveAnims.collect { ev ->
                val path = buildPath(ev)
                val steps = (path.size - 1).coerceAtLeast(1)
                val perStep = if (reduced) 45L else 85L
                anim = TokenAnim(
                    color = ev.color,
                    tokenIndex = ev.tokenIndex,
                    path = path,
                    startNanos = frameNanos,
                    durationNanos = (120L + steps * perStep) * 1_000_000L,
                )
                if (!reduced && ev.captured.isNotEmpty()) {
                    ev.captured.forEach { c ->
                        val cx = (c.x + 0.5f) * cell
                        val cy = (c.y + 0.5f) * cell
                        repeat(16) {
                            val a = Math.random().toFloat() * (2 * Math.PI).toFloat()
                            val sp = cell * (2.5f + Math.random().toFloat() * 3.5f)
                            particles.add(
                                LudoParticle(cx, cy, cos(a) * sp, sin(a) * sp, 0.4f + Math.random().toFloat() * 0.3f, 0.7f, Color.White),
                            )
                        }
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(boardColor)
                .pointerInput(game.turnId, state.inputLocked) {
                    // Tap a legal token of the current player.
                    detectTapToken(
                        cell = cell,
                        legalCenters = {
                            if (state.inputLocked || game.phase != LudoPhase.AWAIT_MOVE) emptyList()
                            else game.legalMoves.map { idx ->
                                idx to tokenCenterPx(game.currentColor, idx, game.tokens.getValue(game.currentColor)[idx], cell)
                            }
                        },
                        onHit = { idx -> onTokenTap(game.currentColor, idx) },
                    )
                },
        ) {
            val c = size.width / LudoBoard.GRID

            drawYards(c)
            drawTrack(c)
            drawHomeColumns(c)
            drawArrows(c)
            drawCenter(c)

            // Tokens — group by resting cell for stack offsets; skip the animating one.
            val animating = anim
            val pulse = (sin(frameNanos / 1_000_000_000.0 * 4.0).toFloat() * 0.5f + 0.5f)
            val legalSet = if (game.phase == LudoPhase.AWAIT_MOVE) game.legalMoves.toSet() else emptySet()

            val grouped = HashMap<Long, MutableList<Pair<LudoColor, Int>>>()
            for (color in game.players) {
                val progs = game.tokens.getValue(color)
                for (i in progs.indices) {
                    if (animating != null && animating.color == color && animating.tokenIndex == i) continue
                    val cellKey = cellKeyOf(color, i, progs[i])
                    grouped.getOrPut(cellKey) { mutableListOf() }.add(color to i)
                }
            }
            for ((_, members) in grouped) {
                members.forEachIndexed { stackIndex, (color, i) ->
                    val base = tokenCenterPx(color, i, game.tokens.getValue(color)[i], c)
                    val off = stackOffset(stackIndex, members.size, c)
                    val legal = color == game.currentColor && i in legalSet
                    drawToken(base + off, LudoColors.of(color), LudoColors.dark(color), c * 0.34f, legal, pulse)
                }
            }

            // Animating token glides along its path.
            if (animating != null) {
                val t = ((frameNanos - animating.startNanos).toFloat() / animating.durationNanos * (animating.path.size - 1)).coerceIn(0f, (animating.path.size - 1).toFloat())
                val i0 = t.toInt().coerceIn(0, animating.path.size - 1)
                val i1 = (i0 + 1).coerceAtMost(animating.path.size - 1)
                val frac = t - i0
                val p0 = animating.path[i0]
                val p1 = animating.path[i1]
                val center = Offset(lerpF(p0.x, p1.x, frac) * c, lerpF(p0.y, p1.y, frac) * c)
                drawToken(center, LudoColors.of(animating.color), LudoColors.dark(animating.color), c * 0.38f, false, 0f)
            }

            // Capture particles.
            for (p in particles) {
                val a = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(LudoColors.red.copy(alpha = a), radius = c * 0.12f * a + c * 0.03f, center = Offset(p.x, p.y))
            }
        }
    }
}

// ---------------------------- drawing helpers ----------------------------

private fun DrawScope.drawYards(c: Float) {
    val corners = mapOf(
        LudoColor.RED to Offset(0f, 0f),
        LudoColor.GREEN to Offset(9f, 0f),
        LudoColor.YELLOW to Offset(9f, 9f),
        LudoColor.BLUE to Offset(0f, 9f),
    )
    for ((color, origin) in corners) {
        val tl = Offset(origin.x * c, origin.y * c)
        val sz = Size(6 * c, 6 * c)
        // Outer colour block with a subtle dark rim for a raised look.
        drawRoundRect(LudoColors.dark(color), Offset(tl.x, tl.y + c * 0.08f), sz, CornerRadius(c * 0.6f))
        drawRoundRect(LudoColors.of(color), tl, sz, CornerRadius(c * 0.6f))
        // Inner light pocket.
        val pad = c * 0.85f
        drawRoundRect(
            LudoColors.light(color),
            Offset(tl.x + pad, tl.y + pad),
            Size(sz.width - pad * 2, sz.height - pad * 2),
            CornerRadius(c * 0.5f),
        )
        // Four home slots (recessed circles).
        LudoBoard.yardSlots.getValue(color).forEach { slot ->
            val center = Offset((slot.x + 0.5f) * c, (slot.y + 0.5f) * c)
            drawCircle(Color.White, c * 0.46f, center)
            drawCircle(LudoColors.tint(color), c * 0.46f, center)
            drawCircle(LudoColors.of(color).copy(alpha = 0.5f), c * 0.46f, center, style = Stroke(width = c * 0.05f))
        }
    }
}

/** The four coloured home-entry arrows at the arm tips, pointing outward. */
private fun DrawScope.drawArrows(c: Float) {
    arrow(Offset(7.5f * c, 0.5f * c), ArrowDir.UP, LudoColors.green, c)
    arrow(Offset(7.5f * c, 14.5f * c), ArrowDir.DOWN, LudoColors.blue, c)
    arrow(Offset(0.5f * c, 7.5f * c), ArrowDir.LEFT, LudoColors.red, c)
    arrow(Offset(14.5f * c, 7.5f * c), ArrowDir.RIGHT, LudoColors.yellow, c)
}

private enum class ArrowDir { UP, DOWN, LEFT, RIGHT }

private fun DrawScope.arrow(center: Offset, dir: ArrowDir, color: Color, c: Float) {
    val s = c * 0.3f
    val path = Path()
    when (dir) {
        ArrowDir.UP -> {
            path.moveTo(center.x, center.y - s)
            path.lineTo(center.x + s * 0.85f, center.y + s * 0.6f)
            path.lineTo(center.x - s * 0.85f, center.y + s * 0.6f)
        }
        ArrowDir.DOWN -> {
            path.moveTo(center.x, center.y + s)
            path.lineTo(center.x + s * 0.85f, center.y - s * 0.6f)
            path.lineTo(center.x - s * 0.85f, center.y - s * 0.6f)
        }
        ArrowDir.LEFT -> {
            path.moveTo(center.x - s, center.y)
            path.lineTo(center.x + s * 0.6f, center.y + s * 0.85f)
            path.lineTo(center.x + s * 0.6f, center.y - s * 0.85f)
        }
        ArrowDir.RIGHT -> {
            path.moveTo(center.x + s, center.y)
            path.lineTo(center.x - s * 0.6f, center.y + s * 0.85f)
            path.lineTo(center.x - s * 0.6f, center.y - s * 0.85f)
        }
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawTrack(c: Float) {
    LudoBoard.ring.forEachIndexed { index, cellPos ->
        val tl = Offset(cellPos.x * c, cellPos.y * c)
        val fill = startFillForRingIndex(index) ?: LudoColors.cellLight
        drawRect(fill, tl, Size(c, c))
        drawRect(LudoColors.boardLine, tl, Size(c, c), style = Stroke(width = c * 0.045f))
        if (index in LudoBoard.safeRingIndices && startFillForRingIndex(index) == null) {
            drawStar(Offset((cellPos.x + 0.5f) * c, (cellPos.y + 0.5f) * c), c * 0.3f, LudoColors.boardLine)
        }
    }
}

private fun startFillForRingIndex(index: Int): Color? {
    for ((color, offset) in LudoBoard.startOffset) {
        if (offset == index) return LudoColors.tint(color)
    }
    return null
}

private fun DrawScope.drawHomeColumns(c: Float) {
    for ((color, cells) in LudoBoard.homeColumn) {
        cells.forEach { cellPos ->
            val tl = Offset(cellPos.x * c, cellPos.y * c)
            drawRect(LudoColors.of(color), tl, Size(c, c))
            drawRect(LudoColors.boardLine, tl, Size(c, c), style = Stroke(width = c * 0.045f))
        }
    }
}

private fun DrawScope.drawCenter(c: Float) {
    val l = 6 * c
    val t = 6 * c
    val r = 9 * c
    val b = 9 * c
    val mid = Offset(7.5f * c, 7.5f * c)
    // Four triangles meeting at the centre, one per colour.
    fun tri(p1: Offset, p2: Offset, color: LudoColor) {
        val path = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(mid.x, mid.y); close() }
        drawPath(path, LudoColors.of(color))
    }
    tri(Offset(l, t), Offset(r, t), LudoColor.GREEN)   // top
    tri(Offset(r, t), Offset(r, b), LudoColor.YELLOW)  // right
    tri(Offset(r, b), Offset(l, b), LudoColor.BLUE)    // bottom
    tri(Offset(l, b), Offset(l, t), LudoColor.RED)     // left
    drawRect(LudoColors.boardLine, Offset(l, t), Size(3 * c, 3 * c), style = Stroke(width = c * 0.06f))
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = (-90 + i * 36) * (Math.PI / 180).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color.copy(alpha = 0.5f), style = Stroke(width = radius * 0.22f))
}

/** Draws a glossy 3D-ish Ludo pawn centred at [center] ([r] ~ half a cell). */
private fun DrawScope.drawToken(center: Offset, color: Color, dark: Color, r: Float, legal: Boolean, pulse: Float) {
    val cx = center.x
    val cy = center.y
    if (legal) {
        drawCircle(color.copy(alpha = 0.30f + pulse * 0.25f), r * (1.3f + pulse * 0.35f), center)
    }
    // Ground shadow.
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(cx - r * 0.6f, cy + r * 0.55f), Size(r * 1.2f, r * 0.38f))
    // Base disc.
    drawOval(dark, Offset(cx - r * 0.6f, cy + r * 0.38f), Size(r * 1.2f, r * 0.4f))
    drawOval(color, Offset(cx - r * 0.55f, cy + r * 0.33f), Size(r * 1.1f, r * 0.36f))
    // Body (bell shape from base up to the neck).
    val body = Path().apply {
        moveTo(cx - r * 0.5f, cy + r * 0.48f)
        cubicTo(cx - r * 0.46f, cy + r * 0.02f, cx - r * 0.3f, cy - r * 0.12f, cx - r * 0.24f, cy - r * 0.38f)
        lineTo(cx + r * 0.24f, cy - r * 0.38f)
        cubicTo(cx + r * 0.3f, cy - r * 0.12f, cx + r * 0.46f, cy + r * 0.02f, cx + r * 0.5f, cy + r * 0.48f)
        close()
    }
    drawPath(body, dark)
    val bodyInner = Path().apply {
        moveTo(cx - r * 0.4f, cy + r * 0.44f)
        cubicTo(cx - r * 0.36f, cy + r * 0.02f, cx - r * 0.24f, cy - r * 0.1f, cx - r * 0.18f, cy - r * 0.34f)
        lineTo(cx + r * 0.18f, cy - r * 0.34f)
        cubicTo(cx + r * 0.24f, cy - r * 0.1f, cx + r * 0.36f, cy + r * 0.02f, cx + r * 0.4f, cy + r * 0.44f)
        close()
    }
    drawPath(bodyInner, color)
    // Head.
    val headC = Offset(cx, cy - r * 0.58f)
    drawCircle(dark, r * 0.46f, headC)
    drawCircle(color, r * 0.4f, headC)
    // Gloss highlight.
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.14f, Offset(cx - r * 0.13f, cy - r * 0.7f))
}

// ---------------------------- position helpers ----------------------------

/** Resting cell-unit centre (x+0.5, y+0.5) for a token at [progress]. */
private fun restingCenterUnits(color: LudoColor, index: Int, progress: Int): Offset = when {
    progress < 0 -> {
        val s = LudoBoard.yardSlots.getValue(color)[index]
        Offset(s.x + 0.5f, s.y + 0.5f)
    }
    progress == LudoBoard.FINISH -> finishedCenterUnits(color, index)
    else -> {
        val cellPos = LudoBoard.cellFor(color, progress)
        Offset(cellPos.x + 0.5f, cellPos.y + 0.5f)
    }
}

/** Finished tokens cluster near the centre on their colour's side. */
private fun finishedCenterUnits(color: LudoColor, index: Int): Offset {
    val base = when (color) {
        LudoColor.RED -> Offset(6.6f, 7.5f)
        LudoColor.GREEN -> Offset(7.5f, 6.6f)
        LudoColor.YELLOW -> Offset(8.4f, 7.5f)
        LudoColor.BLUE -> Offset(7.5f, 8.4f)
    }
    val spread = 0.22f
    val dx = ((index % 2) - 0.5f) * spread
    val dy = ((index / 2) - 0.5f) * spread
    return Offset(base.x + dx, base.y + dy)
}

private fun tokenCenterPx(color: LudoColor, index: Int, progress: Int, cell: Float): Offset {
    val u = restingCenterUnits(color, index, progress)
    return Offset(u.x * cell, u.y * cell)
}

private fun cellKeyOf(color: LudoColor, index: Int, progress: Int): Long {
    // Tokens that visually rest on the same spot get stacked; yard/finish keyed per colour+index.
    return when {
        progress in 0..LudoBoard.LAST_RING -> {
            val abs = LudoBoard.ringIndex(color, progress)
            1_000L + abs
        }
        progress in 51..55 -> 10_000L + color.ordinal * 10 + progress
        progress == LudoBoard.FINISH -> 20_000L + color.ordinal * 10 + index
        else -> 30_000L + color.ordinal * 10 + index // yard
    }
}

private fun stackOffset(stackIndex: Int, count: Int, cell: Float): Offset {
    if (count <= 1) return Offset.Zero
    val d = cell * 0.16f
    return when (stackIndex % 4) {
        0 -> Offset(-d, -d)
        1 -> Offset(d, -d)
        2 -> Offset(-d, d)
        else -> Offset(d, d)
    }
}

private fun buildPath(ev: LudoMoveAnim): List<Offset> {
    if (ev.entered) {
        val yard = LudoBoard.yardSlots.getValue(ev.color)[ev.tokenIndex]
        val start = LudoBoard.cellFor(ev.color, 0)
        return listOf(Offset(yard.x + 0.5f, yard.y + 0.5f), Offset(start.x + 0.5f, start.y + 0.5f))
    }
    val out = ArrayList<Offset>()
    for (p in ev.fromProgress..ev.toProgress) {
        val cellPos = LudoBoard.cellFor(ev.color, p)
        out.add(Offset(cellPos.x + 0.5f, cellPos.y + 0.5f))
    }
    return if (out.size >= 2) out else listOf(out.firstOrNull() ?: Offset.Zero, out.firstOrNull() ?: Offset.Zero)
}

// ---------------------------- tap detection ----------------------------

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapToken(
    cell: Float,
    legalCenters: () -> List<Pair<Int, Offset>>,
    onHit: (Int) -> Unit,
) {
    detectTapGestures { pos ->
        val centers = legalCenters()
        var best = -1
        var bestDist = cell * 0.9f
        centers.forEach { (idx, center) ->
            val dx = pos.x - center.x
            val dy = pos.y - center.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < bestDist) {
                bestDist = d
                best = idx
            }
        }
        if (best >= 0) onHit(best)
    }
}
