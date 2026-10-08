package com.miniplay.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A single spacing scale (4dp base grid). Padding and gaps reference these
 * tokens instead of literal dp values so rhythm stays consistent app-wide.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    /** Standard screen side gutter. */
    val screen: Dp = 20.dp,
    /** Minimum interactive target per Material accessibility guidance. */
    val minTouchTarget: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
