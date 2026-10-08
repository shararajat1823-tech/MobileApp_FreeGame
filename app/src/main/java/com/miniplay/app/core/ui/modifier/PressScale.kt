package com.miniplay.app.core.ui.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import com.miniplay.app.core.ui.theme.Motion

/**
 * A clickable that gives tactile feedback by scaling down slightly while pressed.
 * Used on every card and primary action for a consistent "springy" feel. Ripple
 * is suppressed in favour of the scale so cards stay clean.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = Motion.tactileSpring(),
        label = "bounceScale",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
