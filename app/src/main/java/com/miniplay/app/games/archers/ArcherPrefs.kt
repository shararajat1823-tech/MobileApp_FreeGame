package com.miniplay.app.games.archers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miniplay.app.domain.model.GameDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local settings for Ragdoll Archers. */
data class ArcherSettings(
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val screenShake: Boolean = true,
    val reducedEffects: Boolean = false,
    val tutorialSeen: Boolean = false,
)

/** Local lifetime stats. */
data class ArcherStats(
    val played: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val winPercent: Int get() = if (played == 0) 0 else (wins * 100) / played
}

/**
 * Archers-local persistence on the shared DataStore (only `archer_*` keys — no
 * other game's data is touched). A stored schema [VERSION] lets future releases
 * migrate safely; unknown/missing values fall back to defaults so a corrupt or
 * old save never crashes. Everything works offline; nothing is transmitted.
 */
class ArcherPrefs(private val dataStore: DataStore<Preferences>) {

    fun observeSettings(): Flow<ArcherSettings> = dataStore.data.map { p ->
        ArcherSettings(
            difficulty = p[DIFFICULTY]?.let { runCatching { GameDifficulty.valueOf(it) }.getOrNull() }
                ?: GameDifficulty.MEDIUM,
            screenShake = p[SCREEN_SHAKE] ?: true,
            reducedEffects = p[REDUCED_EFFECTS] ?: false,
            tutorialSeen = p[TUTORIAL_SEEN] ?: false,
        )
    }

    fun observeStats(): Flow<ArcherStats> = dataStore.data.map { p ->
        ArcherStats(
            played = p[PLAYED] ?: 0,
            wins = p[WINS] ?: 0,
            losses = p[LOSSES] ?: 0,
            currentStreak = p[STREAK] ?: 0,
            bestStreak = p[BEST_STREAK] ?: 0,
        )
    }

    suspend fun setDifficulty(value: GameDifficulty) { dataStore.edit { it[DIFFICULTY] = value.name } }
    suspend fun setScreenShake(value: Boolean) { dataStore.edit { it[SCREEN_SHAKE] = value } }
    suspend fun setReducedEffects(value: Boolean) { dataStore.edit { it[REDUCED_EFFECTS] = value } }
    suspend fun setTutorialSeen() { dataStore.edit { it[TUTORIAL_SEEN] = true } }

    /** Records a finished competitive match (practice is not recorded). */
    suspend fun recordMatch(won: Boolean) {
        dataStore.edit { p ->
            p[VERSION] = SCHEMA
            p[PLAYED] = (p[PLAYED] ?: 0) + 1
            if (won) {
                p[WINS] = (p[WINS] ?: 0) + 1
                val streak = (p[STREAK] ?: 0) + 1
                p[STREAK] = streak
                p[BEST_STREAK] = maxOf(p[BEST_STREAK] ?: 0, streak)
            } else {
                p[LOSSES] = (p[LOSSES] ?: 0) + 1
                p[STREAK] = 0
            }
        }
    }

    /** Clears stats only, keeping settings. Backs a confirmed "reset progress". */
    suspend fun resetProgress() {
        dataStore.edit { p ->
            p.remove(PLAYED); p.remove(WINS); p.remove(LOSSES); p.remove(STREAK); p.remove(BEST_STREAK)
        }
    }

    private companion object {
        const val SCHEMA = 1
        val VERSION = intPreferencesKey("archer_schema")
        val DIFFICULTY = stringPreferencesKey("archer_difficulty")
        val SCREEN_SHAKE = booleanPreferencesKey("archer_screen_shake")
        val REDUCED_EFFECTS = booleanPreferencesKey("archer_reduced_effects")
        val TUTORIAL_SEEN = booleanPreferencesKey("archer_tutorial_seen")
        val PLAYED = intPreferencesKey("archer_played")
        val WINS = intPreferencesKey("archer_wins")
        val LOSSES = intPreferencesKey("archer_losses")
        val STREAK = intPreferencesKey("archer_streak")
        val BEST_STREAK = intPreferencesKey("archer_best_streak")
    }
}
