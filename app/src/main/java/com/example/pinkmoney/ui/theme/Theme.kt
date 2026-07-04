package com.example.pinkmoney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = PinkOnPrimaryDark,
    primaryContainer = PinkPrimaryContainerDark,
    onPrimaryContainer = PinkOnPrimaryContainerDark,
    secondary = PinkSecondaryDark,
    onSecondary = PinkOnSecondaryDark,
    secondaryContainer = PinkSecondaryContainerDark,
    onSecondaryContainer = PinkOnSecondaryContainerDark,
    background = PinkBackgroundDark,
    surface = PinkSurfaceDark,
    surfaceVariant = PinkSurfaceVariantDark,
    onSurface = PinkOnSurfaceDark,
    onBackground = PinkOnSurfaceDark,
    onSurfaceVariant = PinkOnSurfaceVariantDark,
    outlineVariant = PinkOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PinkPrimaryLight,
    onPrimary = PinkOnPrimaryLight,
    primaryContainer = PinkPrimaryContainerLight,
    onPrimaryContainer = PinkOnPrimaryContainerLight,
    secondary = PinkSecondaryLight,
    onSecondary = PinkOnSecondaryLight,
    secondaryContainer = PinkSecondaryContainerLight,
    onSecondaryContainer = PinkOnSecondaryContainerLight,
    background = PinkBackgroundLight,
    surface = PinkSurfaceLight,
    surfaceVariant = PinkSurfaceVariantLight,
    onSurface = PinkOnSurfaceLight,
    onBackground = PinkOnSurfaceLight,
    onSurfaceVariant = PinkOnSurfaceVariantLight,
    outlineVariant = PinkOutlineLight
)

@Composable
fun PinkMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
