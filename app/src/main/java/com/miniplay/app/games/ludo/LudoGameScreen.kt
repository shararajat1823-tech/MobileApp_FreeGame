package com.miniplay.app.games.ludo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.modifier.bounceClick
import kotlinx.coroutines.flow.SharedFlow

private val GOLD = Color(0xFFFFC53D)
private val CIRCLE_BTN = Color(0xFF3C4EA8)

@Composable
fun LudoGameScreen(
    state: LudoUiState,
    moveAnims: SharedFlow<LudoMoveAnim>,
    onRoll: () -> Unit,
    onTokenTap: (LudoColor, Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
    onDismissTutorial: () -> Unit,
) {
    val game = state.game ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LudoColors.bgTop, LudoColors.bgBottom))),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(onClick = onMenu) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.game_back), tint = Color.White)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👑", style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.ludo_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = GOLD,
                    )
                }
                CircleButton(onClick = onPause, enabled = state.winner == null) {
                    Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.game_pause), tint = Color.White)
                }
            }

            // Top players: Red (left), Green (right).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayerPanel(state, LudoColor.RED, Modifier.weight(1f))
                PlayerPanel(state, LudoColor.GREEN, Modifier.weight(1f))
            }

            // Board inside a raised frame.
            Box(Modifier.fillMaxWidth().weight(1f).padding(10.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2A3B86))
                        .padding(7.dp),
                ) {
                    LudoBoardView(
                        state = state,
                        moveAnims = moveAnims,
                        onTokenTap = onTokenTap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Bottom players: Blue (left), Yellow (right).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayerPanel(state, LudoColor.BLUE, Modifier.weight(1f))
                PlayerPanel(state, LudoColor.YELLOW, Modifier.weight(1f))
            }

            // Bottom dice bar.
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    turnPrompt(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
                DiceButton(
                    face = state.diceFace,
                    rolling = state.diceRolling,
                    enabled = state.canRoll,
                    color = LudoColors.of(game.currentColor),
                    onRoll = onRoll,
                )
                Text(
                    stringResource(R.string.ludo_roll_dice),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.canRoll) Color.White else Color.White.copy(alpha = 0.5f),
                )
            }
        }

        TutorialOverlay(visible = state.showTutorial && state.winner == null, onDismiss = onDismissTutorial)
        PauseOverlay(visible = state.paused && state.winner == null, onResume = onResume, onRestart = onPlayAgain, onMenu = onMenu)
        VictoryOverlay(state = state, onPlayAgain = onPlayAgain, onMenu = onMenu)
    }
}

@Composable
private fun CircleButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CIRCLE_BTN)
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun PlayerPanel(state: LudoUiState, color: LudoColor, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val active = color in game.players
    val isCurrent = active && color == game.currentColor && state.winner == null
    val seat = state.seats.firstOrNull { it.color == color }
    val isBot = seat?.isBot ?: false
    val home = if (active) game.tokensHome(color) else 0
    val c = LudoColors.of(color)

    Row(
        modifier
            .alpha(if (active) 1f else 0.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(c, LudoColors.dark(color))))
            .then(if (isCurrent) Modifier.border(3.dp, GOLD, RoundedCornerShape(14.dp)) else Modifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
                    .border(2.dp, LudoColors.dark(color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("👤", style = MaterialTheme.typography.titleMedium)
            }
            if (isCurrent) {
                Text("👑", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.TopStart).offset(x = (-6).dp, y = (-10).dp))
            }
        }
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(colorNameRes(color)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🏠 $home/4", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.95f))
                if (active) {
                    Text(
                        if (isBot) "CPU" else "You",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun turnPrompt(state: LudoUiState): String {
    val game = state.game ?: return ""
    val name = stringResource(colorNameRes(game.currentColor))
    return when {
        state.winner != null -> ""
        state.animating -> stringResource(R.string.ludo_moving)
        game.phase == LudoPhase.AWAIT_ROLL && state.isCurrentHuman -> stringResource(R.string.ludo_your_turn, name)
        game.phase == LudoPhase.AWAIT_ROLL -> stringResource(R.string.ludo_rolling)
        game.phase == LudoPhase.AWAIT_MOVE && state.isCurrentHuman -> stringResource(R.string.ludo_tap_piece)
        game.phase == LudoPhase.AWAIT_MOVE -> stringResource(R.string.ludo_thinking)
        else -> ""
    }
}

private fun colorNameRes(color: LudoColor): Int = when (color) {
    LudoColor.RED -> R.string.ludo_color_red
    LudoColor.GREEN -> R.string.ludo_color_green
    LudoColor.YELLOW -> R.string.ludo_color_yellow
    LudoColor.BLUE -> R.string.ludo_color_blue
}

@Composable
private fun DiceButton(face: Int, rolling: Boolean, enabled: Boolean, color: Color, onRoll: () -> Unit) {
    val scale by animateFloatAsState(if (enabled && !rolling) 1.07f else 1f, label = "dieScale")
    val rotation by animateFloatAsState(if (rolling) 22f else 0f, label = "dieRot")
    Box(
        Modifier
            .size(78.dp)
            .scale(scale)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFE23B3B), Color(0xFF9B1C2B))))
            .border(3.dp, GOLD, CircleShape)
            .alpha(if (enabled) 1f else 0.55f)
            .then(if (enabled) Modifier.bounceClick(onClick = onRoll) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(46.dp).padding(8.dp)) {
                val pip = size.minDimension * 0.15f
                val third = size.width / 3f
                fun dot(dx: Float, dy: Float) = drawCircle(Color(0xFF222222), pip, Offset(dx, dy))
                val lo = third * 0.5f
                val mid = third * 1.5f
                val hi = third * 2.5f
                when (face) {
                    1 -> dot(mid, mid)
                    2 -> { dot(lo, lo); dot(hi, hi) }
                    3 -> { dot(lo, lo); dot(mid, mid); dot(hi, hi) }
                    4 -> { dot(lo, lo); dot(hi, lo); dot(lo, hi); dot(hi, hi) }
                    5 -> { dot(lo, lo); dot(hi, lo); dot(mid, mid); dot(lo, hi); dot(hi, hi) }
                    else -> { dot(lo, lo); dot(hi, lo); dot(lo, mid); dot(hi, mid); dot(lo, hi); dot(hi, hi) }
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            MiniPlayCard(modifier = Modifier.padding(20.dp).widthIn(max = 420.dp), contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎲 " + stringResource(R.string.ludo_tutorial_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.ludo_tutorial_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(stringResource(R.string.ludo_got_it), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(visible: Boolean, onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            MiniPlayCard(modifier = Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏸️", style = MaterialTheme.typography.displayMedium)
                    Text(stringResource(R.string.ludo_paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    PrimaryButton(stringResource(R.string.ludo_resume), onClick = onResume, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.ludo_restart), onClick = onRestart, modifier = Modifier.fillMaxWidth())
                    GhostButton(stringResource(R.string.ludo_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun VictoryOverlay(state: LudoUiState, onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    val winner = state.winner
    AnimatedVisibility(visible = winner != null, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            AnimatedVisibility(true, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.8f) + fadeIn()) {
                MiniPlayCard(modifier = Modifier.padding(28.dp).widthIn(max = 360.dp), contentPadding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🏆", style = MaterialTheme.typography.displayLarge)
                        if (winner != null) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(LudoColors.of(winner)))
                            Text(
                                stringResource(R.string.ludo_winner, stringResource(colorNameRes(winner))),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.size(4.dp))
                        PrimaryButton(stringResource(R.string.ludo_play_again), onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
                        GhostButton(stringResource(R.string.ludo_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
