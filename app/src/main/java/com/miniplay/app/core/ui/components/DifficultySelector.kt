package com.miniplay.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miniplay.app.core.ui.label
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.domain.model.GameDifficulty

/**
 * Segmented control for choosing difficulty. Accessible (each segment is a
 * labelled button) and animated on selection.
 */
@Composable
fun DifficultySelector(
    options: List<GameDifficulty>,
    selected: GameDifficulty,
    onSelected: (GameDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                label = "segBg",
            )
            val content by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(container)
                    .bounceClick(pressedScale = 0.97f) { onSelected(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
