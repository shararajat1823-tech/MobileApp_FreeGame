package com.miniplay.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.theme.Motion

/**
 * The celebratory end-of-game overlay shared by all games. Scales/fades in over a
 * scrim and offers Play Again / Back to hub, plus an optional stats slot each
 * game fills with its own numbers.
 */
@Composable
fun GameResultOverlay(
    visible: Boolean,
    emoji: String,
    title: String,
    primaryValue: String,
    primaryLabel: String,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    isNewBest: Boolean = false,
    secondary: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Duration.fast)),
        exit = fadeOut(tween(Motion.Duration.fast)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(Motion.bouncySpring(), initialScale = 0.8f) + fadeIn(),
                exit = scaleOut(tween(Motion.Duration.fast)) + fadeOut(),
            ) {
                MiniPlayCard(
                    modifier = Modifier
                        .padding(28.dp)
                        .widthIn(max = 360.dp),
                    contentPadding = 24.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(emoji, style = MaterialTheme.typography.displayLarge)
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (isNewBest) {
                            InfoChip(
                                text = stringResource(R.string.game_new_best),
                                leading = "⭐",
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Text(
                            primaryValue,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            primaryLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        secondary?.invoke()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PrimaryButton(
                                text = stringResource(R.string.game_play_again),
                                onClick = onPlayAgain,
                                leadingIcon = Icons.Rounded.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            GhostButton(
                                text = stringResource(R.string.game_back_to_hub),
                                onClick = onExit,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
