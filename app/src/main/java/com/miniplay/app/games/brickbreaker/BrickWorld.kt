package com.miniplay.app.games.brickbreaker

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

/**
 * Pure Brick-Breaker simulation in an abstract coordinate space (origin top-left,
 * x→right, y→down). The ViewModel ticks [step] on a frame clock and maps the
 * world onto the Canvas; all physics, collisions, scoring and life handling live
 * here so they are deterministic and unit-testable (no Android, no real time).
 */

data class Ball(val x: Float, val y: Float, val vx: Float, val vy: Float, val radius: Float)

data class Paddle(val centerX: Float, val y: Float, val width: Float, val height: Float) {
    val left get() = centerX - width / 2f
    val right get() = centerX + width / 2f
}

data class Brick(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val points: Int,
    val alive: Boolean = true,
)

enum class BrickStatus { READY, RUNNING, LIFE_LOST, LEVEL_CLEARED, GAME_OVER }

data class BrickWorld(
    val width: Float,
    val height: Float,
    val ball: Ball,
    val paddle: Paddle,
    val bricks: List<Brick>,
    val lives: Int,
    val score: Int,
    val level: Int,
    val status: BrickStatus,
) {
    val bricksRemaining: Int get() = bricks.count { it.alive }
}

object BrickBreaker {

    const val WORLD_WIDTH = 100f
    const val WORLD_HEIGHT = 150f
    const val START_LIVES = 3

    private const val BALL_RADIUS = 1.6f
    private const val PADDLE_WIDTH = 20f
    private const val PADDLE_HEIGHT = 2.6f
    private const val BASE_SPEED = 60f
    private const val SPEED_PER_LEVEL = 7f
    private const val MAX_BOUNCE_ANGLE = 1.05f // radians (~60°)

    fun newGame(level: Int = 1): BrickWorld {
        val paddle = Paddle(
            centerX = WORLD_WIDTH / 2f,
            y = WORLD_HEIGHT - 8f,
            width = PADDLE_WIDTH,
            height = PADDLE_HEIGHT,
        )
        return BrickWorld(
            width = WORLD_WIDTH,
            height = WORLD_HEIGHT,
            ball = ballOnPaddle(paddle),
            paddle = paddle,
            bricks = buildBricks(level),
            lives = START_LIVES,
            score = 0,
            level = level,
            status = BrickStatus.READY,
        )
    }

    /** Builds the next level, carrying over score and lives. */
    fun nextLevel(world: BrickWorld): BrickWorld {
        val level = world.level + 1
        return world.copy(
            bricks = buildBricks(level),
            ball = ballOnPaddle(world.paddle),
            level = level,
            status = BrickStatus.READY,
        )
    }

    fun movePaddle(world: BrickWorld, centerX: Float): BrickWorld {
        val clamped = centerX.coerceIn(world.paddle.width / 2f, world.width - world.paddle.width / 2f)
        val paddle = world.paddle.copy(centerX = clamped)
        // While waiting to launch, the ball rides the paddle.
        val ball = if (world.status == BrickStatus.READY) ballOnPaddle(paddle) else world.ball
        return world.copy(paddle = paddle, ball = ball)
    }

    fun launch(world: BrickWorld): BrickWorld {
        if (world.status != BrickStatus.READY) return world
        val speed = BASE_SPEED + SPEED_PER_LEVEL * (world.level - 1)
        // Launch up and slightly to the right.
        val vx = speed * 0.35f
        val vy = -speed
        return world.copy(ball = world.ball.copy(vx = vx, vy = vy), status = BrickStatus.RUNNING)
    }

    /** Places a stationary ball on top of the paddle. */
    fun ballOnPaddle(paddle: Paddle): Ball =
        Ball(x = paddle.centerX, y = paddle.y - BALL_RADIUS - 0.2f, vx = 0f, vy = 0f, radius = BALL_RADIUS)

    /** After losing a life, re-arm the ball on the paddle and wait for a relaunch. */
    fun rearm(world: BrickWorld): BrickWorld =
        if (world.status == BrickStatus.LIFE_LOST) {
            world.copy(ball = ballOnPaddle(world.paddle), status = BrickStatus.READY)
        } else {
            world
        }

