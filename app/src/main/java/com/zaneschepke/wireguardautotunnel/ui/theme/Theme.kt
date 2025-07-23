package com.zaneschepke.wireguardautotunnel.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV

private val DarkColorScheme =
    darkColorScheme(
        primary = ThemeColors.Dark.primary,
        surface = ThemeColors.Dark.surface,
        background = ThemeColors.Dark.background,
        secondary = ThemeColors.Dark.secondary,
        onSurface = ThemeColors.Dark.onSurface,
        onSecondaryContainer = ThemeColors.Dark.primary,
        outline = ThemeColors.Dark.outline,
        onBackground = ThemeColors.Dark.onBackground,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ThemeColors.Light.primary,
        surface = ThemeColors.Light.surface,
        background = ThemeColors.Light.background,
        secondary = ThemeColors.Light.secondary,
        onSurface = ThemeColors.Light.onSurface,
        onSecondaryContainer = ThemeColors.Light.primary,
        outline = ThemeColors.Light.outline,
        onBackground = ThemeColors.Light.onBackground,
    )

enum class Theme {
    AUTOMATIC,
    LIGHT,
    DARK,
    DARKER,
    AMOLED,
    DYNAMIC,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireguardAutoTunnelTheme(theme: Theme = Theme.AUTOMATIC, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isTv = LocalIsAndroidTV.current
    var isDark = isSystemInDarkTheme()
    val autoTheme = if (isDark) DarkColorScheme else LightColorScheme
    val colorScheme =
        when (theme) {
            Theme.AUTOMATIC -> autoTheme
            Theme.DARK -> {
                isDark = true
                DarkColorScheme
            }
            Theme.DARKER -> {
                isDark = true
                DarkColorScheme.copy(surface = BalticSea, background = BalticSea)
            }
            Theme.AMOLED -> {
                isDark = true
                DarkColorScheme.copy(
                    surface = Color.Black,
                    background = Color.Black,
                    primary = ElectricTeal,
                )
            }
            Theme.LIGHT -> {
                isDark = false
                LightColorScheme
            }
            Theme.DYNAMIC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isDark) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                } else {
                    autoTheme
                }
            }
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = Color.Transparent.toArgb()
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    // Make hover/ripple more obvious on TV
    val rippleConfig =
        if (isTv) {
            RippleConfiguration(
                color = colorScheme.outline.copy(alpha = 0.12f),
                rippleAlpha =
                    RippleAlpha(
                        pressedAlpha = 0.7f,
                        focusedAlpha = 0.6f,
                        draggedAlpha = 0.9f,
                        hoveredAlpha = 0.3f,
                    ),
            )
        } else null

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
