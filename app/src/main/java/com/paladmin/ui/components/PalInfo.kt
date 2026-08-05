package com.paladmin.ui.components

/** Un seul des deux (attaque mêlée / à distance) est réellement renseigné selon le type d'attaque
 * du Pal, l'autre reste à 0 côté API — on prend le plus élevé des deux comme "Attaque". */
data class PalIvInfo(val health: Int, val attack: Int, val defense: Int)

data class PalSoulInfo(val health: Int, val attack: Int, val defense: Int, val craftSpeed: Int)

/** Modèle d'affichage d'un Pal, résolu (nom/image d'espèce, compétences/passifs traduits) avant
 * d'atteindre la Compose UI — partagé entre l'équipe d'un joueur et les Pals de camp d'une guilde.
 * IV/Âme ne sont documentés par PalDefender que pour les Pals d'équipe (GET /pals), pas pour les
 * Pals de camp de guilde (GET /guild/{id}) — restent null côté guilde. */
data class PalInfo(
    val imagePath: String,
    val speciesName: String,
    val nickname: String,
    val level: Int,
    val gender: String,
    val shiny: Boolean,
    val workerSick: String? = null,
    val activeSkills: List<String> = emptyList(),
    val passives: List<String> = emptyList(),
    val iv: PalIvInfo? = null,
    val soul: PalSoulInfo? = null,
)

/** Groupe de Pals avec un libellé optionnel (ex. nom de la base) — null = pas d'en-tête (équipe joueur). */
data class PalGroup(val label: String?, val pals: List<PalInfo>)

data class PalGridUiState(
    val title: String,
    val isLoading: Boolean = true,
    val groups: List<PalGroup> = emptyList(),
    val error: String? = null,
)
