package com.miniplay.app.games.ludo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import kotlinx.coroutines.flow.SharedFlow

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
                    stringResource(R.string.ludo_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPause, enabled = state.winner == null) {
                    Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.game_pause))
                }
            }

            PlayerStrip(state)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiniPlayTheme.spacing.screen, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                LudoBoardView(
                    state = state,
                    moveAnims = moveAnims,
                    onTokenTap = onTokenTap,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).aspectRatio(1f),
                )
            }

            Spacer(Modifier.weight(1f))
            TurnBar(state = state, onRoll = onRoll)
            Spacer(Modifier.size(12.dp))
        }

        TutorialOverlay(visible = state.showTutorial && state.winner == null, onDismiss = onDismissTutorial)
        PauseOverlay(visible = state.paused && state.winner == null, onResume = onResume, onRestart = onPlayAgain, onMenu = onMenu)
        VictoryOverlay(state = state, onPlayAgain = onPlayAgain, onMenu = onMenu)
    }
}

@Composable
private fun PlayerStrip(state: LudoUiState) {
    val game = state.game ?: return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MiniPlayTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.seats.forEach { seat ->
            val isCurrent = seat.color == game.currentColor && state.winner == null
            PlayerChip(
                seat = seat,
                tokensHome = game.tokensHome(seat.color),
                isCurrent = isCurrent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerChip(seat: Seat, tokensHome: Int, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val color = LudoColors.of(seat.color)
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (isCurrent) Modifier.border(2.dp, color, RoundedCornerShape(12.dp)) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(2.dp))
        Text(
            seatLabel(seat),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text("🏠 $tokensHome/4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun seatLabel(seat: Seat): String {
    val colorName = stringResource(colorNameRes(seat.color))
    return if (seat.isBot) "$colorName 🤖" else colorName
}

private fun colorNameRes(color: LudoColor): Int = when (color) {
    LudoColor.RED -> R.string.ludo_color_red
    LudoColor.GREEN -> R.string.ludo_color_green
    LudoColor.YELLOW -> R.string.ludo_color_yellow
    LudoColor.BLUE -> R.string.ludo_color_blue
}

@Composable
private fun TurnBar(state: LudoUiState, onRoll: () -> Unit) {
    val game = state.game ?: return
    val color = LudoColors.of(game.currentColor)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MiniPlayTheme.spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LudoDie(
            face = state.diceFace,
            rolling = state.diceRolling,
            enabled = state.canRoll,
            color = color,
            onRoll = onRoll,
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(colorNameRes(game.currentColor)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                turnPrompt(state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun turnPrompt(state: LudoUiState): String {
    val game = state.game ?: return ""
    return when {
        state.animating -> stringResource(R.string.ludo_moving)
        game.phase == LudoPhase.AWAIT_ROLL && state.isCurrentHuman -> stringResource(R.string.ludo_tap_to_roll)
        game.phase == LudoPhase.AWAIT_ROLL -> stringResource(R.string.ludo_rolling)
        game.phase == LudoPhase.AWAIT_MOVE && state.isCurrentHuman -> stringResource(R.string.ludo_tap_piece)
        game.phase == LudoPhase.AWAIT_MOVE -> stringResource(R.string.ludo_thinking)
        else -> ""
    }
}

@Composable
private fun LudoDie(face: Int, rolling: Boolean, enabled: Boolean, color: Color, onRoll: () -> Unit) {
    val scale by animateFloatAsState(if (enabled && !rolling) 1.06f else 1f, label = "dieScale")
    val rotation by animateFloatAsState(if (rolling) 18f else 0f, label = "dieRot")
    Box(
        Modifier
            .size(60.dp)
            .scale(scale)
            .rotate(rotation)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(2.dp, if (enabled) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.bounceClick(onClick = onRoll) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val pip = size.minDimension * 0.15f
            val third = size.width / 3f
            fun dot(cx: Float, cy: Float) = drawCircle(Color(0xFF222222), pip, Offset(cx, cy))
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
