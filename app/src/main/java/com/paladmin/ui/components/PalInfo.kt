package com.paladmin.ui.components

/** Un seul des deux (attaque mêlée / à distance) est réellement renseigné selon le type d'attaque
 * du Pal, l'autre reste à 0 côté API — on prend le plus élevé des deux comme "Attaque". */
data class PalIvInfo(val health: Int, val attack: Int, val defense: Int)

data class PalSoulInfo(val health: Int, val attack: Int, val defense: Int, val craftSpeed: Int)

/** Compétence active résolue pour l'affichage — element/power/cooldown restent null quand l'id n'a
 * pas été trouvé dans ActiveSkillCatalog (repli sur le Fruit de compétence ou l'id humanisé, qui
 * n'ont pas cette info). */
data class ActiveSkillDisplay(
    val name: String,
    val element: String? = null,
    val power: Int? = null,
    val cooldown: Int? = null,
)

/** Passif résolu pour l'affichage — rank/descriptionLines restent respectivement null/vide pour la
 * même raison (repli hors catalogue). rank pilote le code couleur (négatif = mauvais passif, 1-5 =
 * rareté croissante). descriptionLines : un effet par ligne (voir [com.paladmin.util.splitPassiveEffects]),
 * plus lisible qu'un seul paragraphe pour les passifs à effets multiples. */
data class PassiveDisplay(
    val name: String,
    val rank: Int? = null,
    val descriptionLines: List<String> = emptyList(),
)

/** Modèle d'affichage d'un Pal, résolu (nom/image d'espèce, compétences/passifs traduits) avant
 * d'atteindre la Compose UI — partagé entre l'équipe d'un joueur et les Pals de camp d'une guilde.
 * IV/Âme ne sont documentés par PalDefender que pour les Pals d'équipe (GET /pals), pas pour les
 * Pals de camp de guilde (GET /guild/{id}) — restent null côté guilde. */
data class PalInfo(
    val imagePath: String,
    val speciesName: String,
    val nickname: String,
    val level: Int,
    /** Valeur brute API ("Male"/"Female"/autre) pour le symbole ♂/♀ — [gender] reste le libellé
     * déjà traduit pour l'affichage textuel. */
    val genderRaw: String,
    val gender: String,
    val shiny: Boolean,
    /** PalID préfixé "BOSS_"/"Boss_" (voir [com.paladmin.util.basePalImageId]) — même icône que le
     * toggle Alpha du Créateur de Pal (assets/images/status/boss_alpha.webp). */
    val isBoss: Boolean = false,
    val workerSick: String? = null,
    /** Élément principal (PalEntity.element1) — absent pour les Pals non trouvés dans le
     * catalogue local (ex. PalID inconnu). */
    val element: String? = null,
    /** Non documenté côté camp de guilde (GuildDetailResponse) — reste null dans ce cas. */
    val exp: Long? = null,
    val activeSkills: List<ActiveSkillDisplay> = emptyList(),
    val passives: List<PassiveDisplay> = emptyList(),
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
    /** Active la recherche + pagination client (Palbox, potentiellement des centaines d'entrées) —
     * pas nécessaire pour l'équipe (max 5) ni les camps de guilde (déjà groupés/petits). */
    val searchable: Boolean = false,
)
