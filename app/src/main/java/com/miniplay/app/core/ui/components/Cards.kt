package com.miniplay.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayCorner
import com.miniplay.app.core.ui.theme.MiniPlayTheme

/**
 * The base surface for all cards: a rounded, subtly tinted container that sits
 * on the sunken app background. Optionally clickable with the app's bounce.
 */
@Composable
fun MiniPlayCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MiniPlayCorner.card),
    color: Color = MiniPlayTheme.colors.surfaceRaised,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier
        .clip(shape)
        .background(color)
        .let { if (onClick != null) it.bounceClick(onClick = onClick) else it }
        .padding(contentPadding)
    Box(base) { content() }
}

/**
 * A card whose background is a game accent gradient — the "hero" look used for
 * feature cards (continue playing, daily challenge, featured game).
 */
@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MiniPlayCorner.card),
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier
        .clip(shape)
        .background(brush)
        .let { if (onClick != null) it.bounceClick(onClick = onClick) else it }
        .padding(contentPadding)
    Box(base) { content() }
}

/** A flat, low-emphasis container (e.g. list rows inside a settings group). */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MiniPlayCorner.card),
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(contentPadding),
    ) { content() }
}
