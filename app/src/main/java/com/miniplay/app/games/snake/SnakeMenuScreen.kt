package com.miniplay.app.games.snake

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.GameScaffold
import com.miniplay.app.core.ui.components.HowToPlayDialog
import com.miniplay.app.core.ui.components.InfoChip
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.SectionHeader
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.core.ui.theme.MiniPlayTheme

private data class ModeInfo(@StringRes val title: Int, @StringRes val desc: Int, val emoji: String)

private fun modeInfo(mode: SnakeMode): ModeInfo = when (mode) {
    SnakeMode.CLASSIC -> ModeInfo(R.string.snake_mode_classic, R.string.snake_mode_classic_desc, "🐍")
    SnakeMode.ZEN -> ModeInfo(R.string.snake_mode_zen, R.string.snake_mode_zen_desc, "🌿")
    SnakeMode.TIME_ATTACK -> ModeInfo(R.string.snake_mode_timeattack, R.string.snake_mode_timeattack_desc, "⏱️")
    SnakeMode.OBSTACLE -> ModeInfo(R.string.snake_mode_obstacle, R.string.snake_mode_obstacle_desc, "🧱")
}

@Composable
fun SnakeMenuScreen(
    state: SnakeUiState,
    onExit: () -> Unit,
    onPlay: (SnakeMode) -> Unit,
    onSetSkin: (String) -> Unit,
    onSetSensitivity: (SwipeSensitivity) -> Unit,
    onSetControlStyle: (ControlStyle) -> Unit,
) {
    var showHowTo by remember { mutableStateOf(false) }
    GameScaffold(
        title = stringResource(R.string.snake_title),
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
            SectionHeader(stringResource(R.string.snake_modes_title))
            SnakeMode.entries.forEach { mode ->
                ModeCard(mode = mode, best = state.bests[mode] ?: 0, onPlay = { onPlay(mode) })
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.snake_skins_title))
            SkinRow(selectedId = state.settings.skinId, onSelect = onSetSkin)

            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.snake_controls_title))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.snake_control_style), style = MaterialTheme.typography.titleSmall)
                    Segmented(
                        options = listOf(
                            stringResource(R.string.snake_control_swipe) to ControlStyle.SWIPE,
                            stringResource(R.string.snake_control_dpad) to ControlStyle.DPAD,
                        ),
                        selected = state.settings.controlStyle,
                        onSelect = onSetControlStyle,
                    )
                    Text(stringResource(R.string.snake_sensitivity), style = MaterialTheme.typography.titleSmall)
                    Segmented(
                        options = listOf(
                            stringResource(R.string.snake_sens_low) to SwipeSensitivity.LOW,
                            stringResource(R.string.snake_sens_med) to SwipeSensitivity.MEDIUM,
                            stringResource(R.string.snake_sens_high) to SwipeSensitivity.HIGH,
                        ),
                        selected = state.settings.sensitivity,
                        onSelect = onSetSensitivity,
                    )
                }
            }
        }
    }

    if (showHowTo) {
        HowToPlayDialog(
            titleEmoji = "🐍",
            title = stringResource(R.string.snake_title),
            body = stringResource(R.string.snake_how_to),
            onDismiss = { showHowTo = false },
        )
    }
}

@Composable
private fun ModeCard(mode: SnakeMode, best: Int, onPlay: () -> Unit) {
    val info = modeInfo(mode)
    MiniPlayCard(Modifier.fillMaxWidth(), onClick = onPlay) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(info.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(info.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(info.desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (best > 0) InfoChip(text = "${stringResource(R.string.game_best)} $best", leading = "⭐")
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.snake_play),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SkinRow(selectedId: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SnakeSkins.all.forEach { skin ->
            val selected = skin.id == selectedId
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(skin.bodyHead)
                    .then(
                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier,
                    )
                    .bounceClick { onSelect(skin.id) },
            )
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
