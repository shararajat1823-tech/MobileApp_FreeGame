package com.miniplay.app.games.snake

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SnakeGameScreen(
    state: SnakeUiState,
    eatEvents: SharedFlow<SnakeEatEvent>,
    onTurn: (SnakeDir) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onDismissTutorial: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud(state = state, onBack = onMenu, onPause = onPause)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                contentAlignment = Alignment.Center,
            ) {
                SnakeBoard(
                    state = state,
                    eatEvents = eatEvents,
                    onTurn = onTurn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .aspectRatio(1f),
                )
            }

            if (state.settings.controlStyle == ControlStyle.DPAD) {
                DPad(onTurn = onTurn, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                Spacer(Modifier.size(12.dp))
            }
        }

        CountdownOverlay(state)
        TutorialOverlay(visible = state.showTutorial && state.phase != SnakePhase.GAME_OVER, onDismiss = onDismissTutorial)
        PauseOverlay(visible = state.phase == SnakePhase.PAUSED, onResume = onResume, onRetry = onRetry, onMenu = onMenu)
        GameOverOverlay(state = state, onRetry = onRetry, onMenu = onMenu)
    }
}

@Composable
private fun Hud(state: SnakeUiState, onBack: () -> Unit, onPause: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.game_back))
        }
        MiniPlayCard(modifier = Modifier.weight(1f), contentPadding = 10.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InlineStat(state.game.score.toString(), stringResource(R.string.game_score))
                InlineStat(state.bestForMode.toString(), stringResource(R.string.game_best))
                state.timeLeftMillis?.let { ms ->
                    InlineStat(stringResource(R.string.snake_time_left, (ms / 1000)), stringResource(R.string.game_time))
                } ?: InlineStat(state.game.length.toString(), stringResource(R.string.snake_length))
            }
        }
        IconButton(onClick = onPause, enabled = state.phase == SnakePhase.RUNNING) {
            Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.game_pause))
        }
    }
}

@Composable
private fun DPad(onTurn: (SnakeDir) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DPadButton(Icons.Rounded.ArrowUpward, R.string.snake_dir_up) { onTurn(SnakeDir.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            DPadButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.snake_dir_left) { onTurn(SnakeDir.LEFT) }
            DPadButton(Icons.AutoMirrored.Rounded.ArrowForward, R.string.snake_dir_right) { onTurn(SnakeDir.RIGHT) }
        }
        DPadButton(Icons.Rounded.ArrowDownward, R.string.snake_dir_down) { onTurn(SnakeDir.DOWN) }
    }
}

@Composable
private fun DPadButton(icon: ImageVector, descRes: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = stringResource(descRes), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CountdownOverlay(state: SnakeUiState) {
    val visible = state.phase == SnakePhase.COUNTDOWN && (state.countdownValue > 0 || state.showGo)
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val label = if (state.showGo) stringResource(R.string.snake_countdown_go) else state.countdownValue.toString()
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 36.dp, vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun TutorialOverlay(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            MiniPlayCard(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 420.dp),
                contentPadding = 18.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("👆 " + stringResource(R.string.snake_tutorial_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.snake_tutorial_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(stringResource(R.string.snake_tutorial_got_it), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(visible: Boolean, onResume: () -> Unit, onRetry: () -> Unit, onMenu: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            MiniPlayCard(modifier = Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏸️", style = MaterialTheme.typography.displayMedium)
                    Text(stringResource(R.string.snake_paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    PrimaryButton(stringResource(R.string.snake_resume), onClick = onResume, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.snake_restart), onClick = onRetry, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.snake_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(state: SnakeUiState, onRetry: () -> Unit, onMenu: () -> Unit) {
    AnimatedVisibility(visible = state.phase == SnakePhase.GAME_OVER, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visible = true, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.8f) + fadeIn()) {
                MiniPlayCard(modifier = Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🐍", style = MaterialTheme.typography.displayLarge)
                        Text(stringResource(R.string.result_you_lose), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (state.isNewBest) {
                            Text("⭐ " + stringResource(R.string.game_new_best), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                        Text(state.game.score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.result_score_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.game_best) + " " + state.bestForMode, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.snake_length) + " " + state.game.length, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.size(4.dp))
                        PrimaryButton(stringResource(R.string.game_play_again), onClick = onRetry, modifier = Modifier.fillMaxWidth())
                        GhostButton(stringResource(R.string.snake_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
