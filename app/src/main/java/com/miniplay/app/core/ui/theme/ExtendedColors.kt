package com.miniplay.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Colours Material 3's [androidx.compose.material3.ColorScheme] does not model:
 * success/warning semantics and the two "surface tiers" used to build the
 * layered, card-on-card look of the dashboard. Exposed through
 * [LocalMiniPlayColors] so composables read them the same way they read the
 * Material scheme.
 */
@Immutable
data class MiniPlayColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** Slightly raised surface used for cards sitting on [surfaceSunken]. */
    val surfaceRaised: Color,
    /** Recessed surface used behind grouped content (e.g. the home background). */
    val surfaceSunken: Color,
    /** Hairline separators and the outline of ghost buttons. */
    val hairline: Color,
    val isDark: Boolean,
)

val LightMiniPlayColors = MiniPlayColors(
    success = Palette.Success,
    successContainer = Palette.SuccessContainer,
    onSuccessContainer = Palette.OnSuccessContainer,
    warning = Palette.Warning,
    warningContainer = Palette.WarningContainer,
    onWarningContainer = Palette.OnWarningContainer,
    surfaceRaised = Palette.White,
    surfaceSunken = Palette.Neutral95,
    hairline = Color(0x14000000),
    isDark = false,
)

val DarkMiniPlayColors = MiniPlayColors(
    success = Palette.Success,
    successContainer = Color(0xFF0E3B24),
    onSuccessContainer = Palette.SuccessContainer,
    warning = Palette.Warning,
    warningContainer = Color(0xFF4A3400),
    onWarningContainer = Palette.WarningContainer,
    surfaceRaised = Palette.Neutral24,
    surfaceSunken = Palette.Neutral10,
    hairline = Color(0x1FFFFFFF),
    isDark = true,
)

val LocalMiniPlayColors = staticCompositionLocalOf { LightMiniPlayColors }

/**
 * A pair of colours that gives each game its own identity while staying inside
 * the global palette. Used for game cards and the game-screen hero header.
 */
@Immutable
data class GameAccent(
    val start: Color,
    val end: Color,
    val onAccent: Color = Palette.White,
) {
    /** A 135° linear gradient from [start] to [end], the app's standard hero fill. */
    fun brush(): Brush = Brush.linearGradient(listOf(start, end))

    val container: Color get() = start

    companion object {
        /** Deterministic accent for any game, keyed by its id. */
        fun forId(id: String): GameAccent = Accents.byId[id] ?: Accents.fallback
    }
}

/**
 * The curated accent set. Adding a new game picks an existing accent (or adds a
 * new entry here) — colours never get hard-coded inside a game screen.
 */
object Accents {
    val Violet = GameAccent(Color(0xFF7C5CFF), Color(0xFF5B2FE0))
    val Ocean = GameAccent(Color(0xFF3BA7FF), Color(0xFF1E63E9))
    val Sunset = GameAccent(Color(0xFFFF8A5B), Color(0xFFF2567C))
    val Mint = GameAccent(Color(0xFF34E0B0), Color(0xFF12857B))
    val Amber = GameAccent(Color(0xFFFFC24B), Color(0xFFF2832B), Palette.Neutral10)
    val Grape = GameAccent(Color(0xFFC77DFF), Color(0xFF8A3FFC))
    val Rose = GameAccent(Color(0xFFFF77A9), Color(0xFFE23B7A))

    val fallback = Violet

    /** Keyed to the game ids declared in each game's descriptor. */
    val byId: Map<String, GameAccent> = mapOf(
        "tictactoe" to Ocean,
        "memorymatch" to Grape,
        "reaction" to Amber,
        "numberpuzzle" to Mint,
        "game2048" to Sunset,
        "brickbreaker" to Rose,
        "tapchallenge" to Violet,
    )
}
