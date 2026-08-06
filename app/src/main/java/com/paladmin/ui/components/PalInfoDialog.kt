package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R

/** Détail complet d'un Pal (équipe joueur ou camp de guilde, même modèle [PalInfo] pour les deux). */
@Composable
fun PalInfoDialog(pal: PalInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FallbackAsyncImage(model = pal.imagePath, modifier = Modifier.size(48.dp))
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(pal.nickname.ifBlank { pal.speciesName })
                    Text(
                        buildString {
                            append(pal.speciesName)
                            append(" · Nv.${pal.level}")
                            if (pal.gender.isNotBlank()) append(" · ${pal.gender}")
                            if (pal.shiny) append(" · ✨")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pal.iv?.let {
                    PalInfoSection(stringResource(R.string.pal_info_iv_header)) {
                        Text(stringResource(R.string.pal_stat_health_fmt, it.health))
                        Text(stringResource(R.string.pal_stat_attack_fmt, it.attack))
                        Text(stringResource(R.string.pal_stat_defense_fmt, it.defense))
                    }
                }
                pal.soul?.let {
                    PalInfoSection(stringResource(R.string.pal_info_soul_header)) {
                        Text(stringResource(R.string.pal_stat_health_fmt, it.health))
                        Text(stringResource(R.string.pal_stat_attack_fmt, it.attack))
                        Text(stringResource(R.string.pal_stat_defense_fmt, it.defense))
                        Text(stringResource(R.string.pal_stat_work_fmt, it.craftSpeed))
                    }
                }
                PalSkillSection(stringResource(R.string.pal_info_active_skills), pal.activeSkills)
                PalSkillSection(stringResource(R.string.pal_info_passives), pal.passives)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

/** Libellé de catégorie hors du cadre, cadre de fond réservé au seul contenu — pour distinguer
 * visuellement le titre de bloc (IV, Âme, compétences...) de ses valeurs. Public : même rendu
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

/** [PalInfo.activeSkills]/[passives] arrivent déjà mis en forme (résolus via les catalogues
 * dédiés) — pas de retraitement ici. */
@Composable
private fun PalSkillSection(label: String, skills: List<String>) {
    if (skills.isEmpty()) return
    PalInfoSection(label) {
        skills.forEach { skill -> Text("• $skill", style = MaterialTheme.typography.bodyMedium) }
    }
}
