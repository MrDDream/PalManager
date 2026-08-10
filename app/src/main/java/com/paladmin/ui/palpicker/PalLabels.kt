package com.paladmin.ui.palpicker

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.paladmin.R

/** Éléments/métiers/rareté des Pals — libellés en ressources traduisibles (FR/EN), couleurs reprises de PalSite (src/lib/pal-labels.ts). */
object PalLabels {
    val ELEMENTS: List<String> = listOf(
        "Fire", "Water", "Leaf", "Electricity", "Ice", "Earth", "Dark", "Dragon", "Normal",
    )

    private val ELEMENT_LABEL_RES: Map<String, Int> = mapOf(
        "Fire" to R.string.element_fire,
        "Water" to R.string.element_water,
        "Leaf" to R.string.element_leaf,
        "Electricity" to R.string.element_electricity,
        "Ice" to R.string.element_ice,
        "Earth" to R.string.element_earth,
        "Dark" to R.string.element_dark,
        "Dragon" to R.string.element_dragon,
        "Normal" to R.string.element_normal,
    )

    @Composable
    fun elementLabel(element: String): String =
        ELEMENT_LABEL_RES[element]?.let { stringResource(it) } ?: element

    val ELEMENT_COLORS: Map<String, Color> = mapOf(
        "Fire" to Color(0xFFF97316),
        "Water" to Color(0xFF38BDF8),
        "Leaf" to Color(0xFF4ADE80),
        "Electricity" to Color(0xFFFACC15),
        "Ice" to Color(0xFF67E8F9),
        "Earth" to Color(0xFFA8763E),
        "Dark" to Color(0xFF7C3AED),
        "Dragon" to Color(0xFFC084FC),
        "Normal" to Color(0xFF94A3B8),
    )

    val JOB_LABEL_RES: Map<String, Int> = mapOf(
        "EmitFlame" to R.string.job_emitflame,
        "Watering" to R.string.job_watering,
        "Seeding" to R.string.job_seeding,
        "GenerateElectricity" to R.string.job_generateelectricity,
        "Handcraft" to R.string.job_handcraft,
        "Collection" to R.string.job_collection,
        "Deforest" to R.string.job_deforest,
        "Mining" to R.string.job_mining,
        "OilExtraction" to R.string.job_oilextraction,
        "ProductMedicine" to R.string.job_productmedicine,
        "Cool" to R.string.job_cool,
        "Transport" to R.string.job_transport,
        "MonsterFarm" to R.string.job_monsterfarm,
    )

    @Composable
    fun jobLabel(job: String): String =
        JOB_LABEL_RES[job]?.let { stringResource(it) } ?: job

    /** OilExtraction exclu : jamais non-nul pour aucun Pal (même exclusion que PalSite). */
    val FILTERABLE_JOBS: List<String> = JOB_LABEL_RES.keys.filterNot { it == "OilExtraction" }

    data class RarityTier(val min: Int, val max: Int, @StringRes val labelRes: Int, val color: Color)

    val RARITY_TIERS: List<RarityTier> = listOf(
        RarityTier(1, 2, R.string.rarity_common, Color(0xFF94A3B8)),
        RarityTier(3, 4, R.string.rarity_uncommon, Color(0xFF34D399)),
        RarityTier(5, 6, R.string.rarity_rare, Color(0xFF22D3EE)),
        RarityTier(7, 8, R.string.rarity_epic, Color(0xFFA855F7)),
        RarityTier(9, Int.MAX_VALUE, R.string.rarity_legendary, Color(0xFFFB7185)),
    )

    fun rarityTier(rarity: Int): RarityTier = RARITY_TIERS.find { rarity in it.min..it.max } ?: RARITY_TIERS.last()

    /** Rang de passif PalJSON (-3 à 5 observés) : rouge = négatif, gris = classique (rang 1),
     * jaune/or = Épique (rangs 2-3), bleu brillant = Légendaire (rang 4 et 5 — les "_3"/max d'une
     * famille de boost comme MoveSpeed_up_3 sont déjà rang 4, pas seulement les exclusifs
     * Arbre-Monde). null = rang inconnu (repli hors catalogue), couleur neutre. */
    @Composable
    fun passiveRankColor(rank: Int?): Color = when {
        rank == null -> MaterialTheme.colorScheme.onSurfaceVariant
        rank < 0 -> MaterialTheme.colorScheme.error
        rank <= 1 -> Color(0xFF94A3B8)
        rank in 2..3 -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }
}
