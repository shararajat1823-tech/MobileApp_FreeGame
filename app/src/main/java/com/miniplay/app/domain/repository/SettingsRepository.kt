package com.miniplay.app.domain.repository

import com.miniplay.app.domain.model.AppSettings
import com.miniplay.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** User preferences, backed by DataStore. */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}
