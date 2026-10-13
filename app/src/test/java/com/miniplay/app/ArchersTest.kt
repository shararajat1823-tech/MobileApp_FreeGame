package com.miniplay.app

import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.games.archers.ArcherAI
import com.miniplay.app.games.archers.ArcherMatch
import com.miniplay.app.games.archers.ArcherPhase
import com.miniplay.app.games.archers.ArcherRules
import com.miniplay.app.games.archers.ArcherWeapon
import com.miniplay.app.games.archers.BodyPart
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun rad(deg: Double) = Math.toRadians(deg).toFloat()

private fun ArcherMatch.tickUntil(maxSteps: Int = 4000, predicate: (ArcherMatch) -> Boolean) {
    var n = 0
    while (n < maxSteps && !predicate(this)) {
        tick()
        n++
    }
}

class ArcherPhysicsTest {

    @Test fun ragdollHasExpectedParts() {
        val m = ArcherMatch()
        val r = m.archers[0].ragdoll
        assertEquals(BodyPart.HEAD, r.partOf(r.head))
        assertEquals(BodyPart.TORSO, r.partOf(r.chest))
        assertEquals(BodyPart.TORSO, r.partOf(r.pelvis))
        assertEquals(BodyPart.LIMB, r.partOf(r.hand))
        assertEquals(BodyPart.LIMB, r.partOf(r.footFront))
    }

    @Test fun ragdollsStandStableAndDoNotExplodeOrSinkThroughGround() {
        val m = ArcherMatch()
        val cfg = m.config.world
        repeat(600) { m.tick() } // ~5s with no shots
        for (archer in m.archers) {
            val r = archer.ragdoll
            // Upright: head above chest above pelvis (smaller y = higher on screen).
            assertTrue(r.head.y < r.chest.y, "head should stay above chest")
            assertTrue(r.chest.y < r.pelvis.y, "chest should stay above pelvis")
            // No tunnelling through the ground.
            for (p in r.points) {
                assertTrue(p.y <= cfg.groundY + 0.05f, "point sank through ground: ${p.y}")
            }
            // Joints stay near their rest length (no exploding constraints).
            for (s in r.sticks) {
                val d = hypot(s.a.x - s.b.x, s.a.y - s.b.y)
                assertTrue(kotlin.math.abs(d - s.length) < s.length * 0.4f + 0.05f, "stick drifted: $d vs ${s.length}")
            }
            assertTrue(archer.alive)
        }
    }

    @Test fun firingAnArrowResolvesAndSwitchesTurn() {
        val m = ArcherMatch()
        assertEquals(0, m.current)
        m.fire(rad(80.0), power = 0.3f, weapon = ArcherWeapon.STANDARD) // weak, steep -> lands as a miss
        assertEquals(ArcherPhase.FIRING, m.phase)
        m.tickUntil { it.phase == ArcherPhase.AIMING || it.phase == ArcherPhase.GAME_OVER }
        assertEquals(ArcherPhase.AIMING, m.phase)
        assertEquals(1, m.current, "turn should pass to the opponent after a shot")
    }

    @Test fun missLeavesBothArchersAtFullHealth() {
        val m = ArcherMatch()
        m.fire(rad(80.0), 0.3f, ArcherWeapon.STANDARD)
        m.tickUntil { it.phase == ArcherPhase.AIMING }
        assertEquals(m.config.maxHealth, m.archers[0].health)
        assertEquals(m.config.maxHealth, m.archers[1].health)
    }

    @Test fun simulationIsDeterministicForTheSameInputs() {
        fun run(): Float {
            val m = ArcherMatch()
            m.fire(rad(32.0), 0.85f, ArcherWeapon.STANDARD)
            m.tickUntil { it.phase == ArcherPhase.AIMING || it.phase == ArcherPhase.GAME_OVER }
            return m.archers[1].health
        }
        assertEquals(run(), run())
    }
}

class ArcherRulesTest {

    private val cfg = com.miniplay.app.games.archers.ArcherMatchConfig()

