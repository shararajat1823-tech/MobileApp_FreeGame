package com.miniplay.app

import com.miniplay.app.games.brickbreaker.Ball
import com.miniplay.app.games.brickbreaker.BrickBreaker
import com.miniplay.app.games.brickbreaker.BrickStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrickWorldTest {

    @Test fun newGameIsReadyWithLivesAndBricks() {
        val w = BrickBreaker.newGame(level = 1)
        assertEquals(BrickStatus.READY, w.status)
        assertEquals(BrickBreaker.START_LIVES, w.lives)
        assertTrue(w.bricksRemaining > 0)
        assertEquals(0f, w.ball.vx)
        assertEquals(0f, w.ball.vy)
    }

    @Test fun stepIsNoOpUntilLaunched() {
        val w = BrickBreaker.newGame()
        assertEquals(w, BrickBreaker.step(w, 0.016f))
    }

    @Test fun launchGivesBallUpwardVelocity() {
        val w = BrickBreaker.launch(BrickBreaker.newGame())
        assertEquals(BrickStatus.RUNNING, w.status)
        assertTrue(w.ball.vy < 0f, "ball should move up after launch")
    }

    @Test fun ballBouncesOffLeftWall() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        // Ball heading into the left wall near the top (above bricks).
        val w = base.copy(
            ball = Ball(x = 1f, y = 5f, vx = -40f, vy = -20f, radius = 1.6f),
            bricks = emptyList(),
        )
        val next = BrickBreaker.step(w, 0.05f)
        assertTrue(next.ball.vx > 0f, "x velocity should flip to positive")
        assertTrue(next.ball.x >= next.ball.radius - 0.001f)
    }

    @Test fun ballBouncesOffTopWall() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        val w = base.copy(
            ball = Ball(x = 50f, y = 1f, vx = 10f, vy = -40f, radius = 1.6f),
            bricks = emptyList(),
        )
        val next = BrickBreaker.step(w, 0.05f)
        assertTrue(next.ball.vy > 0f, "y velocity should flip downward")
    }

    @Test fun losingBallCostsALife() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        val w = base.copy(
            ball = Ball(x = 50f, y = base.height - 0.5f, vx = 0f, vy = 50f, radius = 1.6f),
            bricks = emptyList(),
        )
        val next = BrickBreaker.step(w, 0.1f)
        assertEquals(base.lives - 1, next.lives)
        assertEquals(BrickStatus.LIFE_LOST, next.status)
    }

    @Test fun lastLifeLossIsGameOver() {
        val base = BrickBreaker.launch(BrickBreaker.newGame()).copy(lives = 1)
        val w = base.copy(
            ball = Ball(x = 50f, y = base.height - 0.5f, vx = 0f, vy = 50f, radius = 1.6f),
            bricks = emptyList(),
        )
        val next = BrickBreaker.step(w, 0.1f)
        assertEquals(0, next.lives)
        assertEquals(BrickStatus.GAME_OVER, next.status)
    }

    @Test fun hittingBrickScoresAndRemovesIt() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        val brick = base.bricks.first()
        // Put the ball right at the brick centre, moving up into it.
        val w = base.copy(
            ball = Ball(
                x = brick.x + brick.width / 2f,
                y = brick.y + brick.height / 2f,
                vx = 0f,
                vy = -30f,
                radius = 1.6f,
            ),
            bricks = listOf(brick),
        )
        val next = BrickBreaker.step(w, 0.001f)
        assertEquals(0, next.bricksRemaining)
        assertEquals(brick.points, next.score)
        assertEquals(BrickStatus.LEVEL_CLEARED, next.status)
    }

    @Test fun clearingAllBricksClearsLevel_thenNextLevelAddsBricks() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        val brick = base.bricks.first()
        val w = base.copy(
            ball = Ball(brick.x + brick.width / 2f, brick.y + brick.height / 2f, 0f, -30f, 1.6f),
            bricks = listOf(brick),
        )
        val cleared = BrickBreaker.step(w, 0.001f)
        assertEquals(BrickStatus.LEVEL_CLEARED, cleared.status)
        val advanced = BrickBreaker.nextLevel(cleared)
        assertEquals(2, advanced.level)
        assertTrue(advanced.bricksRemaining > 0)
        assertEquals(BrickStatus.READY, advanced.status)
    }

    @Test fun paddleKeepsBallInPlay() {
        val base = BrickBreaker.launch(BrickBreaker.newGame())
        val paddle = base.paddle
        // Keep bricks present so the level isn't trivially "cleared".
        val w = base.copy(
            ball = Ball(x = paddle.centerX, y = paddle.y - 0.5f, vx = 5f, vy = 30f, radius = 1.6f),
        )
        val next = BrickBreaker.step(w, 0.01f)
        assertTrue(next.ball.vy < 0f, "ball should bounce back up off the paddle")
        assertEquals(BrickStatus.RUNNING, next.status)
    }
}
