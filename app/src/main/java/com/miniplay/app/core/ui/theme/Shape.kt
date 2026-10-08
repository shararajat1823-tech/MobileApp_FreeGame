package com.miniplay.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded, friendly shape scale. Cards use [large]; sheets and heroes use
 * [extraLarge]; chips and small controls use [small].
 */
val MiniPlayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object MiniPlayCorner {
    val card = 24.dp
    val tile = 14.dp
    val chip = 999.dp // fully rounded (pill)
    val sheet = 28.dp
    val button = 18.dp
}
