package com.paladmin.data.local.dataset

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Reflète le JSON produit par tools/scrape_paldb.py — voir ce script pour le format exact. */
@Serializable
data class ItemDatasetEntry(
    val id: String,
    val slug: String,
    val category: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val description: String = "",
    val stats: Map<String, String> = emptyMap(),
    val image: String,
)

@Serializable
data class PalStatsDto(val hp: Int, val attack: Int, val defense: Int)

@Serializable
data class PalWorkSuitabilityDto(val job: String, val level: Int)

@Serializable
data class PalPartnerSkillDto(val name: String, val description: String)

@Serializable
data class PalMapPositionDto(val x: Double, val y: Double)

/** Champs enrichis par tools/enrich_pals.py à partir des données déjà extraites du jeu par PalSite. */
@Serializable
data class PalDatasetEntry(
    val id: String,
    val slug: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val image: String,
    val element1: String = "Normal",
    val element2: String? = null,
    val rarity: Int = 1,
    val zukanIndex: Int = -1,
    val stats: PalStatsDto? = null,
    val workSuitabilities: List<PalWorkSuitabilityDto> = emptyList(),
    val partnerSkill: PalPartnerSkillDto? = null,
    val locations: List<String> = emptyList(),
    val mapPosition: PalMapPositionDto? = null,
)

@Serializable
data class HumanDropDto(
    val itemId: String,
    val name: String,
    val minQuantity: Int,
    val maxQuantity: Int,
    val probability: Int,
)

/** stats/workSuitabilities/drops enrichis par tools/scrape_paldb.py::scrape_human_details (page
 * wiki paldb.cc du slug — première variante cosmétique documentée, prise comme représentative). */
@Serializable
data class HumanDatasetEntry(
    val id: String,
    val slug: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val image: String,
    val stats: PalStatsDto? = null,
    val workSuitabilities: List<PalWorkSuitabilityDto> = emptyList(),
    val drops: List<HumanDropDto> = emptyList(),
)

/** Reflète tools/scrape_paldb.py::scrape_technologies (paldb.cc/fr/Technologies, une seule page
 * pour les 587 technologies — id = même identifiant que Techs.Unlocked côté PalDefender). */
@Serializable
data class TechnologyDatasetEntry(
    val id: String,
    val level: Int,
    val cost: Int,
    val category: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val image: String,
)

/** Reflète tools/import_paljson_skills.py (source github.com/MrDDream/PalJSON/data/skills.js — id =
 * EPalWazaID, le même identifiant que ActiveSkills/LearntSkills côté PalDefender). element = même
 * valeur que PalLabels.ELEMENTS (icône dans assets/images/elements/), power/cooldown affichés tels
 * quels (pas de traduction, ce sont des nombres). */
@Serializable
data class ActiveSkillDatasetEntry(
    val id: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val element: String = "",
    val power: Int = 0,
    val cooldown: Int = 0,
)

/** Reflète tools/import_paljson_skills.py (source github.com/MrDDream/PalJSON/data/passives.js —
 * id = le même identifiant que Passives côté PalDefender ; couvre aussi les passifs négatifs
 * "_downN" et les variantes "ElementBoost_*_PAL", absents du catalogue d'implants d'items.json).
 * description_fr replie sur la description EN côté script d'import si PalJSON n'a pas de FR pour
 * cette entrée — jamais vide si description ne l'est pas. */
@Serializable
data class PassiveDatasetEntry(
    val id: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val rank: Int = 0,
    val description: String = "",
    @SerialName("description_fr") val descriptionFr: String = "",
)
