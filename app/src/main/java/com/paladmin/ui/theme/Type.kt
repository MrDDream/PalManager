package com.paladmin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paladmin.R

// Trio de la maquette : Sora pour les titres et les chiffres, Inter pour le texte courant,
// JetBrains Mono pour les adresses, IDs, versions et compteurs techniques.
val Sora = FontFamily(
    Font(R.font.sora_500, FontWeight.Medium),
    Font(R.font.sora_600, FontWeight.SemiBold),
    Font(R.font.sora_700, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
)

/** Styles nommés d'après la maquette, en complément de la Typography M3. Lus via [PalTheme.type]. */
object PalType {
    /** Nom de l'app sur l'écran d'accueil. */
    val brand = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.2).sp)
    /** Grand titre d'écran ("Mes serveurs"). */
    val display = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp)
    /** Titre de barre du haut. */
    val screenTitle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp)
    /** Titre de carte / nom d'un serveur, d'un joueur. */
    val cardTitle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    /** Grand chiffre d'une tuile de statistique. */
    val stat = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp)
    /** Titre de section dans une carte. */
    val sectionTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    /** Libellé de section en capitales ("DONNER", "GÉRER"). */
    val overline = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
    val button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val mono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp)
    val monoSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
}

// La Typography M3 est alignée sur les mêmes familles pour que les composants Material restants
// (dialogues, menus, snackbars) ne détonnent pas à côté des écrans redessinés.
val PalAdminTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
