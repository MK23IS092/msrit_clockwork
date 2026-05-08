package com.vectorminds.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary              = CyanPrimary,
    onPrimary            = Navy10,
    primaryContainer     = CyanDark,
    onPrimaryContainer   = CyanLight,

    secondary            = PurpleSecondary,
    onSecondary          = Surface99,
    secondaryContainer   = PurpleDark,
    onSecondaryContainer = PurpleLight,

    tertiary             = EmeraldTertiary,
    onTertiary           = Navy10,
    tertiaryContainer    = EmeraldDark,
    onTertiaryContainer  = EmeraldLight,

    error                = ErrorRed,
    background           = Navy10,
    onBackground         = Surface90,
    surface              = Navy15,
    onSurface            = Surface80,
    surfaceVariant       = Navy25,
    onSurfaceVariant     = Surface80,
    outline              = Navy30,
)

private val LightColorScheme = lightColorScheme(
    primary              = CyanDark,
    onPrimary            = Surface99,
    primaryContainer     = CyanLight,
    onPrimaryContainer   = Navy10,

    secondary            = PurpleDark,
    onSecondary          = Surface99,
    secondaryContainer   = PurpleLight,
    onSecondaryContainer = Navy10,

    tertiary             = EmeraldDark,
    onTertiary           = Surface99,

    error                = ErrorRed,
    background           = Surface99,
    onBackground         = Navy15,
    surface              = Surface95,
    onSurface            = Navy20,
    surfaceVariant       = Surface90,
    onSurfaceVariant     = Navy25,
    outline              = Surface80,
)

@Composable
fun VectorMindsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VectorMindsTypography,
        content     = content
    )
}
