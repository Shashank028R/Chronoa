package com.studycompanion.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    LIGHT,
    DARK,
    AMOLED,
    SYSTEM
}

object StudyTheme {
    val colors: StudyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyColors.current

    val typography: StudyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyTypography.current

    val shapes: StudyShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyShapes.current
}

@Composable
fun StudyCompanionTheme(
    themeMode: AppThemeMode = AppThemeMode.AMOLED,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val resolvedColors = when (themeMode) {
        AppThemeMode.AMOLED -> AmoledStudyColors
        AppThemeMode.DARK -> DarkStudyColors
        AppThemeMode.LIGHT -> LightStudyColors
        AppThemeMode.SYSTEM -> if (isSystemDark) DarkStudyColors else LightStudyColors
    }

    val materialColorScheme = if (resolvedColors.isAmoled || resolvedColors == DarkStudyColors) {
        darkColorScheme(
            primary = resolvedColors.accent,
            onPrimary = resolvedColors.background,
            background = resolvedColors.background,
            onBackground = resolvedColors.textPrimary,
            surface = resolvedColors.surface,
            onSurface = resolvedColors.textPrimary,
            surfaceVariant = resolvedColors.surfaceVariant,
            onSurfaceVariant = resolvedColors.textSecondary,
            outline = resolvedColors.border
        )
    } else {
        lightColorScheme(
            primary = resolvedColors.accent,
            onPrimary = resolvedColors.surface,
            background = resolvedColors.background,
            onBackground = resolvedColors.textPrimary,
            surface = resolvedColors.surface,
            onSurface = resolvedColors.textPrimary,
            surfaceVariant = resolvedColors.surfaceVariant,
            onSurfaceVariant = resolvedColors.textSecondary,
            outline = resolvedColors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = resolvedColors.background.toArgb()
                window.navigationBarColor = resolvedColors.background.toArgb()
                val isLight = !resolvedColors.isAmoled && resolvedColors == LightStudyColors
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalStudyColors provides resolvedColors,
        LocalStudyShapes provides StudyShapes(),
        LocalStudyTypography provides StudyTypography()
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            shapes = MaterialStudyShapes,
            typography = MaterialStudyTypography,
            content = content
        )
    }
}
