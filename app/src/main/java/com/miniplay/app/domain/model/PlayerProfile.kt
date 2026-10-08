package com.miniplay.app.domain.model

/**
 * The local player identity. No login — stored on-device via DataStore. [avatarId]
 * indexes a built-in emoji avatar set (see `feature.profile.Avatars`).
 */
data class PlayerProfile(
    val nickname: String,
    val avatarId: Int,
    val createdAt: Long,
) {
    companion object {
        const val DEFAULT_AVATAR_ID = 0
    }
}
