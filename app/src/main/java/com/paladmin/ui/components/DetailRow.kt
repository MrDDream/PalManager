package com.paladmin.ui.components

/** Lignes d'une boîte de dialogue de détail (inventaire/équipe/progression joueur, coffre/expéditions/labo de guilde...). */
sealed interface DetailRow {
    data class Section(val text: String) : DetailRow
    data class WithImage(val imagePath: String, val text: String) : DetailRow
    data class Plain(val text: String) : DetailRow
    data class Grid(val items: List<InventoryGridItem>) : DetailRow
    /** Grille de tuiles libellé/valeur (ex. statistiques de progression joueur) — plus lisible
     * qu'une liste de lignes "Libellé : valeur" pour une vingtaine de compteurs. */
    data class Stats(val items: List<Pair<String, String>>) : DetailRow
}
