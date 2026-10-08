package com.miniplay.app.data.repository

import com.miniplay.app.data.local.dao.AchievementStateDao
import com.miniplay.app.data.local.entity.AchievementStateEntity
import com.miniplay.app.domain.model.Achievement
import com.miniplay.app.domain.model.AchievementState
import com.miniplay.app.domain.usecase.AchievementCatalog
import com.miniplay.app.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class AchievementRepositoryImpl(
    private val dao: AchievementStateDao,
) : AchievementRepository {

    private val _newlyUnlocked = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val newlyUnlocked: Flow<String> = _newlyUnlocked

    override fun observeAchievements(): Flow<List<Achievement>> =
        dao.observeAll().map { entities -> merge(entities.map { it.toDomain() }) }

    override suspend fun getStates(): List<AchievementState> =
        dao.getAll().map { it.toDomain() }

    override suspend fun applyStates(states: List<AchievementState>): List<String> {
        val previouslyUnlocked = dao.getAll()
            .filter { it.unlocked }
            .map { it.id }
            .toSet()

        dao.upsertAll(states.map { AchievementStateEntity.from(it) })

        val newlyUnlockedIds = states
            .filter { it.unlocked && it.id !in previouslyUnlocked }
            .map { it.id }

        newlyUnlockedIds.forEach { _newlyUnlocked.tryEmit(it) }
        return newlyUnlockedIds
    }

    override suspend fun resetAll() = dao.clear()

    /** Combine the static catalogue with stored state; unseen achievements read as locked. */
    private fun merge(states: List<AchievementState>): List<Achievement> {
        val byId = states.associateBy { it.id }
        return AchievementCatalog.all.map { def ->
            val state = byId[def.id] ?: AchievementState(def.id, unlocked = false, progress = 0)
            Achievement(definition = def, state = state)
        }
    }
}
