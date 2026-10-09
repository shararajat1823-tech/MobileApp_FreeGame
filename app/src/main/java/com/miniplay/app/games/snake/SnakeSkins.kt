package com.miniplay.app.games.snake

import androidx.compose.ui.graphics.Color

/** A selectable colour theme for the snake. Purely cosmetic. */
data class SnakeSkin(
    val id: String,
    val displayName: String,
    val head: Color,
    val bodyHead: Color,
    val bodyTail: Color,
    val outline: Color,
)

object SnakeSkins {
    val all: List<SnakeSkin> = listOf(
        SnakeSkin("emerald", "Emerald", Color(0xFF72D873), Color(0xFF57C85C), Color(0xFF2E9E4F), Color(0xFF1B6B36)),
        SnakeSkin("ocean", "Ocean", Color(0xFF59C2FF), Color(0xFF3BA7FF), Color(0xFF1E63E9), Color(0xFF12397F)),
        SnakeSkin("sunset", "Sunset", Color(0xFFFFB15B), Color(0xFFFF8A5B), Color(0xFFF2567C), Color(0xFF7A1F3A)),
        SnakeSkin("grape", "Grape", Color(0xFFCB9BFF), Color(0xFFB579FF), Color(0xFF8A3FFC), Color(0xFF4B1E8F)),
        SnakeSkin("mint", "Mint", Color(0xFF6BF0D4), Color(0xFF34E0B0), Color(0xFF12857B), Color(0xFF0A4A44)),
    )

    val default: SnakeSkin get() = all.first()

    fun byId(id: String?): SnakeSkin = all.firstOrNull { it.id == id } ?: default
}
