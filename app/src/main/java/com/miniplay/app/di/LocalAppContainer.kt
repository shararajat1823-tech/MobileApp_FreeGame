package com.miniplay.app.di

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Exposes the [AppContainer] to the Compose tree. Provided once at the app root;
 * screens and game ViewModels read `LocalAppContainer.current` to obtain
 * dependencies instead of threading them through every composable signature.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap content in CompositionLocalProvider(LocalAppContainer provides …).")
}
