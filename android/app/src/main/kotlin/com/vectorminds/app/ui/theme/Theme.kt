package com.vectorminds.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary              = CyanPrimary,
    onPrimary            = Color(0xFF0A1024),
    primaryContainer     = Color(0xFF22305A),
    onPrimaryContainer   = CyanLight,

    secondary            = PurpleSecondary,
    onSecondary          = Color(0xFF1A1130),
    secondaryContainer   = Color(0xFF2C1F55),
    onSecondaryContainer = PurpleLight,

    tertiary             = EmeraldTertiary,
    onTertiary           = Color(0xFF052016),
    tertiaryContainer    = Color(0xFF11442F),
    onTertiaryContainer  = EmeraldLight,

    error                = ErrorRed,
    background           = Navy10,
    onBackground         = Color(0xFFE8EBF2),
    surface              = Navy15,
    onSurface            = Color(0xFFE8EBF2),
    surfaceVariant       = Navy25,
    onSurfaceVariant     = Color(0xFF98A0B3),
    outline              = Navy30,
    outlineVariant       = Navy30,
)

private val LightScheme = lightColorScheme(
    primary              = CyanDark,
    onPrimary            = Surface99,
    primaryContainer     = CyanLight,
    onPrimaryContainer   = Color(0xFF0A1024),

    secondary            = PurpleDark,
    onSecondary          = Surface99,
    secondaryContainer   = PurpleLight,
    onSecondaryContainer = Color(0xFF1A1130),

    tertiary             = EmeraldDark,
    onTertiary           = Surface99,

    error                = Color(0xFFC04050),
    background           = Surface99,
    onBackground         = Color(0xFF101422),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF101422),
    surfaceVariant       = Surface95,
    onSurfaceVariant     = Color(0xFF4F576A),
    outline              = Color(0xFFE3E6EF),
    outlineVariant       = Color(0xFFE3E6EF),
)

@Composable
fun VectorMindsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val vmColors    = if (darkTheme) DarkVmColors else LightVmColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalVmColors    provides vmColors,
        LocalVmShape     provides VmShape(),
        LocalVmSpacing   provides VmSpacing(),
        LocalVmElevation provides VmElevation(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = VectorMindsTypography,
            content     = content
        )
    }
}
