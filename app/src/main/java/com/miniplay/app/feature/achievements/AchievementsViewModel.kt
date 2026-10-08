package com.miniplay.app.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.domain.model.Achievement
import com.miniplay.app.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val unlockedCount: Int = 0,
    val total: Int = 0,
) {
    val fraction: Float get() = if (total == 0) 0f else unlockedCount.toFloat() / total
}

class AchievementsViewModel(
    achievementRepository: AchievementRepository,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = achievementRepository.observeAchievements()
        .map { list ->
            // Unlocked first, then by progress, so the most "earned" show at the top.
            val sorted = list.sortedWith(
                compareByDescending<Achievement> { it.unlocked }
                    .thenByDescending { it.fractionComplete },
            )
            AchievementsUiState(
                achievements = sorted,
                unlockedCount = list.count { it.unlocked },
                total = list.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState(),
        )
}
