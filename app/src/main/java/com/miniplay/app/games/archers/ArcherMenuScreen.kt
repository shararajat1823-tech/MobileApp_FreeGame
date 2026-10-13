package com.miniplay.app.games.archers

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.GhostButton
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.PrimaryButton
import com.miniplay.app.core.ui.components.SectionHeader
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.domain.model.GameDifficulty

@Composable
fun ArcherMenuScreen(
    state: ArcherUiState,
    onExit: () -> Unit,
    onPlay: (ArcherMode) -> Unit,
    onSetDifficulty: (GameDifficulty) -> Unit,
    onSetScreenShake: (Boolean) -> Unit,
    onSetReducedEffects: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
) {
    var showHowTo by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    GameScaffold(
        title = stringResource(R.string.archers_title),
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

            SectionHeader(stringResource(R.string.archers_mode_play))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.archers_difficulty), style = MaterialTheme.typography.labelLarge)
                    Segmented(
                        options = listOf(
                            stringResource(R.string.difficulty_easy) to GameDifficulty.EASY,
                            stringResource(R.string.difficulty_medium) to GameDifficulty.MEDIUM,
                            stringResource(R.string.difficulty_hard) to GameDifficulty.HARD,
                        ),
                        selected = state.settings.difficulty,
                        onSelect = onSetDifficulty,
                    )
                    PrimaryButton(stringResource(R.string.archers_quick_match), onClick = { onPlay(ArcherMode.VS_AI) }, modifier = Modifier.fillMaxWidth())
                }
            }

            MiniPlayCard(Modifier.fillMaxWidth(), onClick = { onPlay(ArcherMode.PRACTICE) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.archers_practice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.archers_practice_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.archers_settings_title))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleRow(stringResource(R.string.archers_screen_shake), state.settings.screenShake, onSetScreenShake)
                    ToggleRow(stringResource(R.string.archers_reduced_effects), state.settings.reducedEffects, onSetReducedEffects)
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.archers_stats_title))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat(state.stats.played.toString(), stringResource(R.string.archers_stat_played))
                    Stat(state.stats.wins.toString(), stringResource(R.string.archers_stat_wins))
                    Stat("${state.stats.winPercent}%", stringResource(R.string.archers_stat_winrate))
                    Stat(state.stats.bestStreak.toString(), stringResource(R.string.archers_stat_streak))
                }
            }

            GhostButton(stringResource(R.string.archers_reset), onClick = { showReset = true }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🏹",
            title = stringResource(R.string.archers_title),
            body = stringResource(R.string.archers_how_to),
            onDismiss = { showHowTo = false },
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            confirmButton = {
                TextButton(onClick = { onResetProgress(); showReset = false }) {
                    Text(stringResource(R.string.archers_reset_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.game_cancel)) } },
            title = { Text(stringResource(R.string.archers_reset)) },
            text = { Text(stringResource(R.string.archers_reset_body)) },
        )
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFE5484D))))
            .padding(20.dp),
    ) {
        Column {
            Text("🏹 " + stringResource(R.string.archers_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(stringResource(R.string.archers_tagline), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val isSel = value == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50))
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
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
