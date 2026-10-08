package com.miniplay.app.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.game.GameRegistry
import com.miniplay.app.domain.repository.GameStatsRepository
import com.miniplay.app.feature.home.GameCardUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the full catalogue with live best scores. Search/category filtering is
 * applied in the UI (the set is tiny), keeping this layer a simple projection.
 */
class GamesViewModel(
    private val registry: GameRegistry,
    statsRepository: GameStatsRepository,
) : ViewModel() {

    val games: StateFlow<List<GameCardUi>> = statsRepository.observeAllStats()
        .map { stats ->
            val bestById = stats.associate { it.gameId to it.bestScore }
            registry.all.map { meta -> GameCardUi(meta, bestById[meta.id]) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = registry.all.map { GameCardUi(it, null) },
        )
}
