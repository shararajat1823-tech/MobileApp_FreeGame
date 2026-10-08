package com.miniplay.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.domain.model.GameMetadata

/** Rounded emoji badge, tinted with the game's accent. */
@Composable
private fun EmojiBadge(
    emoji: String,
    accent: GameAccent,
    size: Int = 48,
    onAccentSurface: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(if (onAccentSurface) Color.White.copy(alpha = 0.22f) else accent.container.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
    }
}

private fun bestText(bestScore: Long?): String? = bestScore?.let { "Best $it" }

/** Grid card for the "All games" section — uniform height for a clean grid. */
@Composable
fun GameGridCard(
    metadata: GameMetadata,
    bestScore: Long?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(metadata.id)
    MiniPlayCard(modifier = modifier, onClick = onPlay, contentPadding = 16.dp) {
        Column(Modifier.heightIn(min = 158.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(metadata.iconEmoji, accent)
                Spacer(Modifier.weight(1f))
                DifficultyChip(metadata.difficulties.last())
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(metadata.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(metadata.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                bestText(bestScore)?.let { InfoChip(text = it, leading = "⭐") }
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.home_play).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.container,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = accent.container,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

/** Wide gradient hero card for the horizontal "Popular" rail. */
@Composable
fun GameRailCard(
    metadata: GameMetadata,
    bestScore: Long?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(metadata.id)
    GradientCard(
        brush = accent.brush(),
        modifier = modifier.width(210.dp),
        onClick = onPlay,
        contentPadding = 18.dp,
    ) {
        Column(Modifier.height(170.dp)) {
            EmojiBadge(metadata.iconEmoji, accent, size = 52, onAccentSurface = true)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(metadata.titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent.onAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(metadata.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = accent.onAccent.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bestText(bestScore) ?: stringResource(R.string.home_play),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent.onAccent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent.onAccent)
                }
            }
        }
    }
}

/** The "Continue playing" hero on home. */
@Composable
fun ContinuePlayingCard(
    metadata: GameMetadata,
    bestScore: Long?,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(metadata.id)
    GradientCard(brush = accent.brush(), modifier = modifier.fillMaxWidth(), onClick = onResume) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(metadata.iconEmoji, accent, size = 56, onAccentSurface = true)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_resume).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.onAccent.copy(alpha = 0.8f),
                )
                Text(
                    stringResource(metadata.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                bestText(bestScore)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = accent.onAccent.copy(alpha = 0.85f))
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.home_resume), tint = accent.onAccent)
            }
        }
    }
}
