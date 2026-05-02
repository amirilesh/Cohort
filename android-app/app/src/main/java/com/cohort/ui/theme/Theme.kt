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
    primary              = IrisIndigo,                  // #818CF8 soft indigo
    onPrimary            = NavyDeep,
    primaryContainer     = Indigo100,                   // #1A1F42 muted container
    onPrimaryContainer   = Color(0xFFBEC4E8),           // softer violet for readability
    secondary            = IrisCyan,                    // #67E8F9 teal cyan
    onSecondary          = NavyDeep,
    secondaryContainer   = Color(0xFF0D2A35),           // deeper cyan container
    onSecondaryContainer = Color(0xFF8EEDF7),           // softer cyan text
    background           = NavyDeep,                    // #0B0E1A deepest
    onBackground         = TextPrimary,                 // #ECEFF4 near-white
    surface              = NavySurface,                 // #131726
    onSurface            = TextPrimary,                 // #ECEFF4
    surfaceVariant       = NavyCard,                    // #191D2E
    onSurfaceVariant     = TextSecondary,               // #9BA3B8 muted
    outline              = BorderDark,                  // #252A3E
    outlineVariant       = Color(0xFF1E2233),           // even subtler border variant
    error                = Color(0xFFF87171),
    errorContainer       = Color(0xFF2D1212),
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