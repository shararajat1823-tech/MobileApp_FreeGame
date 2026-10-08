package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

/** Local player identity, backed by DataStore. No authentication. */
interface ProfileRepository {
    fun observeProfile(): Flow<PlayerProfile>
    suspend fun updateNickname(nickname: String)
    suspend fun updateAvatar(avatarId: Int)
    suspend fun reset()
}
