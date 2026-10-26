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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone
import com.paladmin.util.formatThousands

/** Détail complet d'un Pal (équipe joueur, Palbox ou camp de guilde, même modèle [PalInfo] pour
 * les trois) — en-tête façon fiche (portrait, nom, espèce, niveau, XP, élément) suivi du même
 * corps que les cartes d'équipe dépliées ([PalDetailBody] : IV, âme, compétences, passifs). */
@Composable
fun PalInfoDialog(pal: PalInfo, onDismiss: () -> Unit) {
    val c = PalTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceBar,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PalHeader(pal)
                PalDetailBody(pal)
            }
        },
        confirmButton = {
            PalButton(
                text = stringResource(R.string.common_close),
                onClick = onDismiss,
                style = PalButtonStyle.Neutral,
                height = 44.dp,
            )
        },
    )
}

/** Symbole ♂/♀ et sa couleur (valeur brute API "Male"/"Female"), null sinon. */
@Composable
private fun genderGlyph(genderRaw: String): Pair<String, Color>? = when (genderRaw) {
    "Male" -> "♂" to PalTheme.colors.sky.content
    "Female" -> "♀" to PalTheme.colors.danger.onContainer
    else -> null
}

/** Fond de vignette teinté par l'élément du Pal (couleurs "data" de [PalLabels]), neutre sinon. */
@Composable
private fun elementTint(element: String?): Color =
    element?.let { PalLabels.ELEMENT_COLORS[it] }?.copy(alpha = if (PalTheme.colors.isDark) 0.16f else 0.22f) ?: PalTheme.colors.field

@Composable
private fun PalNameLine(pal: PalInfo, nameStyle: androidx.compose.ui.text.TextStyle, iconSize: androidx.compose.ui.unit.Dp) {
    val c = PalTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            pal.nickname.ifBlank { pal.speciesName },
            style = nameStyle,
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        genderGlyph(pal.genderRaw)?.let { (symbol, color) ->
            Text(symbol, color = color, style = nameStyle.copy(fontSize = nameStyle.fontSize * 0.93f), modifier = Modifier.semantics { contentDescription = pal.gender })
        }
        if (pal.shiny) FallbackAsyncImage("file:///android_asset/images/status/lucky.webp", Modifier.size(iconSize))
        if (pal.isBoss) FallbackAsyncImage("file:///android_asset/images/status/boss_alpha.webp", Modifier.size(iconSize))
    }
}

/** Ligne "élément · N passifs" sous le nom (icône d'élément en tête). */
@Composable
private fun ElementLine(pal: PalInfo) {
    val c = PalTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        pal.element?.let { FallbackAsyncImage("file:///android_asset/images/elements/$it.webp", Modifier.size(18.dp)) }
        val passives = if (pal.passives.isEmpty()) {
            stringResource(R.string.players_v2_no_passive)
        } else {
            pluralStringResource(R.plurals.players_v2_passive_count, pal.passives.size, pal.passives.size)
        }
        Text(
            listOfNotNull(pal.element?.let { PalLabels.elementLabel(it) }, passives).joinToString(" · "),
            style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp),
            color = c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PalHeader(pal: PalInfo) {
    val c = PalTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(elementTint(pal.element)),
        ) {
            FallbackAsyncImage(model = pal.imagePath, modifier = Modifier.size(60.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            PalNameLine(pal, PalTheme.type.cardTitle, 20.dp)
            ElementLine(pal)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoBadge(stringResource(R.string.pal_level_fmt, pal.level), tone = c.green)
                pal.exp?.let {
                    Text(stringResource(R.string.pal_xp_fmt, formatThousands(it)), style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textMuted)
                }
            }
        }
    }
}

/**
 * Carte de l'onglet Équipe de la fiche joueur : portrait teinté par l'élément, nom + sexe,
 * élément et nombre de passifs, niveau à droite ; dépliée, le [PalDetailBody] complet.
 */
