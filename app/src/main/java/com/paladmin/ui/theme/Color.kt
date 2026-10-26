package com.paladmin.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Accents bruts de la marque — gardés pour les usages "data" (icônes de travail des Pals, etc.)
// qui veulent la teinte vive quel que soit le thème.
val PalGreen = Color(0xFF4CD08A)
val PalAmber = Color(0xFFE8A33D)
val PalSky = Color(0xFF6FC3E8)
val PalDanger = Color(0xFFF07A7A)
val PalPurple = Color(0xFFB79CF0)

/**
 * Une famille de couleur d'accent, déclinée comme dans la maquette "Refonte UI/UX" :
 * - [solid] : fond d'un bouton plein / pastille / indicateur, avec [onSolid] dessus ;
 * - [container] : fond teinté discret (tuiles, chips sélectionnées), avec [onContainer] dessus ;
 * - [border] : contour d'une carte ou d'un chip sélectionné ;
 * - [content] : l'accent utilisé comme couleur de texte/icône directement sur une surface.
 */
@Immutable
data class Tone(
    val solid: Color,
    val onSolid: Color,
    val container: Color,
    val onContainer: Color,
    val border: Color,
    val content: Color,
)

/** Palette complète de l'app (neutres + accents). Lue via [PalTheme.colors]. */
@Immutable
data class PalColors(
    val isDark: Boolean,
    /** Fond de l'écran. */
    val background: Color,
    /** Cartes et sections. */
    val surface: Color,
    /** Éléments posés sur une carte (boutons secondaires, lignes cliquables). */
    val surfaceRaised: Color,
    /** Barres fixes (bas d'écran, barre de navigation, feuilles du bas). */
    val surfaceBar: Color,
    /** Fond des champs de saisie (plus sombre que la carte qui les contient). */
    val field: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val text: Color,
    val textSecondary: Color,
    val textBody: Color,
    val textMuted: Color,
    val textFaint: Color,
    val green: Tone,
    val sky: Tone,
    val amber: Tone,
    val danger: Tone,
    val purple: Tone,
    val neutral: Tone,
    /** Couleurs d'avatar (initiale sur pastille) — cycle stable par joueur. */
    val avatars: List<Pair<Color, Color>>,
)

val DarkPalColors = PalColors(
    isDark = true,
    background = Color(0xFF0E1316),
    surface = Color(0xFF161D21),
    surfaceRaised = Color(0xFF1C252A),
    surfaceBar = Color(0xFF131A1E),
    field = Color(0xFF0E1316),
    border = Color(0xFF243036),
    borderStrong = Color(0xFF2A363C),
    divider = Color(0xFF222C31),
    text = Color(0xFFE8EEEA),
    textSecondary = Color(0xFFC9D4CE),
    textBody = Color(0xFFB5C2BB),
    textMuted = Color(0xFF9AABA3),
    textFaint = Color(0xFF8B9C94),
    green = Tone(
        solid = PalGreen, onSolid = Color(0xFF062414),
        container = Color(0xFF0F2A1D), onContainer = Color(0xFF7FE0AC),
        border = Color(0xFF2B4A3A), content = PalGreen,
    ),
    sky = Tone(
        solid = PalSky, onSolid = Color(0xFF062030),
        container = Color(0xFF0C2733), onContainer = Color(0xFFA9D9EE),
        border = Color(0xFF1F3A48), content = PalSky,
    ),
    amber = Tone(
        solid = PalAmber, onSolid = Color(0xFF2A1800),
        container = Color(0xFF2E2310), onContainer = Color(0xFFF2C27A),
        border = Color(0xFF5C4419), content = PalAmber,
    ),
    danger = Tone(
        solid = Color(0xFFE25C5C), onSolid = Color(0xFF2A0606),
        container = Color(0xFF3A1515), onContainer = Color(0xFFF5A3A3),
        border = Color(0xFF5A2323), content = PalDanger,
    ),
    purple = Tone(
        solid = PalPurple, onSolid = Color(0xFF1E1236),
        container = Color(0xFF241B38), onContainer = Color(0xFFD2C2F7),
        border = Color(0xFF3E3160), content = PalPurple,
    ),
    neutral = Tone(
        solid = Color(0xFF34444B), onSolid = Color(0xFFE8EEEA),
        container = Color(0xFF222C31), onContainer = Color(0xFFC9D4CE),
        border = Color(0xFF2A363C), content = Color(0xFFC9D4CE),
    ),
    avatars = listOf(
        Color(0xFF4CD08A) to Color(0xFF062414),
        Color(0xFF6FC3E8) to Color(0xFF062030),
        Color(0xFFE8A33D) to Color(0xFF2A1800),
        Color(0xFFB79CF0) to Color(0xFF1E1236),
        Color(0xFFF0907A) to Color(0xFF2E0F06),
        Color(0xFF8FD8C8) to Color(0xFF05241D),
        Color(0xFFE6D27A) to Color(0xFF2A2205),
    ),
)

val LightPalColors = PalColors(
    isDark = false,
    background = Color(0xFFF3F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEDF2EE),
    surfaceBar = Color(0xFFFFFFFF),
    field = Color(0xFFF6F9F7),
    border = Color(0xFFDCE5DF),
    borderStrong = Color(0xFFCAD5CE),
    divider = Color(0xFFE4EBE6),
    text = Color(0xFF121A16),
    textSecondary = Color(0xFF34413B),
    textBody = Color(0xFF45524C),
    textMuted = Color(0xFF5A6962),
    textFaint = Color(0xFF66756E),
    green = Tone(
        solid = Color(0xFF1F9D5C), onSolid = Color.White,
        container = Color(0xFFDDF4E6), onContainer = Color(0xFF125C36),
        border = Color(0xFFA6D9BB), content = Color(0xFF167A47),
    ),
    sky = Tone(
        solid = Color(0xFF1C7FA8), onSolid = Color.White,
        container = Color(0xFFDDEFF8), onContainer = Color(0xFF0D506A),
        border = Color(0xFFA9D3E6), content = Color(0xFF136A8E),
    ),
    amber = Tone(
        solid = PalAmber, onSolid = Color(0xFF2A1800),
        container = Color(0xFFFCEBCF), onContainer = Color(0xFF6E4300),
        border = Color(0xFFEBC889), content = Color(0xFF935A00),
    ),
    danger = Tone(
        solid = Color(0xFFC93636), onSolid = Color.White,
        container = Color(0xFFFBE3E3), onContainer = Color(0xFF8E1B1B),
        border = Color(0xFFEDB4B4), content = Color(0xFFB02727),
    ),
    purple = Tone(
        solid = Color(0xFF7652C9), onSolid = Color.White,
        container = Color(0xFFEDE6FB), onContainer = Color(0xFF4A2D91),
        border = Color(0xFFCFC0F0), content = Color(0xFF6342B3),
    ),
    neutral = Tone(
        solid = Color(0xFFB9C6BF), onSolid = Color(0xFF121A16),
        container = Color(0xFFE7EDE9), onContainer = Color(0xFF34413B),
        border = Color(0xFFCAD5CE), content = Color(0xFF34413B),
    ),
    avatars = listOf(
        Color(0xFF4CD08A) to Color(0xFF062414),
        Color(0xFF6FC3E8) to Color(0xFF062030),
        Color(0xFFE8A33D) to Color(0xFF2A1800),
        Color(0xFFB79CF0) to Color(0xFF1E1236),
        Color(0xFFF0907A) to Color(0xFF2E0F06),
        Color(0xFF8FD8C8) to Color(0xFF05241D),
        Color(0xFFE6D27A) to Color(0xFF2A2205),
    ),
)