    /**
     * Advances the simulation by [dt] seconds. Pure: same inputs → same output.
     * Handles wall/paddle/brick collisions, scoring, life loss and level clear.
     */
    fun step(world: BrickWorld, dt: Float): BrickWorld {
        if (world.status != BrickStatus.RUNNING) return world

        var ball = world.ball
        var bricks = world.bricks
        var score = world.score

        // Integrate position.
        var nx = ball.x + ball.vx * dt
        var ny = ball.y + ball.vy * dt
        var vx = ball.vx
        var vy = ball.vy

        // Side walls.
        if (nx - ball.radius < 0f) { nx = ball.radius; vx = abs(vx) }
        if (nx + ball.radius > world.width) { nx = world.width - ball.radius; vx = -abs(vx) }
        // Top wall.
        if (ny - ball.radius < 0f) { ny = ball.radius; vy = abs(vy) }

        ball = ball.copy(x = nx, y = ny, vx = vx, vy = vy)

        // Paddle collision (only when descending).
        if (vy > 0f && intersectsPaddle(ball, world.paddle)) {
            val hit = ((ball.x - world.paddle.centerX) / (world.paddle.width / 2f)).coerceIn(-1f, 1f)
            val speed = hypot(ball.vx, ball.vy)
            val angle = hit * MAX_BOUNCE_ANGLE
            vx = speed * kotlin.math.sin(angle)
            vy = -speed * kotlin.math.cos(angle)
            ball = ball.copy(y = world.paddle.y - ball.radius - 0.01f, vx = vx, vy = vy)
        }

        // Brick collision — resolve at most one per step.
        val hitIndex = bricks.indexOfFirst { it.alive && intersectsBrick(ball, it) }
        if (hitIndex >= 0) {
            val brick = bricks[hitIndex]
            bricks = bricks.toMutableList().also { it[hitIndex] = brick.copy(alive = false) }
            score += brick.points
            val (rvx, rvy) = reflectOffBrick(ball, brick)
            ball = ball.copy(vx = rvx, vy = rvy)
        }

        // Bottom — life lost.
        if (ball.y - ball.radius > world.height) {
            val lives = world.lives - 1
            return world.copy(
                ball = ball,
                bricks = bricks,
                score = score,
                lives = lives,
                status = if (lives <= 0) BrickStatus.GAME_OVER else BrickStatus.LIFE_LOST,
            )
        }

        val status = if (bricks.none { it.alive }) BrickStatus.LEVEL_CLEARED else BrickStatus.RUNNING
        return world.copy(ball = ball, bricks = bricks, score = score, status = status)
    }

    // ----------------------------- collision helpers -----------------------------

    private fun intersectsPaddle(ball: Ball, paddle: Paddle): Boolean =
        ball.y + ball.radius >= paddle.y &&
            ball.y - ball.radius <= paddle.y + paddle.height &&
            ball.x >= paddle.left - ball.radius &&
            ball.x <= paddle.right + ball.radius

    private fun intersectsBrick(ball: Ball, brick: Brick): Boolean {
        val closestX = ball.x.coerceIn(brick.x, brick.x + brick.width)
        val closestY = ball.y.coerceIn(brick.y, brick.y + brick.height)
        val dx = ball.x - closestX
        val dy = ball.y - closestY
        return dx * dx + dy * dy <= ball.radius * ball.radius
    }

    /** Reflects the ball off a brick along the axis of least penetration. */
    private fun reflectOffBrick(ball: Ball, brick: Brick): Pair<Float, Float> {
        val brickCenterX = brick.x + brick.width / 2f
        val brickCenterY = brick.y + brick.height / 2f
        val overlapX = (ball.radius + brick.width / 2f) - abs(ball.x - brickCenterX)
        val overlapY = (ball.radius + brick.height / 2f) - abs(ball.y - brickCenterY)
        return if (overlapX < overlapY) {
            // Hit a vertical side.
            val dir = sign(ball.x - brickCenterX).let { if (it == 0f) 1f else it }
            abs(ball.vx) * dir to ball.vy
        } else {
            val dir = sign(ball.y - brickCenterY).let { if (it == 0f) 1f else it }
            ball.vx to abs(ball.vy) * dir
        }
    }

    // ------------------------------- level layout --------------------------------

    private fun buildBricks(level: Int): List<Brick> {
        val rows = (3 + level).coerceAtMost(7)
        val cols = 8
        val marginX = 4f
        val top = 12f
        val gap = 1.2f
        val brickW = (WORLD_WIDTH - marginX * 2f - gap * (cols - 1)) / cols
        val brickH = 4.2f
        val bricks = ArrayList<Brick>(rows * cols)
        var id = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = marginX + c * (brickW + gap)
                val y = top + r * (brickH + gap)
                // Higher rows are worth more.
                val points = (rows - r) * 10
                bricks += Brick(id++, x, y, brickW, brickH, points)
            }
        }
        return bricks
    }
}
