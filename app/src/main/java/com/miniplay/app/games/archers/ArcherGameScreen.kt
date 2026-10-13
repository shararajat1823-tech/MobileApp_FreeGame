package com.miniplay.app.games.archers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.theme.MiniPlayTheme

private val PLAYER_C = Color(0xFF3B82F6)
private val OPP_C = Color(0xFFE5484D)

@Composable
fun ArcherGameScreen(
    state: ArcherUiState,
    match: ArcherMatch,
    onFrame: (Long) -> Unit,
    onAim: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPlayAgain: () -> Unit,
    onChangeDifficulty: () -> Unit,
    onMenu: () -> Unit,
    onDismissTutorial: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.game_back))
                }
                Text(
                    stringResource(R.string.archers_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPause, enabled = state.winner == null) {
                    Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.game_pause))
                }
            }

            // Health + turn HUD.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HealthBar(
                    label = stringResource(R.string.archers_you),
                    fraction = state.healthP0,
                    color = PLAYER_C,
                    active = state.isPlayerTurn && state.winner == null,
                    modifier = Modifier.weight(1f),
                )
                HealthBar(
                    label = if (state.mode == ArcherMode.PRACTICE) stringResource(R.string.archers_target) else stringResource(R.string.archers_cpu),
                    fraction = state.healthP1,
                    color = OPP_C,
                    active = !state.isPlayerTurn && state.winner == null,
                    alignEnd = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f).padding(6.dp)) {
                ArcherBoardView(
                    state = state,
                    match = match,
                    onFrame = onFrame,
                    onAim = onAim,
                    onRelease = onRelease,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                )
                // Power meter while aiming.
                if (state.aiming) {
                    PowerMeter(state.aimPower, Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                }
            }

            // Prompt.
            Text(
                text = prompt(state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        TutorialOverlay(visible = state.showTutorial && state.winner == null, onDismiss = onDismissTutorial)
        PauseOverlay(visible = state.paused && state.winner == null, onResume = onResume, onRestart = onPlayAgain, onMenu = onMenu)
        ResultOverlay(state = state, onPlayAgain = onPlayAgain, onChangeDifficulty = onChangeDifficulty, onMenu = onMenu)
    }
}

@Composable
private fun prompt(state: ArcherUiState): String = when {
    state.winner != null -> ""
    state.aiThinking -> stringResource(R.string.archers_opponent_aiming)
    state.phase == ArcherPhase.FIRING || state.phase == ArcherPhase.RESOLVING -> stringResource(R.string.archers_arrow_flying)
    state.mode == ArcherMode.PRACTICE -> stringResource(R.string.archers_practice_hint)
    state.isPlayerTurn -> stringResource(R.string.archers_aim_hint)
    else -> stringResource(R.string.archers_opponent_turn)
}

@Composable
private fun HealthBar(
    label: String,
    fraction: Float,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (active && !alignEnd) Text("▶", color = color, style = MaterialTheme.typography.labelMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (active && alignEnd) Text("◀", color = color, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(50)).background(color),
            )
        }
    }
}

@Composable
private fun PowerMeter(power: Float, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(14.dp).height(120.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(power.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            power > 0.75f -> Color(0xFFE5484D)
                            power > 0.4f -> Color(0xFFF5B800)
                            else -> Color(0xFF30A46C)
                        },
                    ),
            )
        }
        Text("⚡", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TutorialOverlay(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            MiniPlayCard(Modifier.padding(20.dp).widthIn(max = 420.dp), contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏹 " + stringResource(R.string.archers_tutorial_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.archers_tutorial_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(stringResource(R.string.archers_got_it), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(visible: Boolean, onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            MiniPlayCard(Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏸️", style = MaterialTheme.typography.displayMedium)
                    Text(stringResource(R.string.archers_paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    PrimaryButton(stringResource(R.string.archers_resume), onClick = onResume, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.archers_restart), onClick = onRestart, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.archers_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ResultOverlay(state: ArcherUiState, onPlayAgain: () -> Unit, onChangeDifficulty: () -> Unit, onMenu: () -> Unit) {
    val winner = state.winner
    val playerWon = winner == 0
    AnimatedVisibility(winner != null, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            AnimatedVisibility(true, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.8f) + fadeIn()) {
                MiniPlayCard(Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (playerWon) "🏆" else "💀", style = MaterialTheme.typography.displayLarge)
                        Text(
                            stringResource(if (playerWon) R.string.archers_victory else R.string.archers_defeat),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (playerWon) PLAYER_C else OPP_C,
                        )
                        Text(
                            stringResource(R.string.archers_streak, state.stats.currentStreak, state.stats.bestStreak),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(4.dp))
                        PrimaryButton(stringResource(R.string.archers_play_again), onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
                        GhostButton(stringResource(R.string.archers_change_difficulty), onClick = onChangeDifficulty, modifier = Modifier.fillMaxWidth())
                        GhostButton(stringResource(R.string.archers_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
