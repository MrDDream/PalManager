package com.paladmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

// Rayons de la maquette : 12 (champs), 14-16 (boutons, tuiles), 20-22 (cartes), 28 (feuilles).
private val PalAdminShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalPalColors = staticCompositionLocalOf { DarkPalColors }

/** Accès aux tokens de la refonte : `PalTheme.colors.surface`, `PalTheme.type.stat`... */
object PalTheme {
    val colors: PalColors
        @Composable @ReadOnlyComposable
        get() = LocalPalColors.current

    val type: PalType get() = PalType
}

/** Le ColorScheme M3 est dérivé des mêmes tokens, pour les composants Material encore utilisés. */
private fun PalColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = green.solid,
        onPrimary = green.onSolid,
        primaryContainer = green.container,
        onPrimaryContainer = green.onContainer,
        secondary = amber.solid,
        onSecondary = amber.onSolid,
        secondaryContainer = amber.container,
        onSecondaryContainer = amber.onContainer,
        tertiary = sky.solid,
        onTertiary = sky.onSolid,
        tertiaryContainer = sky.container,
        onTertiaryContainer = sky.onContainer,
        error = danger.content,
        onError = danger.onSolid,
        errorContainer = danger.container,
        onErrorContainer = danger.onContainer,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textMuted,
        surfaceTint = green.solid,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceBright = surfaceRaised,
        surfaceDim = background,
        inverseSurface = text,
        inverseOnSurface = background,
        inversePrimary = green.content,
        outline = borderStrong,
        outlineVariant = divider,
        scrim = base.scrim,
    )
}

@Composable
fun PalAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palColors = if (darkTheme) DarkPalColors else LightPalColors
    CompositionLocalProvider(LocalPalColors provides palColors) {
        MaterialTheme(
            colorScheme = palColors.toColorScheme(),
            typography = PalAdminTypography,
            shapes = PalAdminShapes,
            content = content,
        )
    }
}
