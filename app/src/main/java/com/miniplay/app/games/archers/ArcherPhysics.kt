package com.miniplay.app.games.archers

import kotlin.math.hypot

/**
 * A Verlet particle: position plus previous position (velocity is their
 * difference). Pure data + simple mutation; no Android, no per-step allocation.
 */
class PhysicsPoint(
    var x: Float,
    var y: Float,
    val radius: Float = 0.12f,
    var pinned: Boolean = false,
) {
    var px: Float = x
    var py: Float = y

    val vx: Float get() = x - px
    val vy: Float get() = y - py

    /** Sets velocity by moving the previous position (Verlet idiom). */
    fun setVelocity(nvx: Float, nvy: Float) {
        px = x - nvx
        py = y - nvy
    }

    /** Adds an instantaneous impulse (change in velocity). */
    fun addImpulse(ix: Float, iy: Float) {
        if (pinned) return
        px -= ix
        py -= iy
    }
}

/** A distance constraint holding two points at [length] apart. */
class PhysicsStick(
    val a: PhysicsPoint,
    val b: PhysicsPoint,
    var length: Float,
    val stiffness: Float = 1f,
)

/** World tuning. A fixed [dt] keeps the simulation framerate-independent. */
data class WorldConfig(
    val gravity: Float = 22f,
    val groundY: Float = 8.4f,
    val minX: Float = 0.2f,
    val maxX: Float = 15.8f,
    val damping: Float = 0.99f,
    val groundFriction: Float = 0.72f,
    val iterations: Int = 8,
    val dt: Float = 1f / 120f,
) {
    val width: Float get() = 16f
    val height: Float get() = 9f
}

/** Pure Verlet integrator + constraint solver. Deterministic for a fixed [cfg]. */
object Verlet {

    fun integrate(points: List<PhysicsPoint>, cfg: WorldConfig) {
        val g = cfg.gravity * cfg.dt * cfg.dt
        for (p in points) {
            if (p.pinned) continue
            val nvx = (p.x - p.px) * cfg.damping
            val nvy = (p.y - p.py) * cfg.damping
            p.px = p.x
            p.py = p.y
            p.x += nvx
            p.y += nvy + g
        }
    }

    fun solve(sticks: List<PhysicsStick>, points: List<PhysicsPoint>, cfg: WorldConfig) {
        repeat(cfg.iterations) {
            for (s in sticks) solveStick(s)
        }
        for (p in points) collide(p, cfg)
    }

    private fun solveStick(s: PhysicsStick) {
        val dx = s.b.x - s.a.x
        val dy = s.b.y - s.a.y
        val d = hypot(dx, dy)
        if (d < 1e-5f) return
        val diff = (s.length - d) / d * 0.5f * s.stiffness
        val ox = dx * diff
        val oy = dy * diff
        if (!s.a.pinned) { s.a.x -= ox; s.a.y -= oy }
        if (!s.b.pinned) { s.b.x += ox; s.b.y += oy }
    }

    private fun collide(p: PhysicsPoint, cfg: WorldConfig) {
        if (p.pinned) return
        // Ground.
        val floor = cfg.groundY - p.radius
        if (p.y > floor) {
            p.y = floor
            // Tangential friction: damp horizontal motion while grounded.
            val tvx = (p.x - p.px) * cfg.groundFriction
            p.px = p.x - tvx
        }
        // Walls.
        if (p.x < cfg.minX + p.radius) {
            p.x = cfg.minX + p.radius
            p.px = p.x
        } else if (p.x > cfg.maxX - p.radius) {
            p.x = cfg.maxX - p.radius
            p.px = p.x
        }
    }
}

/** Which part of the body an arrow struck — drives damage and feedback. */
enum class BodyPart { HEAD, TORSO, LIMB }

/**
 * A humanoid ragdoll built from Verlet points and sticks. While [balanced] the
 * feet are pinned so the archer stands and springs back from hits (a lively hit
 * reaction); when defeated it unpins and crumples under gravity + impulses.
 */
