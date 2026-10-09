package com.miniplay.app.games.ludo

import androidx.compose.ui.graphics.Color

/** Accessible, distinct colours for the four players, plus board-neutral tones. */
object LudoColors {
    val red = Color(0xFFE5484D)
    val green = Color(0xFF30A46C)
    val yellow = Color(0xFFF5B800)
    val blue = Color(0xFF3B82F6)

    val boardLine = Color(0xFF9AA0A6)
    val cellLight = Color(0xFFFFFFFF)

    fun of(color: LudoColor): Color = when (color) {
        LudoColor.RED -> red
        LudoColor.GREEN -> green
        LudoColor.YELLOW -> yellow
        LudoColor.BLUE -> blue
    }

    /** A soft tint of the player colour for yards and home columns. */
    fun tint(color: LudoColor): Color = of(color).copy(alpha = 0.22f)

    fun dark(color: LudoColor): Color = when (color) {
        LudoColor.RED -> Color(0xFF9B1C2B)
        LudoColor.GREEN -> Color(0xFF15663F)
        LudoColor.YELLOW -> Color(0xFF8A6A00)
        LudoColor.BLUE -> Color(0xFF1C4FA8)
    }
}
