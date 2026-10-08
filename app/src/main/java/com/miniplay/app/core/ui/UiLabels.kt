package com.miniplay.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.miniplay.app.R
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.domain.model.GameDifficulty

/** Central mapping from domain enums to localised labels, so screens never
 * `when` over enums to build strings ad-hoc. */

@Composable
@ReadOnlyComposable
fun GameCategory.label(): String = stringResource(categoryRes())

@StringRes
fun GameCategory.categoryRes(): Int = when (this) {
    GameCategory.PUZZLE -> R.string.category_puzzle
    GameCategory.ARCADE -> R.string.category_arcade
    GameCategory.STRATEGY -> R.string.category_strategy
    GameCategory.REFLEX -> R.string.category_reflex
    GameCategory.MEMORY -> R.string.category_memory
}

@Composable
@ReadOnlyComposable
fun GameDifficulty.label(): String = stringResource(difficultyRes())

@StringRes
fun GameDifficulty.difficultyRes(): Int = when (this) {
    GameDifficulty.EASY -> R.string.difficulty_easy
    GameDifficulty.MEDIUM -> R.string.difficulty_medium
    GameDifficulty.HARD -> R.string.difficulty_hard
}

/** Human time formatting for durations shown in games and profile. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds) else "${seconds}s"
}

fun formatPlayTime(millis: Long): String {
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
