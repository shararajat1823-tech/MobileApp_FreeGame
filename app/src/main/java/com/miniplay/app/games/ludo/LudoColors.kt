package com.miniplay.app.games.ludo

import androidx.compose.ui.graphics.Color

/** Vivid, classic Ludo colours for the four players, plus board-neutral tones. */
object LudoColors {
    val red = Color(0xFFE23B3B)
    val green = Color(0xFF28A745)
    val yellow = Color(0xFFF5B301)
    val blue = Color(0xFF2E6FF0)

    val boardLine = Color(0xFFB8BDC7)
    val cellLight = Color(0xFFFFFFFF)
    /** Light board backdrop behind the cells (shows at the rounded corners). */
    val boardBackdrop = Color(0xFFF3F4F8)

    /** Deep background gradient for the game screen. */
    val bgTop = Color(0xFF26367E)
    val bgBottom = Color(0xFF151E4D)

    fun of(color: LudoColor): Color = when (color) {
        LudoColor.RED -> red
        LudoColor.GREEN -> green
        LudoColor.YELLOW -> yellow
        LudoColor.BLUE -> blue
    }

    /** A soft tint of the player colour for yard pockets and start cells. */
    fun tint(color: LudoColor): Color = of(color).copy(alpha = 0.18f)

    /** A very light wash for the inner yard pocket. */
    fun light(color: LudoColor): Color = when (color) {
        LudoColor.RED -> Color(0xFFFBE6E6)
        LudoColor.GREEN -> Color(0xFFE4F4E8)
        LudoColor.YELLOW -> Color(0xFFFCF1D8)
        LudoColor.BLUE -> Color(0xFFE2ECFD)
    }

    fun dark(color: LudoColor): Color = when (color) {
        LudoColor.RED -> Color(0xFF9B1C2B)
        LudoColor.GREEN -> Color(0xFF156630)
        LudoColor.YELLOW -> Color(0xFFB5810A)
        LudoColor.BLUE -> Color(0xFF17418F)
    }
}
