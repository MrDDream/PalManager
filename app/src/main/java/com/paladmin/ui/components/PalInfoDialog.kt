package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paladmin.R
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.util.formatThousands

/** Détail complet d'un Pal (équipe joueur, Palbox ou camp de guilde, même modèle [PalInfo] pour
 * les trois) — en-tête façon fiche (image, nom, XP, niveau, badge d'élément, barres d'IV) suivi
 * des sections Passifs/Compétences actives. Pas de [AlertDialog.title] dédié : tout tient dans
 * [AlertDialog.text] pour un layout entièrement personnalisé. */
@Composable
fun PalInfoDialog(pal: PalInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PalHeader(pal)
                pal.element?.let { ElementBadge(it) }
                pal.iv?.let { StatBars(stringResource(R.string.pal_info_iv_header), it) }
                pal.soul?.let { SoulBars(stringResource(R.string.pal_info_soul_header), it) }
                PassiveSection(stringResource(R.string.pal_info_passives), pal.passives)
                ActiveSkillSection(stringResource(R.string.pal_info_active_skills), pal.activeSkills)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

private fun genderGlyph(genderRaw: String): Pair<String, Color>? = when (genderRaw) {
    "Male" -> "♂" to Color(0xFF60A5FA)
    "Female" -> "♀" to Color(0xFFF472B6)
    else -> null
}

@Composable
private fun PalHeader(pal: PalInfo) {
    Row(verticalAlignment = Alignment.Top) {
        FallbackAsyncImage(
            model = pal.imagePath,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pal.nickname.ifBlank { pal.speciesName }, style = MaterialTheme.typography.titleLarge)
                genderGlyph(pal.genderRaw)?.let { (symbol, color) ->
                    Text(symbol, color = color, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp))
                }
                if (pal.shiny) {
                    FallbackAsyncImage(
                        model = "file:///android_asset/images/status/lucky.webp",
                        modifier = Modifier.padding(start = 6.dp).size(22.dp),
                    )
                }
                if (pal.isBoss) {
                    FallbackAsyncImage(
                        model = "file:///android_asset/images/status/boss_alpha.webp",
                        modifier = Modifier.padding(start = 6.dp).size(22.dp),
                    )
                }
            }
            if (pal.nickname.isNotBlank() && pal.nickname != pal.speciesName) {
                Text(pal.speciesName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pal.exp?.let {
                Text(
                    stringResource(R.string.pal_xp_fmt, formatThousands(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.pal_level_fmt, pal.level),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ElementBadge(element: String) {
    val color = PalLabels.ELEMENT_COLORS[element] ?: MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        FallbackAsyncImage(model = "file:///android_asset/images/elements/$element.webp", modifier = Modifier.size(16.dp))
        Text(
            PalLabels.elementLabel(element).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** IV : PV/Attaque/Défense sur 100 (échelle native des IV Palworld). Santé -> primaire, Attaque ->
 * secondaire, Défense -> tertiaire — reprend les 3 couleurs d'accent déjà utilisées ailleurs dans
 * l'app (tuiles du Dashboard...). */
@Composable
private fun StatBars(label: String, iv: PalIvInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatBar(stringResource(R.string.pal_stat_short_hp), iv.health, 100, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatBar(stringResource(R.string.palcreator_stat_atk), iv.attack, 100, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            StatBar(stringResource(R.string.palcreator_stat_def), iv.defense, 100, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        }
    }
}

/** Âme de Pal : PV/Attaque/Défense/Fabrication, même forme que [StatBars] — pas d'échelle native
 * fixe (empilable sans limite en jouant à plusieurs Pals de même espèce), plafond visuel à 20
 * (valeur par défaut du Créateur de Pal) pour rester lisible ; au-delà, la barre reste pleine. */
@Composable
private fun SoulBars(label: String, soul: PalSoulInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatBar(stringResource(R.string.pal_stat_short_hp), soul.health, 20, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatBar(stringResource(R.string.palcreator_stat_atk), soul.attack, 20, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            StatBar(stringResource(R.string.palcreator_stat_def), soul.defense, 20, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            StatBar(stringResource(R.string.palcreator_stat_work), soul.craftSpeed, 20, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0, max) / max.toFloat())
                    .background(color, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Libellé de catégorie hors du cadre, cadre de fond réservé au seul contenu — pour distinguer
 * visuellement le titre de bloc (Âme, compétences...) de ses valeurs. Public : même rendu
 * réutilisé par le Créateur de Pal pour une cohérence visuelle affichage/création. */
@Composable
fun PalInfoSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(10.dp),
            content = content,
        )
    }
}

/** Pastilles arrondies façon "chip" (élément + puissance) plutôt qu'une simple liste — tap pour le
 * détail complet (élément traduit + temps de recharge). element/power/cooldown restent absents
 * quand la compétence n'a pas été trouvée dans ActiveSkillCatalog (repli sur le Fruit de compétence
 * ou l'id humanisé). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveSkillSection(label: String, skills: List<ActiveSkillDisplay>) {
    if (skills.isEmpty()) return
    var selected by remember { mutableStateOf<ActiveSkillDisplay?>(null) }
    PalInfoSection(label) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            skills.forEach { skill ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                        .clickable { selected = skill }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    skill.element?.let { element ->
                        FallbackAsyncImage(model = "file:///android_asset/images/elements/$element.webp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(skill.name, style = MaterialTheme.typography.bodySmall)
                    skill.power?.let {
                        Text(
                            " $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    selected?.let { skill ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(skill.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    skill.element?.let { element ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FallbackAsyncImage(model = "file:///android_asset/images/elements/$element.webp", modifier = Modifier.size(20.dp))
                            Text(PalLabels.elementLabel(element), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    skill.power?.let { Text(stringResource(R.string.skill_power_fmt, it)) }
                    skill.cooldown?.let { Text(stringResource(R.string.skill_cooldown_fmt, it)) }
                }
            },
            confirmButton = { Button(onClick = { selected = null }) { Text(stringResource(R.string.common_close)) } },
        )
    }
}

/** Bandeaux colorés selon le rang (rareté) — liseré + fond teinté + icône de rang, plutôt qu'une
 * simple ligne de texte coloré. rank/description restent absents quand le passif n'a pas été
 * trouvé dans PassiveSkillCatalog (repli hors catalogue) — bandeau neutre dans ce cas. */
@Composable
private fun PassiveSection(label: String, passives: List<PassiveDisplay>) {
    if (passives.isEmpty()) return
    var selected by remember { mutableStateOf<PassiveDisplay?>(null) }
    PalInfoSection(label) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            passives.forEach { passive ->
                val color = PalLabels.passiveRankColor(passive.rank)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.16f))
                        .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .clickable { selected = passive },
                ) {
                    Box(modifier = Modifier.width(4.dp).height(30.dp).background(color))
                    Text(
                        passive.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    passive.rank?.let { rank ->
                        RankChevrons(rank, color, modifier = Modifier.padding(end = 10.dp))
                    }
                }
            }
        }
    }
    selected?.let { passive ->
        val color = PalLabels.passiveRankColor(passive.rank)
        AlertDialog(
            onDismissRequest = { selected = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    Text(passive.name, modifier = Modifier.padding(start = 8.dp), color = color)
                }
            },
            text = {
                if (passive.descriptionLines.isEmpty()) {
                    Text(stringResource(R.string.passive_no_description))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        passive.descriptionLines.forEach { line -> Text("• $line") }
                    }
                }
            },
            confirmButton = { Button(onClick = { selected = null }) { Text(stringResource(R.string.common_close)) } },
        )
    }
}

/** Rang positif : vraie icône de rang paldb.cc (assets/images/status/passive_rank_N.webp,
 * N=1..5 — chevron(s) empilés, de plus en plus élaborés), teintée par couleur. paldb.cc n'a pas
 * d'icône dédiée aux passifs négatifs (il réutilise l'icône de rang 1 sans distinction visuelle) —
 * on garde un chevron vers le bas ici pour rester lisible en un coup d'œil. */
@Composable
private fun RankChevrons(rank: Int, color: Color, modifier: Modifier = Modifier) {
    if (rank < 0) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = color, modifier = modifier.size(20.dp))
    } else {
        val tier = rank.coerceIn(1, 5)
        FallbackAsyncImage(
            model = "file:///android_asset/images/status/passive_rank_$tier.webp",
            tint = color,
            modifier = modifier.size(20.dp),
        )
    }
}
