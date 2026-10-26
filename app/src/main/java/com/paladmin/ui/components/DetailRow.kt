package com.paladmin.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Lignes d'une vue de détail (inventaire/progression/technologies joueur, coffre/expéditions/labo de
 * guilde...). Rendues par [DetailRowsList] — les champs ajoutés avec la refonte UI/UX ont tous une
 * valeur par défaut, les appelants existants restent compatibles. */
sealed interface DetailRow {
    /** icon/accent optionnels (ex. progression joueur : une icône + une couleur par catégorie de
     * statistiques) — restent null pour les sections qui n'en ont pas besoin (inventaire...).
     * [trailing] : compteur affiché à droite en monospace (ex. "16/48" pour une section
     * d'inventaire). [tone] : famille de couleur du thème, préférée à [accent] (couleur brute). */
    data class Section(
        val text: String,
        val icon: ImageVector? = null,
        val accent: Color? = null,
        val trailing: String? = null,
        val tone: ToneKey? = null,
    ) : DetailRow

    /** Ligne image + libellé. [status] : état affiché à droite (ex. "Débloquée"), en couleur
     * d'accent si [positive], sinon atténué — le libellé est alors atténué lui aussi. [tint] :
     * fond teinté de la vignette (ex. technologie ancienne/boss, violet). */
    data class WithImage(
        val imagePath: String,
        val text: String,
        val status: String? = null,
        val positive: Boolean = true,
        val tint: ToneKey? = null,
    ) : DetailRow

    /** accent optionnel (ex. liste de missions d'expédition : vert débloquée / rouge verrouillée)
     * — reste null pour un texte neutre (messages d'erreur, indisponibilité...). */
    data class Plain(val text: String, val accent: Color? = null, val tone: ToneKey? = null) : DetailRow

    data class Grid(val items: List<InventoryGridItem>) : DetailRow

    /** Groupe de compteurs libellé/valeur (ex. statistiques de progression joueur). Précédé d'une
     * [Section], il s'affiche dans la même carte que le titre de cette section. accent/tone :
     * teinte partagée par le groupe (même code couleur que la [Section] associée). */
    data class Stats(val items: List<Pair<String, String>>, val accent: Color? = null, val tone: ToneKey? = null) : DetailRow

    /** Chiffres clés en tuiles (grand nombre + libellé), en tête de fiche (ex. Niveau, Boss vaincus). */
    data class Kpis(val items: List<Pair<String, String>>) : DetailRow

    /** Carte de progression "libellé — current/total" avec une jauge (ex. technologies débloquées). */
    data class Progress(val label: String, val current: Int, val total: Int, val tone: ToneKey = ToneKey.Sky) : DetailRow
}