@Composable
fun PalTeamCard(pal: PalInfo, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val stateLabel = stringResource(if (expanded) R.string.players_v2_state_expanded else R.string.players_v2_state_collapsed)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceRaised)
            .border(1.dp, if (expanded) c.green.border else c.surfaceRaised, shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateLabel }
                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(elementTint(pal.element)),
            ) {
                FallbackAsyncImage(model = pal.imagePath, modifier = Modifier.size(54.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PalNameLine(pal, PalTheme.type.sectionTitle, 16.dp)
                ElementLine(pal)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.players_v2_level_short), style = PalTheme.type.caption.copy(fontSize = 11.sp), color = c.textMuted)
                Text(pal.level.toString(), style = PalTheme.type.cardTitle.copy(fontSize = 18.sp), color = c.text)
            }
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)) {
                PalDetailBody(pal, showSpeciesLine = true)
            }
        }
    }
}

/**
 * Corps détaillé d'un Pal : IV et âme côte à côte (jauges), compétences actives (élément,
 * puissance, recharge) et passifs en pastilles colorées par rang (tap = description).
 * [showSpeciesLine] : rappelle l'espèce (si surnommé) et l'XP, absents de l'en-tête des cartes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalDetailBody(pal: PalInfo, modifier: Modifier = Modifier, showSpeciesLine: Boolean = false) {
    val c = PalTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showSpeciesLine) {
            val parts = listOfNotNull(
                pal.speciesName.takeIf { pal.nickname.isNotBlank() && pal.nickname != pal.speciesName },
                pal.exp?.let { stringResource(R.string.pal_xp_fmt, formatThousands(it)) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = PalTheme.type.caption, color = c.textMuted)
        }
        if (pal.iv != null || pal.soul != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pal.iv?.let { iv ->
                    StatColumn(
                        title = stringResource(R.string.pal_info_iv_header),
                        stats = listOf(
                            stringResource(R.string.pal_stat_hp) to iv.health,
                            stringResource(R.string.pal_stat_atk) to iv.attack,
                            stringResource(R.string.pal_stat_def) to iv.defense,
                        ),
                        max = 100,
                        tone = c.green,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Âme : pas d'échelle native fixe — plafond visuel à 20 (valeur par défaut du
                // Créateur de Pal) ; au-delà, la jauge reste pleine.
                pal.soul?.let { soul ->
                    StatColumn(
                        title = stringResource(R.string.pal_info_soul_header),
                        stats = listOf(
                            stringResource(R.string.pal_stat_hp) to soul.health,
                            stringResource(R.string.pal_stat_atk) to soul.attack,
                            stringResource(R.string.pal_stat_def) to soul.defense,
                            stringResource(R.string.players_v2_stat_work_short) to soul.craftSpeed,
                        ),
                        max = 20,
                        tone = c.amber,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (pal.activeSkills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyLabel(stringResource(R.string.pal_info_active_skills))
                pal.activeSkills.forEach { skill -> ActiveSkillRow(skill) }
            }
        }
        if (pal.passives.isNotEmpty()) {
            var selected by remember { mutableStateOf<PassiveDisplay?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyLabel(stringResource(R.string.pal_info_passives))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pal.passives.forEach { passive -> PassiveChip(passive, onClick = { selected = passive }) }
                }
            }
            selected?.let { PassiveDescriptionDialog(it, onDismiss = { selected = null }) }
        }
    }
}

@Composable
private fun BodyLabel(text: String) {
    Text(text.uppercase(), style = PalTheme.type.caption.copy(letterSpacing = 0.6.sp), color = PalTheme.colors.textMuted)
}

@Composable
private fun StatColumn(title: String, stats: List<Pair<String, Int>>, max: Int, tone: Tone, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BodyLabel(title)
        stats.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = PalTheme.type.caption, color = c.textBody, maxLines = 1, modifier = Modifier.width(38.dp))
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(c.field)) {
                    Box(
                        Modifier
                            .fillMaxWidth(value.coerceIn(0, max) / max.toFloat())
                            .height(6.dp)
                            .background(tone.solid, RoundedCornerShape(6.dp)),
                    )
                }
                Text(
                    value.toString(),
                    style = PalTheme.type.mono.copy(fontSize = 12.sp),
                    color = c.text,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(26.dp),
                )
            }
        }
    }
}

/** Compétence active : élément (icône), nom, puissance et recharge. element/power/cooldown restent
 * absents quand la compétence n'a pas été trouvée dans ActiveSkillCatalog (repli sur le Fruit de
 * compétence ou l'id humanisé). */
