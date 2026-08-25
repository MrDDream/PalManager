package com.paladmin.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Lignes d'une boîte de dialogue de détail (inventaire/équipe/progression joueur, coffre/expéditions/labo de guilde...). */
sealed interface DetailRow {
    /** icon/accent optionnels (ex. progression joueur : une icône + une couleur par catégorie de
     * statistiques) — restent null pour les sections qui n'en ont pas besoin (inventaire...). */
    data class Section(val text: String, val icon: ImageVector? = null, val accent: Color? = null) : DetailRow
    data class WithImage(val imagePath: String, val text: String) : DetailRow
    /** accent optionnel (ex. liste de missions d'expédition : vert débloquée / rouge verrouillée)
     * — reste null pour un texte neutre (messages d'erreur, indisponibilité...). */
    data class Plain(val text: String, val accent: Color? = null) : DetailRow
    data class Grid(val items: List<InventoryGridItem>) : DetailRow
    /** Grille de tuiles libellé/valeur (ex. statistiques de progression joueur) — plus lisible
     * qu'une liste de lignes "Libellé : valeur" pour une vingtaine de compteurs. accent : teinte
     * partagée par toutes les tuiles de ce groupe (même code couleur que la [Section] associée). */
    data class Stats(val items: List<Pair<String, String>>, val accent: Color? = null) : DetailRow
}
