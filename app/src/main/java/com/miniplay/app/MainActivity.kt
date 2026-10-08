package com.miniplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.AppContainer
import com.miniplay.app.di.LocalAppContainer
import com.miniplay.app.domain.model.AppSettings
import com.miniplay.app.domain.model.ThemeMode
import com.miniplay.app.navigation.MiniPlayApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MiniPlayApplication).container

        setContent {
            MiniPlayRoot(container)
        }
    }
}

@Composable
private fun MiniPlayRoot(container: AppContainer) {
    val settings by container.settingsRepository
        .observeSettings()
        .collectAsStateWithLifecycle(initialValue = AppSettings.DEFAULT)

    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(LocalAppContainer provides container) {
        MiniPlayTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                MiniPlayApp()
            }
        }
    }
}
