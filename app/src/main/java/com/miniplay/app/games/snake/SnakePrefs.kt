package com.miniplay.app.games.snake

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Swipe distance needed to register a turn, as a sensitivity level. */
enum class SwipeSensitivity(val thresholdDp: Float) {
    LOW(40f), MEDIUM(26f), HIGH(16f)
}

/** SWIPE = gestures only (default). DPAD = adds on-screen arrows for accessibility. */
enum class ControlStyle { SWIPE, DPAD }

data class SnakeSettings(
    val sensitivity: SwipeSensitivity = SwipeSensitivity.MEDIUM,
    val controlStyle: ControlStyle = ControlStyle.SWIPE,
    val skinId: String = SnakeSkins.default.id,
    val tutorialSeen: Boolean = false,
)

/**
 * Snake-local persistence layered onto the app's shared DataStore: control
 * settings, chosen skin, the first-play tutorial flag and per-mode best scores.
 * Keeps the hub's global schema untouched while satisfying Snake's own needs.
 */
class SnakePrefs(private val dataStore: DataStore<Preferences>) {

    fun observeSettings(): Flow<SnakeSettings> = dataStore.data.map { p ->
        SnakeSettings(
            sensitivity = p[SENSITIVITY]?.let { runCatching { SwipeSensitivity.valueOf(it) }.getOrNull() }
                ?: SwipeSensitivity.MEDIUM,
            controlStyle = p[CONTROL_STYLE]?.let { runCatching { ControlStyle.valueOf(it) }.getOrNull() }
                ?: ControlStyle.SWIPE,
            skinId = p[SKIN] ?: SnakeSkins.default.id,
            tutorialSeen = p[TUTORIAL_SEEN] ?: false,
        )
    }

    fun observeBests(): Flow<Map<SnakeMode, Int>> = dataStore.data.map { p ->
        SnakeMode.entries.associateWith { p[bestKey(it)] ?: 0 }
    }

    suspend fun setSensitivity(value: SwipeSensitivity) {
        dataStore.edit { it[SENSITIVITY] = value.name }
    }

    suspend fun setControlStyle(value: ControlStyle) {
        dataStore.edit { it[CONTROL_STYLE] = value.name }
    }

    suspend fun setSkin(id: String) {
        dataStore.edit { it[SKIN] = id }
    }

    suspend fun setTutorialSeen() {
        dataStore.edit { it[TUTORIAL_SEEN] = true }
    }

    /** Writes [score] as the best for [mode] if it beats the stored one. Returns true if it did. */
    suspend fun updateBest(mode: SnakeMode, score: Int): Boolean {
        val current = dataStore.data.first()[bestKey(mode)] ?: 0
        if (score <= current) return false
        dataStore.edit { it[bestKey(mode)] = score }
        return true
    }

    private companion object {
        val SENSITIVITY = stringPreferencesKey("snake_sensitivity")
        val CONTROL_STYLE = stringPreferencesKey("snake_control_style")
        val SKIN = stringPreferencesKey("snake_skin")
        val TUTORIAL_SEEN = booleanPreferencesKey("snake_tutorial_seen")
        fun bestKey(mode: SnakeMode) = intPreferencesKey("snake_best_${mode.name}")
    }
}
