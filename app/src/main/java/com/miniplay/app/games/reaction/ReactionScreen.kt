package com.miniplay.app.games.reaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.core.ui.theme.Palette
import com.miniplay.app.di.rememberViewModel

@Composable
fun ReactionScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        ReactionViewModel(container.recordGameResult, container.soundManager, container.hapticManager)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.reaction_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MiniPlayTheme.spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TapArea(
                phase = state.phase,
                lastMs = state.stats.lastMs,
                rating = state.lastRating,
                onTap = viewModel::onTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .weight(1f)
                    .padding(top = 16.dp),
            )
            StatsRow(
                stats = state.stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(bottom = 20.dp),
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "⚡",
            title = stringResource(R.string.reaction_title),
            body = stringResource(R.string.reaction_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun TapArea(
    phase: ReactionPhase,
    lastMs: Int?,
    rating: ReactionRating?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetBackground = when (phase) {
        ReactionPhase.IDLE, ReactionPhase.RESULT -> MaterialTheme.colorScheme.primaryContainer
        ReactionPhase.WAITING, ReactionPhase.TOO_SOON -> MaterialTheme.colorScheme.error
        ReactionPhase.GO -> MiniPlayTheme.colors.success
    }
    val onBackground = when (phase) {
        ReactionPhase.IDLE, ReactionPhase.RESULT -> MaterialTheme.colorScheme.onPrimaryContainer
        ReactionPhase.WAITING, ReactionPhase.TOO_SOON -> MaterialTheme.colorScheme.onError
        ReactionPhase.GO -> Palette.White
    }
    val background by animateColorAsState(targetBackground, tween(Motion.Duration.fast), label = "reactionBg")

    // A subtle pop when the screen turns GO so the cue is unmistakable.
    val goScale by animateFloatAsState(
        targetValue = if (phase == ReactionPhase.GO) 1f else 0.9f,
        animationSpec = Motion.tactileSpring(),
        label = "goScale",
    )

    // No ripple and no bounce — the tap must register with the lowest latency.
    val interactionSource = remember { MutableInteractionSource() }

    MiniPlayCard(
        modifier = modifier,
        color = background,
        contentPadding = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTap,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (phase) {
                ReactionPhase.IDLE -> BigPrompt(
                    primary = stringResource(R.string.reaction_idle),
                    color = onBackground,
                )
                ReactionPhase.WAITING -> BigPrompt(
                    primary = stringResource(R.string.reaction_wait),
                    color = onBackground,
                )
                ReactionPhase.GO -> BigPrompt(
                    primary = stringResource(R.string.reaction_go),
                    color = onBackground,
                    modifier = Modifier.graphicsLayer { scaleX = goScale; scaleY = goScale },
                )
                ReactionPhase.TOO_SOON -> BigPrompt(
                    primary = stringResource(R.string.reaction_too_soon),
                    secondary = stringResource(R.string.reaction_tap_to_retry),
                    color = onBackground,
                )
                ReactionPhase.RESULT -> BigPrompt(
                    primary = lastMs?.let { stringResource(R.string.reaction_ms, it) } ?: "",
                    secondary = rating?.let { stringResource(it.labelRes()) },
                    tertiary = stringResource(R.string.reaction_tap_to_retry),
                    color = onBackground,
                )
            }
        }
    }
}

@Composable
private fun BigPrompt(
    primary: String,
    color: Color,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    tertiary: String? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = color,
            textAlign = TextAlign.Center,
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
        if (tertiary != null) {
            Text(
                text = tertiary,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatsRow(stats: ReactionStats, modifier: Modifier = Modifier) {
    MiniPlayCard(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            InlineStat(stats.lastMs.toMsLabel(), stringResource(R.string.reaction_last))
            InlineStat(stats.averageMs.toMsLabel(), stringResource(R.string.reaction_average))
            InlineStat(stats.bestMs.toMsLabel(), stringResource(R.string.game_best))
            InlineStat(stats.attempts.toString(), stringResource(R.string.reaction_attempts))
        }
    }
}

@Composable
private fun Int?.toMsLabel(): String =
    if (this == null) "—" else stringResource(R.string.reaction_ms, this)

private fun ReactionRating.labelRes(): Int = when (this) {
    ReactionRating.LIGHTNING -> R.string.reaction_rating_lightning
    ReactionRating.EXCELLENT -> R.string.reaction_rating_excellent
    ReactionRating.GOOD -> R.string.reaction_rating_good
    ReactionRating.PRACTICE -> R.string.reaction_rating_practice
}
