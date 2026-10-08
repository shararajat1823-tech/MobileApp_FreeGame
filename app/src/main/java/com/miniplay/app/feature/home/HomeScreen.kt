package com.miniplay.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.ContinuePlayingCard
import com.miniplay.app.core.ui.components.GameGridCard
import com.miniplay.app.core.ui.components.GameRailCard
import com.miniplay.app.core.ui.components.LoadingState
import com.miniplay.app.core.ui.components.SectionHeader
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenGame: (String) -> Unit,
    onSeeAllGames: () -> Unit,
) {
    val viewModel = rememberViewModel { container ->
        HomeViewModel(
            registry = container.gameRegistry,
            statsRepository = container.gameStatsRepository,
            dailyChallengeRepository = container.dailyChallengeRepository,
            streakRepository = container.streakRepository,
            clock = container.clock,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingState()
        return
    }

    val gutter = MiniPlayTheme.spacing.screen
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(Modifier.padding(horizontal = gutter)) }

        item {
            StreakStrip(
                streakDays = state.streakDays,
                bestStreak = state.bestStreak,
                week = state.week,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }

        val daily = state.daily
        if (daily != null) {
            item {
                Column(Modifier.padding(horizontal = gutter)) {
                    SectionHeader(stringResource(R.string.home_section_daily))
                    Spacer(Modifier.height(8.dp))
                    DailyChallengeCard(
                        challenge = daily,
                        gameMeta = state.dailyGame,
                        onPlay = { onOpenGame(daily.gameId) },
                    )
                }
            }
        }

        state.lastPlayed?.let { last ->
            item {
                Column(Modifier.padding(horizontal = gutter)) {
                    SectionHeader(stringResource(R.string.home_section_continue))
                    Spacer(Modifier.height(8.dp))
                    ContinuePlayingCard(
                        metadata = last.metadata,
                        bestScore = last.bestScore,
                        onResume = { onOpenGame(last.metadata.id) },
                    )
                }
            }
        }

        item {
            Column {
                SectionHeader(
                    stringResource(R.string.home_section_popular),
                    modifier = Modifier.padding(horizontal = gutter),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = gutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.popular, key = { it.metadata.id }) { card ->
                        GameRailCard(
                            metadata = card.metadata,
                            bestScore = card.bestScore,
                            onPlay = { onOpenGame(card.metadata.id) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.home_section_all),
                actionLabel = stringResource(R.string.games_filter_all),
                onActionClick = onSeeAllGames,
                modifier = Modifier.padding(horizontal = gutter),
            )
        }
        items(state.allGames.chunked(2), key = { it.first().metadata.id }) { rowGames ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowGames.forEach { card ->
                    GameGridCard(
                        metadata = card.metadata,
                        bestScore = card.bestScore,
                        onPlay = { onOpenGame(card.metadata.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowGames.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(modifier.statusBarsPadding().padding(top = 8.dp)) {
        Text(
            text = greeting(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_logo_emoji),
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun greeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val res = when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..17 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }
    return stringResource(res)
}
