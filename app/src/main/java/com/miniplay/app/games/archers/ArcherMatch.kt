package com.miniplay.app.games.archers

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class ArcherPhase { AIMING, FIRING, RESOLVING, GAME_OVER }

/** One competitor: its ragdoll, side (0 = left/player, 1 = right), and health. */
class Archer(val ragdoll: Ragdoll, val side: Int, var health: Float, val maxHealth: Float) {
    val alive: Boolean get() = health > 0f
    val healthFraction: Float get() = (health / maxHealth).coerceIn(0f, 1f)
}

/** Result of an arrow landing, for feedback (sound/particles/damage numbers). */
data class HitInfo(val archerSide: Int, val part: BodyPart, val damage: Float, val x: Float, val y: Float)

/** An arrow at rest, optionally tracking a body point so it rides the ragdoll. */
class EmbeddedArrow(var x: Float, var y: Float, val angle: Float, val follow: PhysicsPoint?, val offX: Float, val offY: Float)

data class ArcherMatchConfig(
    val maxHealth: Float = 100f,
    val world: WorldConfig = WorldConfig(),
    val headMult: Float = 3f,
    val torsoMult: Float = 1.3f,
    val limbMult: Float = 0.7f,
    val baseDamage: Float = 24f,
    val minSpeed: Float = 7f,
    val maxSpeed: Float = 24f,
    val refStepSpeed: Float = 24f / 120f, // per-step speed at full power (reference for damage)
)

/**
 * The authoritative, framework-free Ragdoll Archers match: two ragdolls, health,
 * turn order, a single arrow in flight, part-based damage, knockback and win
 * detection. It is a mutable fixed-timestep simulation ([tick]) rather than an
 * immutable data class because physics is inherently stateful; it stays pure
 * (no Android) and deterministic, so rules are unit-testable on the JVM.
 *
 * Match state is kept separate from presentation and from AI: the UI reads a
 * snapshot each frame, and a future online transport can drive [fire]/[tick]
 * from validated shot data (owner, angle, power, turn) without touching physics.
 */
class ArcherMatch(val config: ArcherMatchConfig = ArcherMatchConfig()) {

    private val cfg get() = config.world

    lateinit var archers: List<Archer>
        private set
    var current: Int = 0
        private set
    var phase: ArcherPhase = ArcherPhase.AIMING
        private set
    var arrow: Arrow? = null
        private set
    var winner: Int? = null
        private set
    var turnCount: Int = 0
        private set
    var shake: Float = 0f
        private set
    var lastHit: HitInfo? = null
    /** Increments on every arrow-to-body hit, so the UI can fire a one-shot cue. */
    var hitCount: Int = 0
        private set

    val embedded = ArrayList<EmbeddedArrow>()

    private var allPoints: List<PhysicsPoint> = emptyList()
    private var allSticks: List<PhysicsStick> = emptyList()
    private var resolveTimer = 0
    private var flightSteps = 0

    init { reset() }

    fun reset() {
        val left = Ragdoll(facingRight = true, baseX = 2.7f, groundY = cfg.groundY)
        val right = Ragdoll(facingRight = false, baseX = 13.3f, groundY = cfg.groundY)
        archers = listOf(
            Archer(left, side = 0, health = config.maxHealth, maxHealth = config.maxHealth),
            Archer(right, side = 1, health = config.maxHealth, maxHealth = config.maxHealth),
        )
        allPoints = archers.flatMap { it.ragdoll.points }
        allSticks = archers.flatMap { it.ragdoll.sticks }
        current = 0
        phase = ArcherPhase.AIMING
        arrow = null
        winner = null
        turnCount = 0
        shake = 0f
        lastHit = null
        embedded.clear()
        resolveTimer = 0
        flightSteps = 0
    }

    val currentArcher: Archer get() = archers[current]
    val isPlayerTurn: Boolean get() = current == 0

    /** Forces whose turn it is while AIMING (used by Practice to keep the player shooting). */
    fun setTurn(side: Int) {
        if (phase == ArcherPhase.AIMING && winner == null) current = side.coerceIn(0, 1)
    }

