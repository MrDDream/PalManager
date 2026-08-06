package com.paladmin.ui.palcreator

import com.paladmin.data.remote.paldefender.PalIVs
import com.paladmin.data.remote.paldefender.PalSoulRanks
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Schéma confirmé via la doc PalDefender (docs/en/FileTypes/PalTemplate.md) : seul `PalID` est
 * obligatoire, tout le reste optionnel. On expose ici le sous-ensemble le plus utile d'un
 * créateur de template (pas tous les champs documentés — WorkerSick/PhysicalHealth/HP/SP/MP/
 * Shield/Hunger/SAN/Support/ExtraWorkSuitabilities/DisableWorkPreferences/UniqueNPCID/SkinId/Exp/
 * PartnerSkillLevel/CondensedPals/UnusedStatusPoints/FriendshipPoints/ImportedCharacter/
 * LearntSkills restent à leur valeur serveur par défaut, non gérés par ce formulaire).
 */
@Serializable
data class PalTemplateFile(
    @SerialName("PalID") val palId: String,
    @SerialName("Nickname") val nickname: String? = null,
    @SerialName("Gender") val gender: String? = null,
    @SerialName("Level") val level: Int? = null,
    @SerialName("Shiny") val shiny: Boolean? = null,
    @SerialName("IVs") val ivs: PalIVs? = null,
    @SerialName("PalSouls") val palSouls: PalSoulRanks? = null,
    @SerialName("ActiveSkills") val activeSkills: List<String>? = null,
    @SerialName("Passives") val passives: List<String>? = null,
)
