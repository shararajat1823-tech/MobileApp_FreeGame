package com.miniplay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miniplay.app.data.local.datastore.PrefKeys
import com.miniplay.app.domain.model.AppSettings
import com.miniplay.app.domain.model.ThemeMode
import com.miniplay.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            soundEnabled = prefs[PrefKeys.SOUND] ?: AppSettings.DEFAULT.soundEnabled,
            musicEnabled = prefs[PrefKeys.MUSIC] ?: AppSettings.DEFAULT.musicEnabled,
            hapticsEnabled = prefs[PrefKeys.HAPTICS] ?: AppSettings.DEFAULT.hapticsEnabled,
            themeMode = prefs[PrefKeys.THEME_MODE]?.let(::themeModeOf) ?: AppSettings.DEFAULT.themeMode,
            dynamicColor = prefs[PrefKeys.DYNAMIC_COLOR] ?: AppSettings.DEFAULT.dynamicColor,
        )
    }

    override suspend fun setSoundEnabled(enabled: Boolean) =
        edit { it[PrefKeys.SOUND] = enabled }

    override suspend fun setMusicEnabled(enabled: Boolean) =
        edit { it[PrefKeys.MUSIC] = enabled }

    override suspend fun setHapticsEnabled(enabled: Boolean) =
        edit { it[PrefKeys.HAPTICS] = enabled }

    override suspend fun setThemeMode(mode: ThemeMode) =
        edit { it[PrefKeys.THEME_MODE] = mode.name }

    override suspend fun setDynamicColor(enabled: Boolean) =
        edit { it[PrefKeys.DYNAMIC_COLOR] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun themeModeOf(raw: String): ThemeMode =
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
}
