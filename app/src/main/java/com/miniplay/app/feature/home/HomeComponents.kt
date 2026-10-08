package com.miniplay.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GradientCard
import com.miniplay.app.core.ui.components.InfoChip
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.theme.Accents
import com.miniplay.app.core.ui.theme.GameAccent
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.domain.model.DailyChallenge
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameMetadata

@Composable
fun StreakStrip(
    streakDays: Int,
    bestStreak: Int,
    week: List<StreakDay>,
    modifier: Modifier = Modifier,
) {
    MiniPlayCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (streakDays > 0) Accents.Amber.container else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (streakDays > 0) {
                        stringResource(R.string.streak_days, streakDays)
                    } else {
                        stringResource(R.string.streak_start)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (bestStreak > 0) {
                    InfoChip(text = stringResource(R.string.streak_best, bestStreak))
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { day -> DayCell(day) }
            }
        }
    }
}

@Composable
private fun DayCell(day: StreakDay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (day.active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (day.active) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            day.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun DailyChallengeCard(
    challenge: DailyChallenge,
    gameMeta: GameMetadata?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = gameMeta?.let { GameAccent.forId(it.id) } ?: Accents.Sunset
    GradientCard(brush = accent.brush(), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gameMeta?.iconEmoji ?: "🔥", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        gameMeta?.let { stringResource(it.titleRes) } ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent.onAccent,
                    )
                    Text(
                        stringResource(R.string.daily_target, formatTarget(challenge)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent.onAccent.copy(alpha = 0.9f),
                    )
                }
                if (challenge.completed) {
                    Text("🏆", style = MaterialTheme.typography.headlineMedium)
                }
            }

            val resultText = when {
                challenge.completed -> stringResource(R.string.daily_completed)
                challenge.attempted -> stringResource(R.string.daily_your_result, formatResult(challenge))
                else -> stringResource(R.string.daily_not_attempted)
            }
            Text(
                resultText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent.onAccent,
            )

            PrimaryButton(
                text = stringResource(
                    if (challenge.attempted) R.string.daily_retry else R.string.daily_accept,
                ),
                onClick = onPlay,
                containerColor = Color.White.copy(alpha = 0.22f),
                contentColor = accent.onAccent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatTarget(challenge: DailyChallenge): String =
    if (challenge.gameId == GameIds.REACTION) "${challenge.target} ms" else challenge.target.toString()

private fun formatResult(challenge: DailyChallenge): String {
    val result = challenge.playerResult ?: return "-"
    return if (challenge.gameId == GameIds.REACTION) "$result ms" else result.toString()
}
