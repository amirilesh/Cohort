package com.cohort.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Indigo600,
    onPrimary          = Color.White,
    primaryContainer   = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary          = Violet600,
    onSecondary        = Color.White,
    background         = BackgroundLight,
    onBackground       = OnBackground,
    surface            = Color.White,
    onSurface          = OnBackground,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = NeutralGrey,
    outline            = SlateLight,
)

private val DarkColorScheme = darkColorScheme(
    primary              = IrisIndigo,            // #818CF8 soft indigo
    onPrimary            = NavyDeep,
    primaryContainer     = Indigo100,             // #1E2455 dark indigo tile
    onPrimaryContainer   = IrisViolet,            // #A78BFA violet
    secondary            = IrisCyan,              // #67E8F9 teal cyan
    onSecondary          = NavyDeep,
    secondaryContainer   = Color(0xFF0A2F3D),
    onSecondaryContainer = IrisCyan,
    background           = NavyDeep,             // #0E1120 deepest navy
    onBackground         = TextWhite,            // #F1F5F9
    surface              = NavySurface,          // #151929
    onSurface            = TextWhite,
    surfaceVariant       = NavyCard,             // #1C2035
    onSurfaceVariant     = Color(0xFF94A3B8),    // slate-400
    outline              = BorderDark,           // #2D3454
    error                = Color(0xFFF87171),
    errorContainer       = Color(0xFF3B1010),
    onError              = NavyDeep,
    onErrorContainer     = Color(0xFFFCA5A5),
)

@Composable
fun CohortAndroidTheme(
    darkTheme: Boolean = true,                   // premium dark-first design
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}