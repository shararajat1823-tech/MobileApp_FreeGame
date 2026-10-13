package com.miniplay.app.games.archers

import com.miniplay.app.domain.model.GameDifficulty
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** A validated shot: everything the match needs, and everything a future online transport would send. */
data class ArcherShot(val angle: Float, val power: Float, val weaponId: String = ArcherWeapon.STANDARD.id)

/**
 * On-device opponent. It searches a grid of (angle, power) candidates using the
 * same projectile model as the game, scores each by how close it passes to the
 * target, then adds difficulty-scaled error so it is challenging but beatable —
 * never perfect, never cheating with future dice/physics. Fully deterministic
 * given its [Random].
 */
object ArcherAI {

    fun chooseShot(
        match: ArcherMatch,
        difficulty: GameDifficulty,
        random: Random = Random.Default,
        weapon: ArcherWeapon = ArcherWeapon.STANDARD,
    ): ArcherShot {
        val (sx, sy) = match.bowAnchor()
        val opp = match.archers[1 - match.current].ragdoll
        val aimHead = difficulty == GameDifficulty.HARD
        val tx = if (aimHead) opp.head.x else opp.chest.x
        val ty = if (aimHead) opp.head.y else opp.chest.y

        val (angStepDeg, powStep) = when (difficulty) {
            GameDifficulty.EASY -> 9f to 0.12f
            GameDifficulty.MEDIUM -> 5f to 0.08f
            GameDifficulty.HARD -> 2.5f to 0.04f
        }

        var bestAngle = Math.toRadians(135.0).toFloat()
        var bestPower = 0.7f
        var bestDist = Float.MAX_VALUE
        var angDeg = 15f
        while (angDeg <= 165f) {
            val ang = Math.toRadians(angDeg.toDouble()).toFloat()
            var p = 0.45f
            while (p <= 1.0f) {
                val d = closestApproach(match, sx, sy, ang, p, weapon, tx, ty)
                if (d < bestDist) {
                    bestDist = d
                    bestAngle = ang
                    bestPower = p
                }
                p += powStep
            }
            angDeg += angStepDeg
        }

        val (angJitDeg, powJit, flailChance) = when (difficulty) {
            GameDifficulty.EASY -> Triple(17f, 0.2f, 0.25f)
            GameDifficulty.MEDIUM -> Triple(6f, 0.08f, 0f)
            GameDifficulty.HARD -> Triple(1.5f, 0.03f, 0f)
        }

        var angle = bestAngle
        var power = bestPower
        if (flailChance > 0f && random.nextFloat() < flailChance) {
            // An easy opponent sometimes flubs the shot entirely.
            angle += Math.toRadians(((random.nextFloat() * 2f - 1f) * angJitDeg * 2f).toDouble()).toFloat()
            power = (power + (random.nextFloat() * 2f - 1f) * powJit * 2f)
        } else {
            angle += Math.toRadians(((random.nextFloat() * 2f - 1f) * angJitDeg).toDouble()).toFloat()
            power += (random.nextFloat() * 2f - 1f) * powJit
        }
        return ArcherShot(angle = angle, power = power.coerceIn(0.3f, 1f), weaponId = weapon.id)
    }

    /** Minimum distance the projectile passes from (tx,ty); no collision, pure. */
    private fun closestApproach(
        match: ArcherMatch,
        sx: Float,
        sy: Float,
        angle: Float,
        power: Float,
        weapon: ArcherWeapon,
        tx: Float,
        ty: Float,
    ): Float {
        val cfg = match.config.world
        val speedWorld = (match.config.minSpeed + power * (match.config.maxSpeed - match.config.minSpeed)) * weapon.speed
        val v = speedWorld * cfg.dt
        val g = cfg.gravity * cfg.dt * cfg.dt
        var x = sx + cos(angle) * 0.35f
        var y = sy - sin(angle) * 0.35f
        var vx = cos(angle) * v
        var vy = -sin(angle) * v
        var best = Float.MAX_VALUE
        var steps = 0
        while (steps < 2000) {
            vy += g
            x += vx
            y += vy
            val d = hypot(x - tx, y - ty)
            if (d < best) best = d
            if (y >= cfg.groundY || x < cfg.minX || x > cfg.maxX) break
            steps++
        }
        return best
    }
}
