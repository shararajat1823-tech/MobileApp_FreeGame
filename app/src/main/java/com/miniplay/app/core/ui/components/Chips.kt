package com.miniplay.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.core.ui.label
import com.miniplay.app.domain.model.GameDifficulty

/** Small pill used for metadata (category, best score, difficulty). */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leading: String? = null,
) {
    Row(
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) Text(leading, style = MaterialTheme.typography.labelMedium)
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

/** Difficulty pill with a traffic-light colour (never colour-only — label too). */
@Composable
fun DifficultyChip(difficulty: GameDifficulty, modifier: Modifier = Modifier) {
    val (container, content) = when (difficulty) {
        GameDifficulty.EASY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        GameDifficulty.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        GameDifficulty.HARD -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    InfoChip(text = difficulty.label(), modifier = modifier, container = container, content = content)
}
