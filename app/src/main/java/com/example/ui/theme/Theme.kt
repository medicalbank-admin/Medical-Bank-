package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = OnBrandBlue,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = OnBrandBlueContainer,
    secondary = BrandRed,
    onSecondary = OnBrandRed,
    secondaryContainer = BrandRedContainer,
    onSecondaryContainer = OnBrandRedContainer,
    tertiary = BrandDarkerBlue,
    onTertiary = OnBrandDarkerBlue,
    error = BrandRed,
    onError = OnBrandRed,
    errorContainer = BrandErrorContainer,
    onErrorContainer = OnBrandErrorContainer,
    background = BrandBackground,
    onBackground = BrandBlack,
    surface = BrandSurface,
    onSurface = BrandBlack,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    scrim = BrandScrim
)

@Composable
fun MedBankTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
