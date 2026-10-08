package com.miniplay.app.games.game2048

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.theme.Accents
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.di.rememberViewModel
import kotlin.math.abs
import kotlin.math.max

@Composable
fun Game2048Screen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        Game2048ViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.gameStatsRepository,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.state
    var showHowTo by remember { mutableStateOf(false) }

    // Record an abandoned-but-played game before leaving, then navigate.
    val exit = {
        viewModel.recordIfNeeded()
        onExit()
    }

    GameScaffold(
        title = stringResource(R.string.g2048_title),
        onExit = exit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::newGame),
            GameMenuItem(stringResource(R.string.game_undo), Icons.AutoMirrored.Rounded.Undo, viewModel::undo),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScoreHeader(score = state.score, best = uiState.bestScore)

                Spacer(Modifier.weight(1f))

                Board(
                    state = state,
                    onSwipe = viewModel::onSwipe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .aspectRatio(1f),
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GhostButton(
                        text = stringResource(R.string.game_undo),
                        onClick = viewModel::undo,
                        enabled = uiState.canUndo,
                        leadingIcon = Icons.AutoMirrored.Rounded.Undo,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.game_new_game),
                        onClick = viewModel::newGame,
                        leadingIcon = Icons.Rounded.Refresh,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Win celebration — "Keep going" lives in the secondary slot.
            GameResultOverlay(
                visible = uiState.showWin,
                emoji = "🎉",
                title = stringResource(R.string.g2048_win_title),
                primaryValue = state.score.toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                onPlayAgain = viewModel::newGame,
                onExit = exit,
                secondary = {
                    GhostButton(
                        text = stringResource(R.string.g2048_keep_going),
                        onClick = viewModel::continueAfterWin,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )

            // Game over — only when we are not already celebrating a win.
            GameResultOverlay(
                visible = state.over && !uiState.showWin,
                emoji = "💥",
                title = stringResource(R.string.result_you_lose),
                primaryValue = state.score.toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                isNewBest = state.score.toLong() >= uiState.bestScore && uiState.bestScore > 0,
                onPlayAgain = viewModel::newGame,
                onExit = exit,
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🔢",
            title = stringResource(R.string.g2048_title),
            body = stringResource(R.string.g2048_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun ScoreHeader(score: Int, best: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniPlayCard(modifier = Modifier.weight(1f)) {
            InlineStat(
                value = score.toString(),
                label = stringResource(R.string.game_score),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MiniPlayCard(modifier = Modifier.weight(1f)) {
            InlineStat(
                value = best.toString(),
                label = stringResource(R.string.game_best),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Board(
    state: Game2048State,
    onSwipe: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardPadding = 10.dp
    val gap = 10.dp
    val size = state.size
    val emptyCell = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val boardDesc = stringResource(R.string.g2048_board_description, state.score)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = boardDesc }
            .pointerInput(Unit) {
                val thresholdPx = 40.dp.toPx()
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                    },
                    onDragEnd = {
                        if (max(abs(dx), abs(dy)) >= thresholdPx) {
                            if (abs(dx) > abs(dy)) {
                                onSwipe(if (dx > 0f) Direction.RIGHT else Direction.LEFT)
                            } else {
                                onSwipe(if (dy > 0f) Direction.DOWN else Direction.UP)
                            }
                        }
                    },
                )
            },
    ) {
        // Cell pitch: available width minus outer padding and inter-cell gaps.
        val cell: Dp = (maxWidth - boardPadding * 2 - gap * (size - 1)) / size

        // Static empty slots.
        for (r in 0 until size) {
            for (c in 0 until size) {
                Box(
                    Modifier
                        .size(cell)
                        .offset(
                            x = boardPadding + (cell + gap) * c,
                            y = boardPadding + (cell + gap) * r,
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(emptyCell),
                )
            }
        }

        // Live tiles, tracked by stable id so sliding animates by identity.
        for (tile in state.tiles) {
            key(tile.id) {
                TileCell(tile = tile, cell = cell, boardPadding = boardPadding, gap = gap)
            }
        }
    }
}

@Composable
private fun TileCell(tile: Tile, cell: Dp, boardPadding: Dp, gap: Dp) {
    val targetX = boardPadding + (cell + gap) * tile.col
    val targetY = boardPadding + (cell + gap) * tile.row
    // State read deferred into the layout/draw lambdas below to avoid per-frame recomposition.
    val x = animateDpAsState(targetX, tween(Motion.Duration.medium, easing = Motion.emphasized), label = "tileX")
    val y = animateDpAsState(targetY, tween(Motion.Duration.medium, easing = Motion.emphasized), label = "tileY")

    // Pop freshly spawned/merged tiles once, keyed to their (fresh) id.
    val isNew = tile.spawnedThisMove || tile.mergedThisMove
    val scale = remember(tile.id) { Animatable(if (isNew) 0.6f else 1f) }
    LaunchedEffect(tile.id) {
        if (isNew) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, Motion.tactileSpring())
        } else {
            scale.snapTo(1f)
        }
    }

    val (background, content) = tileColors(tile.value)
    Box(
        modifier = Modifier
            .size(cell)
            .offset { IntOffset(x.value.roundToPx(), y.value.roundToPx()) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.value.toString(),
            color = content,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = tileFontSize(tile.value),
            maxLines = 1,
        )
    }
}

private fun tileFontSize(value: Int) = when {
    value < 100 -> 30.sp
    value < 1000 -> 26.sp
    value < 10000 -> 21.sp
    else -> 17.sp
}

/** Background + content colour per tile value, warming through the app palette. */
private fun tileColors(value: Int): Pair<Color, Color> {
    val dark = Color(0xFF3A3335)
    val light = Color(0xFFFFFFFF)
    return when (value) {
        2 -> Color(0xFFF0E7DD) to dark
        4 -> Color(0xFFF2E0C4) to dark
        8 -> Accents.Amber.start to dark
        16 -> Accents.Sunset.start to light
        32 -> Accents.Amber.end to light
        64 -> Accents.Sunset.end to light
        128 -> Accents.Rose.end to light
        256 -> Accents.Grape.start to light
        512 -> Accents.Grape.end to light
        1024 -> Accents.Violet.end to light
        2048 -> Accents.Violet.start to light
        else -> Color(0xFF3F2F8F) to light
    }
}
