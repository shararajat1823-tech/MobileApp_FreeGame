package com.miniplay.app.games.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.components.SectionHeader
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayTheme

@Composable
fun LudoMenuScreen(
    state: LudoUiState,
    onExit: () -> Unit,
    onPlay: (LudoMode, Int) -> Unit,
    onResume: () -> Unit,
    onSetDifficulty: (GameDifficulty) -> Unit,
    onSetReducedEffects: (Boolean) -> Unit,
    onSetQuickWin: (Boolean) -> Unit,
    onSetThreeSixes: (Boolean) -> Unit,
) {
    var showHowTo by remember { mutableStateOf(false) }
    var cpuCount by remember { mutableIntStateOf(2) }
    var localCount by remember { mutableIntStateOf(2) }

    GameScaffold(
        title = stringResource(R.string.ludo_title),
        onExit = onExit,
        onHowToPlay = { showHowTo = true },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MiniPlayTheme.spacing.screen)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Hero()

            if (state.hasSavedMatch) {
                MiniPlayCard(Modifier.fillMaxWidth(), onClick = onResume) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏯️", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ludo_resume_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.ludo_resume_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.ludo_modes_title))

            // Play with Computer
            ModeCard(emoji = "🤖", title = stringResource(R.string.ludo_mode_vscpu), desc = stringResource(R.string.ludo_mode_vscpu_desc)) {
                Text(stringResource(R.string.ludo_opponents), style = MaterialTheme.typography.labelLarge)
                Segmented(
                    options = listOf("1" to 2, "2" to 3, "3" to 4),
                    selected = cpuCount,
                    onSelect = { cpuCount = it },
                )
                Text(stringResource(R.string.ludo_difficulty), style = MaterialTheme.typography.labelLarge)
                Segmented(
                    options = listOf(
                        stringResource(R.string.difficulty_easy) to GameDifficulty.EASY,
                        stringResource(R.string.difficulty_medium) to GameDifficulty.MEDIUM,
                        stringResource(R.string.difficulty_hard) to GameDifficulty.HARD,
                    ),
                    selected = state.settings.botDifficulty,
                    onSelect = onSetDifficulty,
                )
                PrimaryButton(stringResource(R.string.ludo_play), onClick = { onPlay(LudoMode.VS_COMPUTER, cpuCount) }, modifier = Modifier.fillMaxWidth())
            }

            // Local Multiplayer
            ModeCard(emoji = "👥", title = stringResource(R.string.ludo_mode_local), desc = stringResource(R.string.ludo_mode_local_desc)) {
                Text(stringResource(R.string.ludo_players), style = MaterialTheme.typography.labelLarge)
                Segmented(
                    options = listOf("2" to 2, "3" to 3, "4" to 4),
                    selected = localCount,
                    onSelect = { localCount = it },
                )
                PrimaryButton(stringResource(R.string.ludo_play), onClick = { onPlay(LudoMode.LOCAL, localCount) }, modifier = Modifier.fillMaxWidth())
            }

            // Practice
            ModeCard(emoji = "🎯", title = stringResource(R.string.ludo_mode_practice), desc = stringResource(R.string.ludo_mode_practice_desc)) {
                PrimaryButton(stringResource(R.string.ludo_play), onClick = { onPlay(LudoMode.PRACTICE, 2) }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.ludo_settings_title))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleRow(stringResource(R.string.ludo_quick_win), stringResource(R.string.ludo_quick_win_desc), state.settings.quickWin, onSetQuickWin)
                    ToggleRow(stringResource(R.string.ludo_three_sixes), stringResource(R.string.ludo_three_sixes_desc), state.settings.threeSixesForfeits, onSetThreeSixes)
                    ToggleRow(stringResource(R.string.ludo_reduced_effects), stringResource(R.string.ludo_reduced_effects_desc), state.settings.reducedEffects, onSetReducedEffects)
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.ludo_stats_title))
            StatsCard(state.stats)

            if (state.history.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.ludo_history_title))
                MiniPlayCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.history.take(6).forEach { HistoryRow(it) }
                    }
                }
            }
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🎲",
            title = stringResource(R.string.ludo_title),
            body = stringResource(R.string.ludo_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(LudoColors.red, LudoColors.green, LudoColors.yellow, LudoColors.blue),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Text("🎲 " + stringResource(R.string.ludo_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(stringResource(R.string.ludo_tagline), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, desc: String, content: @Composable () -> Unit) {
    MiniPlayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val isSel = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .bounceClick(pressedScale = 0.97f) { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatsCard(stats: LudoStats) {
    MiniPlayCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(stats.played.toString(), stringResource(R.string.ludo_stat_played))
            Stat(stats.won.toString(), stringResource(R.string.ludo_stat_won))
            Stat("${stats.winPercent}%", stringResource(R.string.ludo_stat_winrate))
            Stat(stats.captures.toString(), stringResource(R.string.ludo_stat_captures))
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(record: LudoMatchRecord) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val winner = record.winner
        Box(
            Modifier.size(14.dp).clip(CircleShape)
                .background(if (winner != null) LudoColors.of(winner) else MaterialTheme.colorScheme.outline),
        )
        Spacer(Modifier.size(10.dp))
        Text(modeLabel(record.modeLabel), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        val outcome = when (record.humanWon) {
            true -> stringResource(R.string.ludo_outcome_won)
            false -> stringResource(R.string.ludo_outcome_lost)
            null -> ""
        }
        Text(outcome, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun modeLabel(stored: String): String = when (stored) {
    LudoMode.VS_COMPUTER.name -> stringResource(R.string.ludo_mode_vscpu)
    LudoMode.LOCAL.name -> stringResource(R.string.ludo_mode_local)
    LudoMode.PRACTICE.name -> stringResource(R.string.ludo_mode_practice)
    else -> stored
}
