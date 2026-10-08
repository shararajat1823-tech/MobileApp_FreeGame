package com.miniplay.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.BuildConfig
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.ConfirmDialog
import com.miniplay.app.core.ui.components.InfoChip
import com.miniplay.app.core.ui.components.LabeledRow
import com.miniplay.app.core.ui.components.LoadingState
import com.miniplay.app.core.ui.components.MiniPlayCard
import com.miniplay.app.core.ui.components.SectionHeader
import com.miniplay.app.core.ui.components.StatTile
import com.miniplay.app.core.ui.formatPlayTime
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.ThemeMode

@Composable
fun ProfileScreen() {
    val viewModel = rememberViewModel { container ->
        ProfileViewModel(
            profileRepository = container.profileRepository,
            settingsRepository = container.settingsRepository,
            statsRepository = container.gameStatsRepository,
            streakRepository = container.streakRepository,
            achievementRepository = container.achievementRepository,
            registry = container.gameRegistry,
            clock = container.clock,
            resetAllProgress = container::resetAllProgress,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingState(); return
    }

    var showEdit by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    val gutter = MiniPlayTheme.spacing.screen

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = gutter)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.statusBarsPadding())
        ProfileHeader(
            avatarEmoji = Avatars.emojiFor(state.profile.avatarId),
            nickname = state.profile.nickname,
            achievementsUnlocked = state.summary.achievementsUnlocked,
            achievementsTotal = state.summary.achievementsTotal,
            onEdit = { showEdit = true },
        )

        SectionHeader(stringResource(R.string.profile_section_stats))
        StatsGrid(state)

        if (state.perGame.isNotEmpty()) {
            SectionHeader(stringResource(R.string.profile_section_per_game))
            MiniPlayCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.perGame.forEach { card ->
                        LabeledRow(
                            label = card.metadata.iconEmoji + "  " + stringResource(card.metadata.titleRes),
                            value = card.bestScore?.toString() ?: stringResource(R.string.profile_none),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.profile_section_settings))
        SettingsSection(
            state = state,
            onToggleSound = viewModel::setSound,
            onToggleMusic = viewModel::setMusic,
            onToggleHaptics = viewModel::setHaptics,
            onThemeMode = viewModel::setThemeMode,
            onToggleDynamic = viewModel::setDynamicColor,
            onResetClick = { showReset = true },
        )
    }

    if (showEdit) {
        EditProfileDialog(
            initialName = state.profile.nickname,
            initialAvatar = state.profile.avatarId,
            onSave = { name, avatar ->
                viewModel.updateNickname(name)
                viewModel.updateAvatar(avatar)
                showEdit = false
            },
            onDismiss = { showEdit = false },
        )
    }
    if (showReset) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_confirm_title),
            body = stringResource(R.string.settings_reset_confirm_body),
            confirmLabel = stringResource(R.string.settings_reset_confirm_action),
            dismissLabel = stringResource(R.string.profile_cancel),
            destructive = true,
            onConfirm = { viewModel.resetProgress(); showReset = false },
            onDismiss = { showReset = false },
        )
    }
}

@Composable
private fun ProfileHeader(
    avatarEmoji: String,
    nickname: String,
    achievementsUnlocked: Int,
    achievementsTotal: Int,
    onEdit: () -> Unit,
) {
    MiniPlayCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(avatarEmoji, style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    nickname,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                InfoChip(
                    text = stringResource(R.string.achievements_unlocked_count, achievementsUnlocked, achievementsTotal),
                    leading = "🏆",
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.profile_edit))
            }
        }
    }
}

@Composable
private fun StatsGrid(state: ProfileUiState) {
    val summary = state.summary
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = summary.gamesPlayed.toString(),
                label = stringResource(R.string.profile_stat_games_played),
                icon = Icons.Rounded.SportsEsports,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = summary.totalWins.toString(),
                label = stringResource(R.string.profile_stat_total_wins),
                icon = Icons.Rounded.WorkspacePremium,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = summary.bestScore.toString(),
                label = stringResource(R.string.profile_stat_best_score),
                icon = Icons.Rounded.Star,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = formatPlayTime(summary.totalPlayTimeMillis),
                label = stringResource(R.string.profile_stat_play_time),
                icon = Icons.Rounded.Schedule,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = summary.currentStreakDays.toString(),
                label = stringResource(R.string.profile_stat_streak),
                icon = Icons.Rounded.LocalFireDepartment,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = state.favouriteGame?.iconEmoji ?: stringResource(R.string.profile_none),
                label = stringResource(R.string.profile_stat_favourite),
                icon = Icons.Rounded.EmojiEvents,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    state: ProfileUiState,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onToggleDynamic: (Boolean) -> Unit,
    onResetClick: () -> Unit,
) {
    val s = state.settings
    MiniPlayCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SwitchRow(stringResource(R.string.settings_sound), s.soundEnabled, onToggleSound)
            SwitchRow(stringResource(R.string.settings_music), s.musicEnabled, onToggleMusic)
            SwitchRow(stringResource(R.string.settings_haptics), s.hapticsEnabled, onToggleHaptics)

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ThemeModeSelector(selected = s.themeMode, onSelect = onThemeMode)
            SwitchRow(stringResource(R.string.settings_dynamic_color), s.dynamicColor, onToggleDynamic)

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_reset_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onResetClick) {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        contentDescription = stringResource(R.string.settings_reset_progress),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.LIGHT to R.string.settings_theme_light,
        ThemeMode.DARK to R.string.settings_theme_dark,
        ThemeMode.SYSTEM to R.string.settings_theme_system,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (mode, res) ->
            val isSelected = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(res),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