    @Test fun headshotsHurtMoreThanTorsoMoreThanLimb() {
        val speed = cfg.refStepSpeed
        val head = ArcherRules.damage(BodyPart.HEAD, ArcherWeapon.STANDARD, speed, cfg)
        val torso = ArcherRules.damage(BodyPart.TORSO, ArcherWeapon.STANDARD, speed, cfg)
        val limb = ArcherRules.damage(BodyPart.LIMB, ArcherWeapon.STANDARD, speed, cfg)
        assertTrue(head > torso, "head ($head) should beat torso ($torso)")
        assertTrue(torso > limb, "torso ($torso) should beat limb ($limb)")
    }

    @Test fun fasterArrowsAndHeavierWeaponsHitHarder() {
        val slow = ArcherRules.damage(BodyPart.TORSO, ArcherWeapon.STANDARD, cfg.refStepSpeed * 0.5f, cfg)
        val fast = ArcherRules.damage(BodyPart.TORSO, ArcherWeapon.STANDARD, cfg.refStepSpeed * 1.5f, cfg)
        assertTrue(fast > slow, "faster arrow should hit harder")
        val std = ArcherRules.damage(BodyPart.TORSO, ArcherWeapon.STANDARD, cfg.refStepSpeed, cfg)
        val heavy = ArcherRules.damage(BodyPart.TORSO, ArcherWeapon.HEAVY, cfg.refStepSpeed, cfg)
        assertTrue(heavy > std, "heavy weapon should hit harder")
    }
}

class ArcherMatchWinTest {

    @Test fun winnerIsDeclaredWhenAnOpponentIsOutOfHealth() {
        val m = ArcherMatch()
        m.archers[1].health = 0f // opponent is already down
        m.fire(rad(80.0), 0.3f, ArcherWeapon.STANDARD) // player's turn ends with a harmless shot
        m.tickUntil { it.phase == ArcherPhase.GAME_OVER }
        assertEquals(ArcherPhase.GAME_OVER, m.phase)
        assertEquals(0, m.winner)
        assertTrue(!m.archers[1].ragdoll.balanced, "a defeated archer collapses")
    }

    @Test fun noMoreShotsAfterGameOver() {
        val m = ArcherMatch()
        m.archers[1].health = 0f
        m.fire(rad(80.0), 0.3f, ArcherWeapon.STANDARD)
        m.tickUntil { it.phase == ArcherPhase.GAME_OVER }
        val before = m.phase
        m.fire(rad(30.0), 0.9f, ArcherWeapon.STANDARD) // should be ignored
        assertEquals(before, m.phase)
        assertEquals(null, m.arrow)
    }
}

class ArcherAiTest {

    private fun meanMiss(difficulty: GameDifficulty): Float {
        val n = 30
        var total = 0f
        for (seed in 0 until n) {
            val m = ArcherMatch()
            // Pass the turn to the AI (player takes a harmless shot).
            m.fire(rad(80.0), 0.3f, ArcherWeapon.STANDARD)
            m.tickUntil { it.phase == ArcherPhase.AIMING || it.phase == ArcherPhase.GAME_OVER }
            if (m.current != 1) continue
            val shot = ArcherAI.chooseShot(m, difficulty, Random(seed.toLong()))
            val target = m.archers[0].ragdoll.chest
            val samples = m.previewTrajectory(shot.angle, shot.power, ArcherWeapon.STANDARD, maxSamples = 60)
            val minDist = samples.minOfOrNull { hypot(it.first - target.x, it.second - target.y) } ?: Float.MAX_VALUE
            total += minDist
        }
        return total / n
    }

    @Test fun harderOpponentsAimCloser() {
        val easy = meanMiss(GameDifficulty.EASY)
        val normal = meanMiss(GameDifficulty.MEDIUM)
        val hard = meanMiss(GameDifficulty.HARD)
        assertTrue(hard < normal, "hard ($hard) should aim closer than normal ($normal)")
        assertTrue(normal < easy, "normal ($normal) should aim closer than easy ($easy)")
    }

    @Test fun aiShotIsWithinValidBounds() {
        val m = ArcherMatch()
        m.fire(rad(80.0), 0.3f, ArcherWeapon.STANDARD)
        m.tickUntil { it.phase == ArcherPhase.AIMING }
        val shot = ArcherAI.chooseShot(m, GameDifficulty.HARD, Random(1))
        assertTrue(shot.power in 0.3f..1f)
    }
}
