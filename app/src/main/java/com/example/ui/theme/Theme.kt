package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TealLight,
    onPrimary = Color.White,
    primaryContainer = TealLightContainer,
    onPrimaryContainer = TealOnLightContainer,
    secondary = TealLightDarker,
    onSecondary = Color.White,
    secondaryContainer = WarmSurfaceVariant,
    onSecondaryContainer = TextPrimaryLight,
    background = WarmOffWhiteBg,
    onBackground = TextPrimaryLight,
    surface = WarmCardSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = WarmOutline,
    error = StatusRejectedLight,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF042F2A),
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = TealOnDarkContainer,
    secondary = TealDark,
    onSecondary = Color(0xFF042F2A),
    secondaryContainer = CharcoalSurfaceVariant,
    onSecondaryContainer = TextPrimaryDark,
    background = CharcoalGreenBg,
    onBackground = TextPrimaryDark,
    surface = CharcoalCardSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CharcoalOutline,
    error = StatusRejectedDark,
    onError = Color(0xFF450A0A),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun JobTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
