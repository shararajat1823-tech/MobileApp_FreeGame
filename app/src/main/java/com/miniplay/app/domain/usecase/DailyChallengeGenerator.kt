package com.miniplay.app.domain.usecase

import com.miniplay.app.domain.model.DailyChallenge
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.ScoreDirection
import kotlin.random.Random

/**
 * Deterministically turns a calendar day into a challenge. "Deterministic" means
 * every device shows the same challenge for the same day with no network, and it
 * is reproducible in tests. A backend could later override this by supplying a
 * [DailyChallenge] directly — the repository contract is unchanged.
 */
object DailyChallengeGenerator {

    /** A candidate challenge shape. Targets are drawn from [minTarget]..[maxTarget]. */
    data class Template(
        val gameId: String,
        val difficulty: GameDifficulty,
        val scoreDirection: ScoreDirection,
        val minTarget: Long,
        val maxTarget: Long,
        val step: Long = 1L,
    )

    /** Built-in rotation. Ordered, but the day's pick is pseudo-random over it. */
    val defaultTemplates: List<Template> = listOf(
        Template(GameIds.REACTION, GameDifficulty.MEDIUM, ScoreDirection.LOWER_IS_BETTER, 280, 360, 5),
        Template(GameIds.GAME_2048, GameDifficulty.MEDIUM, ScoreDirection.HIGHER_IS_BETTER, 1500, 4000, 100),
        Template(GameIds.TAP_CHALLENGE, GameDifficulty.MEDIUM, ScoreDirection.HIGHER_IS_BETTER, 1200, 2600, 50),
        Template(GameIds.MEMORY_MATCH, GameDifficulty.MEDIUM, ScoreDirection.HIGHER_IS_BETTER, 700, 1400, 25),
        Template(GameIds.BRICK_BREAKER, GameDifficulty.MEDIUM, ScoreDirection.HIGHER_IS_BETTER, 400, 1000, 25),
    )

    fun generate(
        epochDay: Long,
        templates: List<Template> = defaultTemplates,
        playerResult: Long? = null,
    ): DailyChallenge {
        require(templates.isNotEmpty()) { "Need at least one challenge template" }
        // A stable seed per day; the magic constant just spreads the bits.
        val seed = epochDay * 0x9E3779B97F4A7C15uL.toLong()
        val rng = Random(seed)

        val template = templates[rng.nextInt(templates.size)]
        val span = ((template.maxTarget - template.minTarget) / template.step).coerceAtLeast(0)
        val target = template.minTarget + rng.nextLong(span + 1) * template.step

        return DailyChallenge(
            epochDay = epochDay,
            gameId = template.gameId,
            difficulty = template.difficulty,
            target = target,
            scoreDirection = template.scoreDirection,
            playerResult = playerResult,
        )
    }
}
