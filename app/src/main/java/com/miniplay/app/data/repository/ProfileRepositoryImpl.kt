package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.core.common.Clock
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.domain.model.PlayerProfile
import com.miniplay.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    private val defaultNickname: String,
) : ProfileRepository {

    override fun observeProfile(): Flow<PlayerProfile> = dataStore.data.map { prefs ->
        PlayerProfile(
            nickname = prefs[PrefKeys.NICKNAME]?.takeIf { it.isNotBlank() } ?: defaultNickname,
            avatarId = prefs[PrefKeys.AVATAR] ?: PlayerProfile.DEFAULT_AVATAR_ID,
            createdAt = prefs[PrefKeys.PROFILE_CREATED_AT] ?: clock.nowMillis(),
        )
    }

    override suspend fun updateNickname(nickname: String) {
        val trimmed = nickname.trim().take(MAX_NICKNAME_LENGTH)
        dataStore.edit { prefs ->
            prefs[PrefKeys.NICKNAME] = trimmed
            prefs.putIfAbsentCreatedAt()
        }
    }

    override suspend fun updateAvatar(avatarId: Int) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.AVATAR] = avatarId
            prefs.putIfAbsentCreatedAt()
        }
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(PrefKeys.NICKNAME)
            prefs.remove(PrefKeys.AVATAR)
            prefs.remove(PrefKeys.PROFILE_CREATED_AT)
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.putIfAbsentCreatedAt() {
        if (this[PrefKeys.PROFILE_CREATED_AT] == null) {
            this[PrefKeys.PROFILE_CREATED_AT] = clock.nowMillis()
        }
    }

    companion object {
        const val MAX_NICKNAME_LENGTH = 20
    }
}
