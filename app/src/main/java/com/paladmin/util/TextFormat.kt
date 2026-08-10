package com.paladmin.util

import android.content.Context
import com.paladmin.R
import com.paladmin.data.local.dataset.ActiveSkillCatalog
import com.paladmin.data.local.dataset.PassiveSkillCatalog
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.repository.ItemRepository
import com.paladmin.ui.components.ActiveSkillDisplay
import com.paladmin.ui.components.PassiveDisplay
import java.util.Locale

/** Grand nombre lisible façon jeu (ex. 45859908 -> "45 859 908") — espace comme séparateur de
 * milliers quel que soit le paramètre régional de l'appareil, plus sûr à lire qu'une abréviation
 * (K/M) pour des compteurs de progression où la valeur exacte compte. */
fun formatThousands(value: Long): String = String.format(Locale.getDefault(), "%,d", value).replace(',', ' ')
fun formatThousands(value: Int): String = formatThousands(value.toLong())

/** Les noms de dataset (items/pals/humains/compétences) sont stockés en FR+EN — ce choix respecte
 * la langue de l'app (réglages) plutôt que de toujours privilégier le FR, avec repli sur l'autre
 * langue si celle demandée est vide. */
fun pickLocalizedName(nameFr: String, nameEn: String, language: AppLanguage): String {
    val primary = if (language == AppLanguage.ENGLISH) nameEn else nameFr
    val secondary = if (language == AppLanguage.ENGLISH) nameFr else nameEn
    return primary.ifBlank { secondary }
}

/** Humanise un id technique (PascalCase/snake_case) en libellé lisible quand aucun catalogue de
 * noms n'est disponible pour le traduire correctement (ex. ids de recherche de labo de guilde,
 * ids de compétences de Pal) — pas de traduction inventée, juste un espacement lisible. */
fun prettifyId(id: String): String =
    id.replace('_', ' ').replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ").trim()

/** Les Pals Boss/Alpha ont un PalID préfixé "BOSS_" (ex. "BOSS_Foxcicle", parfois "Boss_" en
 * minuscules selon la source API) qui ne correspond à aucune entrée du dataset (id de base sans
 * préfixe) — on le retire pour résoudre nom/icône. Certains boss invoqués via compétence de
 * partenaire (ex. "BOSS_KingWhale_otomo" pour Panthalus) portent en plus un suffixe d'instance
 * "_otomo" ("compagnon" en japonais) absent du dataset — on le retire aussi. */
fun basePalImageId(palId: String): String {
    val withoutBossPrefix = if (palId.startsWith("boss_", ignoreCase = true)) palId.substring(5) else palId
    return withoutBossPrefix.removeSuffix("_otomo")
}

/** Les descriptions de passifs PalJSON enchaînent plusieurs effets sans séparateur clair côté FR
 * (l'anglais utilise "·", pas systématiquement traduit) — ex. "Vol de vie +5.0 % Restauration
 * automatique des PV des Pals +100.0 % Attaque +15.0 %". On découpe sur "·" quand présent, sinon
 * juste avant chaque nouvelle clause capitalisée qui suit un "%" (chaque effet de ce dataset se
 * termine par un pourcentage, sauf éventuellement le dernier). */
private val EFFECT_SPLIT_FALLBACK = Regex("(?<=%)\\s+(?=[A-ZÀ-Ý])")

fun splitPassiveEffects(description: String): List<String> {
    val raw = if ('·' in description) description.split('·') else description.split(EFFECT_SPLIT_FALLBACK)
    return raw.map { it.trim() }.filter { it.isNotEmpty() }
}

/** Les items du catalogue nomment ces libellés "Implant : X", "Fruit Terra : X", "X Skill Fruit: Y"...
 * — le nom utile est toujours ce qui suit le dernier ":". Générique, pas de liste de préfixes à
 * maintenir à la main. */
private fun stripCatalogPrefix(name: String): String = name.substringAfterLast(':').trim().ifBlank { name }

/** Catalogue PalJSON (tools/import_paljson_skills.py) — couvre aussi les passifs négatifs "_downN"
 * (ex. "Deffence_down2") et les variantes "ElementBoost_*_PAL", absents du catalogue d'implants
 * d'items.json. Repli sur ce dernier (implants "Implant : X" / "Implant unique : X") puis sur l'id
 * humanisé si un passif venait à manquer des deux catalogues. */
suspend fun resolvePalPassive(
    passiveSkillCatalog: PassiveSkillCatalog,
    itemRepository: ItemRepository,
    rawId: String,
    language: AppLanguage,
): PassiveDisplay {
    passiveSkillCatalog.get(rawId)?.let { entry ->
        val description = pickLocalizedName(entry.descriptionFr, entry.description, language)
        return PassiveDisplay(
            name = pickLocalizedName(entry.nameFr, entry.nameEn, language),
            rank = entry.rank,
            descriptionLines = if (description.isBlank()) emptyList() else splitPassiveEffects(description),
        )
    }
    val entry = itemRepository.getById("PalPassiveSkillChange_$rawId")
        ?: itemRepository.getById("PalPassiveSkillChange_Consumable_$rawId")
    val name = entry?.let { pickLocalizedName(it.nameFr, it.nameEn, language) }
    return PassiveDisplay(name = name?.let(::stripCatalogPrefix) ?: prettifyId(rawId))
}

/** L'id brut d'une compétence active/apprise renvoyé par l'API (ex. "SolarBeam") est son EPalWazaID —
 * catalogué directement (nom en clair) sur paldb.cc/Active_Skills, source fiable qui couvre aussi
 * les compétences exclusives à un Pal sans Fruit correspondant. Repli sur le Fruit de compétence qui
 * l'enseigne (SkillCard_X, items.json) si absent de ce catalogue — vérifié utile : un id humanisé
 * naïvement donnerait à tort "Solar Beam" alors que le vrai nom est "Solar Blast" ("Rayon Solaire"). */
suspend fun resolvePalSkill(
    activeSkillCatalog: ActiveSkillCatalog,
    itemRepository: ItemRepository,
    rawId: String,
    language: AppLanguage,
): ActiveSkillDisplay {
    activeSkillCatalog.get(rawId)?.let { entry ->
        return ActiveSkillDisplay(
            name = pickLocalizedName(entry.nameFr, entry.nameEn, language),
            element = entry.element.ifBlank { null },
            power = entry.power,
            cooldown = entry.cooldown,
        )
    }
    val entry = itemRepository.getById("SkillCard_$rawId")
    val name = entry?.let { pickLocalizedName(it.nameFr, it.nameEn, language) }
    return ActiveSkillDisplay(name = name?.let(::stripCatalogPrefix) ?: prettifyId(rawId))
}

/** "Male"/"Female" bruts de l'API PalDefender — traduits pour l'affichage, repli sur la valeur
 * brute pour toute autre valeur (ex. Pals sans genre déterminé). */
fun translatePalGender(context: Context, raw: String): String = when (raw) {
    "Male" -> context.getString(R.string.pal_gender_male)
    "Female" -> context.getString(R.string.pal_gender_female)
    else -> raw
}
