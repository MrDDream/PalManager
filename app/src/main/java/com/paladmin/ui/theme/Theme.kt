package com.paladmin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Shapes par défaut de M3 (ex. extraSmall = 4dp pour OutlinedTextField) sont trop discrètes et
// rendent les champs/cartes visuellement carrés — arrondis explicitement augmentés ici.
private val PalAdminShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val DarkColors = darkColorScheme(
    primary = PalGreen,
    onPrimary = Color(0xFF00391D),
    primaryContainer = PalGreenContainerDark,
    onPrimaryContainer = PalOnGreenContainerDark,
    secondary = PalAmber,
    onSecondary = Color(0xFF3D2900),
    secondaryContainer = PalAmberContainerDark,
    onSecondaryContainer = PalOnAmberContainerDark,
    tertiary = PalSky,
    onTertiary = Color(0xFF00344450),
    tertiaryContainer = PalSkyContainerDark,
    onTertiaryContainer = PalOnSkyContainerDark,
    error = PalDanger,
    onError = Color(0xFF3A0808),
    errorContainer = PalDangerContainerDark,
    onErrorContainer = PalDangerContainerLight,
    background = PalBackgroundDark,
    onBackground = Color(0xFFE2E6E1),
    surface = PalSurfaceDark,
    onSurface = Color(0xFFE2E6E1),
    surfaceVariant = PalSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC0CAC3),
    outline = Color(0xFF8A948C),
)

private val LightColors = lightColorScheme(
    primary = PalGreenDark,
    onPrimary = Color.White,
    primaryContainer = PalGreenContainerLight,
    onPrimaryContainer = PalOnGreenContainerLight,
    secondary = PalAmberDark,
    onSecondary = Color.White,
    secondaryContainer = PalAmberContainerLight,
    onSecondaryContainer = PalOnAmberContainerLight,
    tertiary = PalSkyDark,
    onTertiary = Color.White,
    tertiaryContainer = PalSkyContainerLight,
    onTertiaryContainer = PalOnSkyContainerLight,
    error = PalDangerDark,
    onError = Color.White,
    errorContainer = PalDangerContainerLight,
    onErrorContainer = Color(0xFF410002),
    background = PalBackgroundLight,
    surface = PalSurfaceLight,
    surfaceVariant = PalSurfaceVariantLight,
)

@Composable
fun PalAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Le Material You dynamique reprend les couleurs du fond d'écran (souvent désaturé), ce qui
    // rendait l'app terne — la palette Pal ci-dessus est donc utilisée par défaut, dynamicColor
    // reste disponible pour qui préfère s'harmoniser avec le reste de son téléphone.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalAdminTypography,
        shapes = PalAdminShapes,
        content = content,
    )
}
