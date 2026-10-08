package com.miniplay.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Palette.Violet40,
    onPrimary = Palette.White,
    primaryContainer = Palette.Violet90,
    onPrimaryContainer = Palette.Violet20,
    secondary = Palette.Teal40,
    onSecondary = Palette.White,
    secondaryContainer = Palette.Teal90,
    onSecondaryContainer = Palette.Teal30,
    tertiary = Palette.Coral40,
    onTertiary = Palette.White,
    tertiaryContainer = Palette.Coral90,
    onTertiaryContainer = Palette.Coral30,
    error = Palette.Error40,
    onError = Palette.White,
    background = Palette.Neutral98,
    onBackground = Palette.Neutral10,
    surface = Palette.Neutral99,
    onSurface = Palette.Neutral10,
    surfaceVariant = Palette.NeutralVariant90,
    onSurfaceVariant = Palette.NeutralVariant30,
    outline = Palette.NeutralVariant50,
    outlineVariant = Palette.NeutralVariant80,
    surfaceContainerLowest = Palette.White,
    surfaceContainerLow = Palette.Neutral98,
    surfaceContainer = Palette.Neutral95,
    surfaceContainerHigh = Palette.Neutral93,
    surfaceContainerHighest = Palette.Neutral90,
    inverseSurface = Palette.Neutral20,
    inverseOnSurface = Palette.Neutral95,
)

private val DarkColorScheme = darkColorScheme(
    primary = Palette.Violet60,
    onPrimary = Palette.Violet10,
    primaryContainer = Palette.Violet30,
    onPrimaryContainer = Palette.Violet90,
    secondary = Palette.Teal50,
    onSecondary = Palette.Teal30,
    secondaryContainer = Palette.Teal40,
    onSecondaryContainer = Palette.Teal90,
    tertiary = Palette.Coral50,
    onTertiary = Palette.Coral30,
    tertiaryContainer = Palette.Coral40,
    onTertiaryContainer = Palette.Coral90,
    error = Palette.Error80,
    onError = Palette.Neutral10,
    background = Palette.Neutral10,
    onBackground = Palette.Neutral90,
    surface = Palette.Neutral20,
    onSurface = Palette.Neutral90,
    surfaceVariant = Palette.NeutralVariant30,
    onSurfaceVariant = Palette.NeutralVariant80,
    outline = Palette.NeutralVariant60,
    outlineVariant = Palette.NeutralVariant30,
    surfaceContainerLowest = Palette.Neutral10,
    surfaceContainerLow = Palette.Neutral20,
    surfaceContainer = Palette.Neutral22,
    surfaceContainerHigh = Palette.Neutral24,
    surfaceContainerHighest = Palette.Neutral24,
    inverseSurface = Palette.Neutral90,
    inverseOnSurface = Palette.Neutral20,
)

/**
 * Root theme. Wires the Material 3 scheme plus every MiniPlay design-system
 * [CompositionLocalProvider] (extended colours, spacing). Dynamic colour is
 * opt-in and only takes effect on Android 12+.
 */
@Composable
fun MiniPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extended = if (darkTheme) DarkMiniPlayColors else LightMiniPlayColors

    CompositionLocalProvider(
        LocalMiniPlayColors provides extended,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MiniPlayTypography,
            shapes = MiniPlayShapes,
            content = content,
        )
    }
}

/**
 * Ergonomic accessors so call-sites read `MiniPlayTheme.colors.success` and
 * `MiniPlayTheme.spacing.lg` just like `MaterialTheme.colorScheme`.
 */
object MiniPlayTheme {
    val colors: MiniPlayColors
        @Composable @ReadOnlyComposable get() = LocalMiniPlayColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}
