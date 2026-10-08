package com.miniplay.app.feature.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.theme.Accents
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.Achievement

@Composable
fun AchievementsScreen() {
    val viewModel = rememberViewModel { container ->
        AchievementsViewModel(container.achievementRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gutter = MiniPlayTheme.spacing.screen

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Header(unlocked = state.unlockedCount, total = state.total, fraction = state.fraction)
        }
        items(state.achievements, key = { it.id }) { achievement ->
            AchievementCard(achievement)
        }
    }
}

@Composable
private fun Header(unlocked: Int, total: Int, fraction: Float) {
    val animated by animateFloatAsState(targetValue = fraction, label = "achProgress")
    Column(
        Modifier
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.achievements_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.achievements_unlocked_count, unlocked, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val unlocked = achievement.unlocked
    MiniPlayCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) Accents.Amber.container.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        .alpha(if (unlocked) 1f else 0.6f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(achievement.definition.emoji, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (unlocked) stringResource(R.string.achievements_unlocked) else stringResource(R.string.achievements_locked),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) MiniPlayTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                stringResource(achievement.definition.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(achievement.definition.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!unlocked && achievement.target > 1) {
                LinearProgressIndicator(
                    progress = { achievement.fractionComplete },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Text(
                    stringResource(R.string.achievements_progress, achievement.progress, achievement.target),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