    /** World-space origin of the current archer's shot (the bow hand). */
    fun bowAnchor(side: Int = current): Pair<Float, Float> {
        val h = archers[side].ragdoll.hand
        return h.x to h.y
    }

    /** Launch an arrow for the current archer. [angle] in radians (0 = +x, up is positive). */
    fun fire(angle: Float, power: Float, weapon: ArcherWeapon) {
        if (phase != ArcherPhase.AIMING || winner != null) return
        val speedWorld = (config.minSpeed + power.coerceIn(0f, 1f) * (config.maxSpeed - config.minSpeed)) * weapon.speed
        val vStep = speedWorld * cfg.dt
        val dx = cos(angle)
        val dy = -sin(angle)
        val (hx, hy) = bowAnchor()
        val sx = hx + dx * 0.35f
        val sy = hy + dy * 0.35f
        arrow = Arrow(sx, sy, dx * vStep, dy * vStep, weapon, owner = current)
        phase = ArcherPhase.FIRING
        flightSteps = 0
    }

    /** Advances the simulation one fixed step. */
    fun tick() {
        if (phase == ArcherPhase.GAME_OVER) {
            stepBodies()
            shake *= 0.9f
            return
        }
        stepBodies()

        when (phase) {
            ArcherPhase.FIRING -> stepArrow()
            ArcherPhase.RESOLVING -> {
                if (--resolveTimer <= 0) finishTurn()
            }
            else -> Unit
        }
        shake *= 0.9f
        // Keep embedded arrows riding their body point.
        for (e in embedded) {
            val f = e.follow ?: continue
            e.x = f.x + e.offX
            e.y = f.y + e.offY
        }
    }

    private fun stepBodies() {
        Verlet.integrate(allPoints, cfg)
        Verlet.solve(allSticks, allPoints, cfg)
        for (archer in archers) archer.ragdoll.stabilize()
    }

    private fun stepArrow() {
        val a = arrow ?: return
        val g = cfg.gravity * cfg.dt * cfg.dt
        // Sub-step for reliable thin-arrow collision.
        val sub = 4
        repeat(sub) {
            if (a.stuck) return@repeat
            val nvx = a.x - a.px
            val nvy = a.y - a.py
            a.px = a.x
            a.py = a.y
            a.x += nvx / sub
            a.y += (nvy + g) / sub
            resolveArrowCollision(a)
        }
        flightSteps++
        if (!a.stuck && (a.x < cfg.minX || a.x > cfg.maxX || flightSteps > 1200)) {
            // Missed: fell off the field or timed out.
            beginResolve()
        }
    }

    private fun resolveArrowCollision(a: Arrow) {
        // Ground.
        if (a.y >= cfg.groundY) {
            a.y = cfg.groundY
            a.stuck = true
            embedded.add(EmbeddedArrow(a.x, a.y, arrowAngle(a), follow = null, offX = 0f, offY = 0f))
            trimEmbedded()
            beginResolve()
            return
        }
        // Bodies (not the owner's).
        for (archer in archers) {
            if (archer.side == a.owner) continue
            val hp = archer.ragdoll.hitPoint(a.x, a.y, reach = 0.08f) ?: continue
            applyHit(a, archer, hp)
            return
        }
    }

    private fun applyHit(a: Arrow, archer: Archer, hp: PhysicsPoint) {
        val part = archer.ragdoll.partOf(hp)
        val mult = when (part) {
            BodyPart.HEAD -> config.headMult
            BodyPart.TORSO -> config.torsoMult
            BodyPart.LIMB -> config.limbMult
        }
        val dmg = ArcherRules.damage(part, a.weapon, a.speed, config)
        archer.health = (archer.health - dmg).coerceAtLeast(0f)

        // Knockback along the arrow's travel.
        val speedFactor = (a.speed / config.refStepSpeed).coerceIn(0.35f, 1.6f)
        val k = 0.6f * a.weapon.knockback * speedFactor
        hp.addImpulse(a.vx * k, a.vy * k)

        shake += (dmg * 0.02f).coerceAtMost(1.2f)
        lastHit = HitInfo(archer.side, part, dmg, a.x, a.y)
        hitCount++

        a.stuck = true
        embedded.add(
            EmbeddedArrow(a.x, a.y, arrowAngle(a), follow = hp, offX = a.x - hp.x, offY = a.y - hp.y),
        )
        trimEmbedded()
        beginResolve()
    }

