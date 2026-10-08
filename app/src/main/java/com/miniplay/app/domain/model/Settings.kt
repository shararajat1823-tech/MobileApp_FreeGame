package com.miniplay.app.domain.model

/** User-selectable appearance mode. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * All user preferences, persisted via DataStore. Offline, device-local.
 */
data class AppSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
) {
    companion object {
        val DEFAULT = AppSettings()
    }
}