class Ragdoll(
    val facingRight: Boolean,
    baseX: Float,
    groundY: Float,
) {
    val head = PhysicsPoint(baseX, groundY - 1.58f, radius = 0.22f)
    val chest = PhysicsPoint(baseX, groundY - 1.15f, radius = 0.20f)
    val pelvis = PhysicsPoint(baseX, groundY - 0.75f, radius = 0.18f, pinned = true)
    private val handDir = if (facingRight) 0.42f else -0.42f
    val hand = PhysicsPoint(baseX + handDir, groundY - 1.1f, radius = 0.12f)
    val footFront = PhysicsPoint(baseX + 0.22f, groundY, radius = 0.12f, pinned = true)
    val footBack = PhysicsPoint(baseX - 0.22f, groundY, radius = 0.12f, pinned = true)

    val points: List<PhysicsPoint> = listOf(head, chest, pelvis, hand, footFront, footBack)

    // Standing "home" pose captured at construction; used to keep the archer
    // upright while alive (a distance-constraint chain alone would just collapse).
    private val homeX = FloatArray(points.size) { points[it].x }
    private val homeY = FloatArray(points.size) { points[it].y }

    val sticks: List<PhysicsStick> = listOf(
        PhysicsStick(head, chest, dist(head, chest)),
        PhysicsStick(chest, pelvis, dist(chest, pelvis)),
        PhysicsStick(pelvis, footFront, dist(pelvis, footFront), stiffness = 0.9f),
        PhysicsStick(pelvis, footBack, dist(pelvis, footBack), stiffness = 0.9f),
        PhysicsStick(chest, hand, dist(chest, hand), stiffness = 0.7f),
        // Braces keep the torso from folding flat while alive.
        PhysicsStick(head, pelvis, dist(head, pelvis), stiffness = 0.6f),
        PhysicsStick(footFront, footBack, dist(footFront, footBack), stiffness = 0.5f),
    )

    var balanced: Boolean = true
        private set

    /** Marks the ragdoll defeated: release the feet so it falls. */
    fun collapse() {
        if (!balanced) return
        balanced = false
        pelvis.pinned = false
        footFront.pinned = false
        footBack.pinned = false
    }

    /**
     * While alive, eases the upper body back toward its standing pose so the
     * archer stays upright (a distance-constraint chain on pinned feet would
     * otherwise just fold under gravity). Both current and previous position move
     * by the same delta, so this injects no velocity/jitter — an arrow impact
     * still visibly knocks the body and it springs back for a lively reaction.
     */
    fun stabilize(strength: Float = 0.2f, velocityRetain: Float = 0.5f) {
        if (!balanced) return
        // Head (0), chest (1) and hand (3) sway; pelvis (2) and feet are pinned.
        for (i in intArrayOf(0, 1, 3)) {
            val p = points[i]
            val dx = (homeX[i] - p.x) * strength
            val dy = (homeY[i] - p.y) * strength
            // Position spring without changing velocity (move x and px together)...
            p.x += dx; p.px += dx
            p.y += dy; p.py += dy
            // ...then bleed off velocity so it settles instead of oscillating.
            val vx = p.x - p.px
            val vy = p.y - p.py
            p.px = p.x - vx * velocityRetain
            p.py = p.y - vy * velocityRetain
        }
    }

    fun partOf(point: PhysicsPoint): BodyPart = when (point) {
        head -> BodyPart.HEAD
        chest, pelvis -> BodyPart.TORSO
        else -> BodyPart.LIMB
    }

    /** Nearest body point to (x,y) within [reach], or null. */
    fun hitPoint(x: Float, y: Float, reach: Float): PhysicsPoint? {
        var best: PhysicsPoint? = null
        var bestD = Float.MAX_VALUE
        for (p in points) {
            val d = hypot(p.x - x, p.y - y)
            if (d < p.radius + reach && d < bestD) {
                bestD = d
                best = p
            }
        }
        return best
    }

    private companion object {
        fun dist(a: PhysicsPoint, b: PhysicsPoint) = hypot(a.x - b.x, a.y - b.y)
    }
}

/** An in-flight or embedded arrow. Angle for drawing comes from its velocity. */
class Arrow(
    var x: Float,
    var y: Float,
    vx: Float,
    vy: Float,
    val weapon: ArcherWeapon,
    val owner: Int,
) {
    var px: Float = x - vx
    var py: Float = y - vy
    var stuck: Boolean = false
    val vx: Float get() = x - px
    val vy: Float get() = y - py
    val speed: Float get() = hypot(x - px, y - py)
}