@Composable
private fun ActiveSkillRow(skill: ActiveSkillDisplay) {
    val c = PalTheme.colors
    val elementLabel = skill.element?.let { PalLabels.elementLabel(it) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(c.field, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        skill.element?.let {
            FallbackAsyncImage(
                "file:///android_asset/images/elements/$it.webp",
                Modifier.size(18.dp).semantics { contentDescription = elementLabel.orEmpty() },
            )
        }
        Text(skill.name, style = PalTheme.type.body.copy(fontSize = 13.5.sp), color = c.text, modifier = Modifier.weight(1f))
        val meta = listOfNotNull(
            skill.power?.let { stringResource(R.string.players_v2_skill_power_short_fmt, it) },
            skill.cooldown?.let { stringResource(R.string.players_v2_skill_cooldown_short_fmt, it) },
        )
        if (meta.isNotEmpty()) {
            Text(meta.joinToString(" · "), style = PalTheme.type.mono.copy(fontSize = 11.5.sp), color = c.textMuted, maxLines = 1)
        }
    }
}

/** Famille de couleur d'un rang de passif : négatif = rouge, 1 (classique) = neutre, 2-3 (Épique)
 * = ambre, 4-5 (Légendaire) = bleu — même code que les rangs du jeu. null = rang inconnu. */
@Composable
private fun passiveTone(rank: Int?): Tone = with(PalTheme.colors) {
    when {
        rank == null -> neutral
        rank < 0 -> danger
        rank <= 1 -> neutral
        rank <= 3 -> amber
        else -> sky
    }
}

@Composable
private fun PassiveChip(passive: PassiveDisplay, onClick: () -> Unit) {
    val tone = passiveTone(passive.rank)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(min = 30.dp)
            .clip(CircleShape)
            .background(tone.container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
    ) {
        passive.rank?.let { RankChevrons(it, tone.onContainer, Modifier.size(14.dp)) }
        Text(passive.name, style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp), color = tone.onContainer, maxLines = 1)
    }
}

@Composable
private fun PassiveDescriptionDialog(passive: PassiveDisplay, onDismiss: () -> Unit) {
    val c = PalTheme.colors
    val tone = passiveTone(passive.rank)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceBar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                passive.rank?.let { RankChevrons(it, tone.content, Modifier.size(20.dp)) }
                Text(passive.name, style = PalTheme.type.cardTitle, color = c.text)
            }
        },
        text = {
            if (passive.descriptionLines.isEmpty()) {
                Text(stringResource(R.string.passive_no_description), style = PalTheme.type.body, color = c.textBody)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    passive.descriptionLines.forEach { line -> Text("• $line", style = PalTheme.type.body, color = c.textBody) }
                }
            }
        },
        confirmButton = {
            PalButton(stringResource(R.string.common_close), onClick = onDismiss, style = PalButtonStyle.Neutral, height = 44.dp)
        },
    )
}

/** Libellé de catégorie au-dessus, cadre de fond réservé au seul contenu — pour distinguer
 * visuellement le titre de bloc (Âme, compétences...) de ses valeurs. Public : même rendu
 * réutilisé par le Créateur de Pal pour une cohérence visuelle affichage/création. */
@Composable
fun PalInfoSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    val c = PalTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BodyLabel(label)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.field, RoundedCornerShape(12.dp))
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(10.dp),
            content = content,
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
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = color, modifier = modifier)
    } else {
        FallbackAsyncImage(
            model = "file:///android_asset/images/status/passive_rank_${rank.coerceIn(1, 5)}.webp",
            tint = color,
            modifier = modifier,
        )
    }
}
