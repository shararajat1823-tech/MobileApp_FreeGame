package com.miniplay.app.games.tapchallenge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameIds
import kotlin.math.min

@Composable
fun TapScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        TapViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.gameStatsRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.tap_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::start),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScoreBar(state)
                PlayArea(
                    state = state,
                    onStart = viewModel::start,
                    onTargetHit = viewModel::onTargetHit,
                    onMissTap = viewModel::onMissTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 20.dp),
                )
            }

            GameResultOverlay(
                visible = state.phase == TapPhase.FINISHED,
                emoji = "🎯",
                title = stringResource(R.string.result_complete),
                primaryValue = state.stats.score.toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                onPlayAgain = viewModel::start,
                onExit = onExit,
                isNewBest = state.stats.score.toLong() >= state.bestScore && state.bestScore > 0,
                secondary = {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        InlineStat(state.stats.hits.toString(), stringResource(R.string.tap_hits))
                        InlineStat(
                            "${state.stats.accuracyPercent}%",
                            stringResource(R.string.tap_accuracy),
                        )
                        InlineStat(state.stats.bestCombo.toString(), stringResource(R.string.tap_combo))
                    }
                },
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🎯",
            title = stringResource(R.string.tap_title),
            body = stringResource(R.string.tap_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun ScoreBar(state: TapUiState) {
    val seconds = ((state.millisLeft + 999) / 1000).toInt()
    MiniPlayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                InlineStat(state.stats.score.toString(), stringResource(R.string.game_score))
                InlineStat("x${TapEngine.multiplier(state.stats.combo)}", stringResource(R.string.tap_combo))
                InlineStat(
                    stringResource(R.string.tap_time_left, seconds),
                    stringResource(R.string.game_time),
                )
            }
            LinearProgressIndicator(
                progress = { state.millisLeft.toFloat() / TapEngine.DURATION_MS },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlayArea(
    state: TapUiState,
    onStart: () -> Unit,
    onTargetHit: () -> Unit,
    onMissTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = GameAccent.forId(GameIds.TAP_CHALLENGE)
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minPx = min(widthPx, heightPx)

        when (state.phase) {
            TapPhase.IDLE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.tap_how_to),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.tap_start),
                        onClick = onStart,
                    )
                }
            }

            TapPhase.RUNNING -> {
                val latestTarget by rememberUpdatedState(state.target)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(widthPx, heightPx) {
                            detectTapGestures { pos ->
                                val target = latestTarget
                                if (target == null) {
                                    onMissTap()
                                    return@detectTapGestures
                                }
                                val cx = target.xFraction * widthPx
                                val cy = target.yFraction * heightPx
                                val r = target.radiusFraction * minPx
                                val dx = pos.x - cx
                                val dy = pos.y - cy
                                if (dx * dx + dy * dy <= r * r) onTargetHit() else onMissTap()
                            }
                        },
                )
                state.target?.let { target ->
                    TargetCircle(
                        target = target,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        minPx = minPx,
                        accent = accent,
                    )
                }
            }

            TapPhase.FINISHED -> Unit
        }
    }
}

/** The current target: a filled accent circle that scales in and gently pulses. Purely visual —
 *  hit-testing is handled by the play area's gesture detector, so this never consumes taps. */
@Composable
private fun TargetCircle(
    target: Target,
    widthPx: Float,
    heightPx: Float,
    minPx: Float,
    accent: GameAccent,
) {
    val density = LocalDensity.current
    val r = target.radiusFraction * minPx
    val cx = target.xFraction * widthPx
    val cy = target.yFraction * heightPx

    val appear = remember(target.id) { Animatable(0f) }
    LaunchedEffect(target.id) { appear.animateTo(1f, Motion.tactileSpring()) }

    val infinite = rememberInfiniteTransition(label = "tapPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "tapPulseScale",
    )

    val diameterDp = with(density) { (2 * r).toDp() }
    val leftDp = with(density) { (cx - r).toDp() }
    val topDp = with(density) { (cy - r).toDp() }
    val targetDesc = stringResource(R.string.tap_target_description)

    Box(
        modifier = Modifier
            .offset(x = leftDp, y = topDp)
            .size(diameterDp)
            .graphicsLayer {
                val scale = appear.value * pulse
                scaleX = scale
                scaleY = scale
                alpha = appear.value
            }
            .clip(CircleShape)
            .background(accent.brush())
            .border(3.dp, accent.onAccent.copy(alpha = 0.6f), CircleShape)
            .semantics { contentDescription = targetDesc },
    )
}
