package com.miniplay.app.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Centralised motion tokens. Durations and easing live here so animations feel
 * coherent and can be tuned in one place. Kept deliberately restrained to
 * protect 60fps on low-end devices.
 */
object Motion {
    object Duration {
        const val instant = 90
        const val fast = 150
        const val medium = 250
        const val slow = 400
        const val celebrate = 650
    }

    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val standard: Easing = FastOutSlowInEasing
    val decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Springs tuned for tactile UI (press/pop) and for playful celebration. */
    fun <T> tactileSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> bouncySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )
}