    private fun beginResolve() {
        arrow = null
        phase = ArcherPhase.RESOLVING
        resolveTimer = RESOLVE_STEPS
    }

    private fun finishTurn() {
        // Collapse any archer whose health is gone.
        var anyDead = false
        for (archer in archers) {
            if (!archer.alive) {
                archer.ragdoll.collapse()
                anyDead = true
            }
        }
        if (anyDead) {
            val alive = archers.filter { it.alive }
            winner = when {
                alive.size == 1 -> alive.first().side
                alive.isEmpty() -> 1 - current // both down: the defender survives the tie
                else -> null
            }
            if (winner != null) {
                phase = ArcherPhase.GAME_OVER
                return
            }
        }
        current = 1 - current
        turnCount++
        phase = ArcherPhase.AIMING
    }

    /**
     * Samples the trajectory a shot would take (no collision), for the aiming
     * preview and Practice mode. Stops at the ground or [maxSamples].
     */
    fun previewTrajectory(angle: Float, power: Float, weapon: ArcherWeapon, maxSamples: Int = 24): List<Pair<Float, Float>> {
        val speedWorld = (config.minSpeed + power.coerceIn(0f, 1f) * (config.maxSpeed - config.minSpeed)) * weapon.speed
        val vStep = speedWorld * cfg.dt
        val g = cfg.gravity * cfg.dt * cfg.dt
        var x = bowAnchor().first + cos(angle) * 0.35f
        var y = bowAnchor().second - sin(angle) * 0.35f
        var vx = cos(angle) * vStep
        var vy = -sin(angle) * vStep
        val out = ArrayList<Pair<Float, Float>>(maxSamples)
        var steps = 0
        val stride = 7
        while (out.size < maxSamples && steps < maxSamples * stride) {
            vy += g
            x += vx
            y += vy
            if (y >= cfg.groundY || x < cfg.minX || x > cfg.maxX) {
                out.add(x.coerceIn(cfg.minX, cfg.maxX) to y.coerceAtMost(cfg.groundY))
                break
            }
            if (steps % stride == 0) out.add(x to y)
            steps++
        }
        return out
    }

    private fun trimEmbedded() {
        while (embedded.size > MAX_EMBEDDED) embedded.removeAt(0)
    }

    private fun arrowAngle(a: Arrow): Float = kotlin.math.atan2(a.y - a.py, a.x - a.px)

    companion object {
        const val RESOLVE_STEPS = 150 // ~1.25s at 120Hz for the body to settle
        const val MAX_EMBEDDED = 12
    }
}

/** Pure damage model, split out so balancing lives in one tested place. */
object ArcherRules {
    fun damage(part: BodyPart, weapon: ArcherWeapon, stepSpeed: Float, config: ArcherMatchConfig): Float {
        val mult = when (part) {
            BodyPart.HEAD -> config.headMult
            BodyPart.TORSO -> config.torsoMult
            BodyPart.LIMB -> config.limbMult
        }
        val speedFactor = (stepSpeed / config.refStepSpeed).coerceIn(0.35f, 1.6f)
        return config.baseDamage * weapon.damage * mult * speedFactor
    }
}

/** Convenience used by AI/tests: straight-line distance between the two archers' hands. */
fun ArcherMatch.spanBetweenArchers(): Float {
    val (ax, ay) = bowAnchor(0)
    val (bx, by) = bowAnchor(1)
    return hypot(bx - ax, by - ay)
}
