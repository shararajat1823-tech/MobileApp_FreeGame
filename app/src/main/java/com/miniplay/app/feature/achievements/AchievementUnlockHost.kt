package com.miniplay.app.feature.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.audio.SoundEffect
import com.miniplay.app.core.haptics.HapticFeedbackType
import com.miniplay.app.core.ui.theme.Accents
import com.miniplay.app.di.LocalAppContainer
import com.miniplay.app.domain.model.AchievementDefinition
import com.miniplay.app.domain.usecase.AchievementCatalog
import kotlinx.coroutines.delay

/**
 * App-wide listener that pops a celebratory banner whenever an achievement
 * unlocks — no matter which screen the player is on. Placed once at the app root.
 */
@Composable
fun AchievementUnlockHost(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val catalogById = remember { AchievementCatalog.all.associateBy { it.id } }
    val queue = remember { mutableStateListOf<AchievementDefinition>() }

    LaunchedEffect(Unit) {
        container.achievementRepository.newlyUnlocked.collect { id ->
            catalogById[id]?.let { queue.add(it) }
        }
    }

    val current = queue.firstOrNull()
    LaunchedEffect(current?.id) {
        if (current != null) {
            container.soundManager.play(SoundEffect.ACHIEVEMENT)
            container.hapticManager.perform(HapticFeedbackType.SUCCESS)
            delay(2800)
            if (queue.isNotEmpty()) queue.removeAt(0)
        }
    }

    Box(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            current?.let { UnlockBanner(it) }
        }
    }
}

@Composable
private fun UnlockBanner(def: AchievementDefinition) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Accents.Amber.container.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(def.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer()
        Column {
            Text(
                stringResource(R.string.achievements_unlocked_toast),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
            )
            Text(
                stringResource(def.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
}
