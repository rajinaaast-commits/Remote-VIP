package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuraColorScheme = darkColorScheme(
    primary = AuraCyanNeon,
    onPrimary = AuraDarkBg,
    primaryContainer = AuraKeyBg,
    onPrimaryContainer = AuraCyanNeon,
    secondary = AuraCyanDim,
    onSecondary = AuraDarkBg,
    background = AuraDarkBg,
    onBackground = AuraTextPrimary,
    surface = AuraSurfaceDark,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraCardDark,
    onSurfaceVariant = AuraTextSecondary
)

@Composable
fun AuraRemoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AuraRemoteTheme(content = content)
}
