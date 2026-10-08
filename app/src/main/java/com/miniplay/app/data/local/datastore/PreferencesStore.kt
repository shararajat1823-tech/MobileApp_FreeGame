package com.miniplay.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single preferences DataStore for all scalar state: settings, profile, streak,
 * daily-challenge progress and achievement facts. Keys are centralised so there
 * is no risk of two repositories colliding on a name.
 */
val Context.miniPlayDataStore: DataStore<Preferences> by preferencesDataStore(name = "miniplay_prefs")

object PrefKeys {
    // Settings
    val SOUND = booleanPreferencesKey("settings_sound")
    val MUSIC = booleanPreferencesKey("settings_music")
    val HAPTICS = booleanPreferencesKey("settings_haptics")
    val THEME_MODE = stringPreferencesKey("settings_theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("settings_dynamic_color")

    // Profile
    val NICKNAME = stringPreferencesKey("profile_nickname")
    val AVATAR = intPreferencesKey("profile_avatar")
    val PROFILE_CREATED_AT = longPreferencesKey("profile_created_at")

    // Last played
    val LAST_PLAYED = stringPreferencesKey("last_played_game")

    // Streak
    val STREAK_CURRENT = intPreferencesKey("streak_current")
    val STREAK_BEST = intPreferencesKey("streak_best")
    val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")

    // Daily challenge progress
    val DAILY_DAY = longPreferencesKey("daily_day")
    val DAILY_RESULT = longPreferencesKey("daily_result")

    // Achievement facts
    val FACT_HARD_COMPLETED = stringSetPreferencesKey("fact_hard_completed")
    val FACT_ANY_FLAWLESS = booleanPreferencesKey("fact_any_flawless")
}
