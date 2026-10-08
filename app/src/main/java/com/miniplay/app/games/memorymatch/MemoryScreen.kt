package com.miniplay.app.games.memorymatch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.DifficultySelector
import com.miniplay.app.core.ui.components.GameMenuItem
import com.miniplay.app.core.ui.components.GameResultOverlay
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InlineStat
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.formatDuration
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import kotlinx.coroutines.delay

@Composable
fun MemoryScreen(onExit: () -> Unit) {
    val viewModel = rememberViewModel { container ->
        MemoryViewModel(
            container.recordGameResult,
            container.soundManager,
            container.hapticManager,
            container.gameStatsRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHowTo by remember { mutableStateOf(false) }

    // The engine leaves a mismatched pair face up; flip it back after a short look.
    LaunchedEffect(state.engineState.pendingMismatch) {
        if (state.engineState.pendingMismatch != null) {
            delay(MISMATCH_PEEK_MILLIS)
            viewModel.resolveMismatch()
        }
    }

    GameScaffold(
        title = stringResource(R.string.memory_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
        menuItems = listOf(
            GameMenuItem(stringResource(R.string.game_new_game), Icons.Rounded.Refresh, viewModel::newGame),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MiniPlayTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DifficultySelector(
                    options = listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD),
                    selected = state.difficulty,
                    onSelected = viewModel::setDifficulty,
                    modifier = Modifier.widthIn(max = 480.dp),
                )

                StatsRow(state, modifier = Modifier.widthIn(max = 480.dp))

                CardGrid(
                    state = state,
                    onCardClick = viewModel::onCardClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                )

                Spacer(Modifier.height(20.dp))
            }

            GameResultOverlay(
                visible = state.showResult,
                emoji = "🧠",
                title = stringResource(R.string.result_complete),
                primaryValue = (state.finalScore ?: 0).toString(),
                primaryLabel = stringResource(R.string.result_score_label),
                onPlayAgain = viewModel::newGame,
                onExit = onExit,
                isNewBest = state.bestScore != null &&
                    state.finalScore != null &&
                    state.finalScore!!.toLong() >= state.bestScore!!,
                secondary = {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        InlineStat(state.engineState.moves.toString(), stringResource(R.string.game_moves))
                        InlineStat(formatDuration(state.elapsedMillis), stringResource(R.string.game_time))
                    }
                },
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🧠",
            title = stringResource(R.string.memory_title),
            body = stringResource(R.string.memory_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun StatsRow(state: MemoryUiState, modifier: Modifier = Modifier) {
    MiniPlayCard(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            InlineStat(state.engineState.moves.toString(), stringResource(R.string.game_moves))
            InlineStat(formatDuration(state.elapsedMillis), stringResource(R.string.game_time))
            InlineStat(
                "${state.engineState.matchedPairs}/${state.engineState.totalPairs}",
                stringResource(R.string.memory_pairs),
            )
        }
    }
}

@Composable
private fun CardGrid(
    state: MemoryUiState,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cols = MemoryViewModel.columns(state.difficulty)
    val cards = state.engineState.cards
    val rowCount = (cards.size + cols - 1) / cols
    val accent = GameAccent.forId(GameIds.MEMORY_MATCH)
    val pending = state.engineState.pendingMismatch != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in 0 until rowCount) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until cols) {
                    val index = row * cols + col
                    if (index < cards.size) {
                        val card = cards[index]
                        MemoryCardView(
                            card = card,
                            index = index,
                            accent = accent,
                            enabled = !card.faceUp && !card.matched && !pending && !state.showResult,
                            onClick = { onCardClick(index) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: Card,
    index: Int,
    accent: GameAccent,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val faceShown = card.faceUp || card.matched
    val rotation by animateFloatAsState(
        targetValue = if (faceShown) 180f else 0f,
        animationSpec = tween(Motion.Duration.medium),
        label = "cardFlip",
    )
    val density = LocalDensity.current.density

    val stateDesc = when {
        card.matched -> stringResource(R.string.memory_card_matched, card.symbol)
        card.faceUp -> card.symbol
        else -> stringResource(R.string.memory_card_hidden)
    }
    val cardDesc = stringResource(R.string.memory_card_description, index + 1, stateDesc)

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12 * density
            }
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = cardDesc },
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            CardBack(accent)
        } else {
            // Counter-rotate so the face reads correctly past the half-turn.
            CardFace(card, Modifier.graphicsLayer { rotationY = 180f })
        }
    }
}

@Composable
private fun CardBack(accent: GameAccent, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(accent.brush()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = accent.onAccent,
        )
    }
}

@Composable
private fun CardFace(card: Card, modifier: Modifier = Modifier) {
    val background = if (card.matched) {
        MiniPlayTheme.colors.successContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(card.symbol, style = MaterialTheme.typography.headlineMedium)
    }
}

private const val MISMATCH_PEEK_MILLIS = 700L
