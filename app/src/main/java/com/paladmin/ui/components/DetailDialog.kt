package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.theme.PalAmber
import com.paladmin.ui.theme.PalDanger
import com.paladmin.ui.theme.PalGreen
import com.paladmin.ui.theme.PalPurple
import com.paladmin.ui.theme.PalSky
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone
import com.paladmin.util.formatThousands

/** Aperçu affiché en tapant une case (nom complet + quantité + image). accent : reprend la couleur
 * de statut de la case tapée le cas échéant (ex. vert/rouge débloqué/verrouillé) plutôt que
 * toujours le même neutre. */
private data class SelectedPreview(val imagePath: String, val label: String, val accent: Color? = null)

/** Nombre de cases par ligne des grilles d'objets (maquette "Fiche joueur"). */
private const val GRID_COLUMNS = 5

/**
 * Feuille du bas commune aux vues de détail (fiche joueur, coffre/expéditions/labo de guilde,
 * Pals de camp) : fond "bar", coins 28 dp, poignée — même habillage que la feuille Alimentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = PalTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceBar,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp).background(c.neutral.solid, RoundedCornerShape(4.dp)))
        },
        content = content,
    )
}

/** Vue de détail générique (liste de [DetailRow] avec état de chargement/erreur) — réutilisée par
 * le coffre/expéditions/labo de guilde. Affichée en feuille du bas ([DetailSheet]). */
@Composable
fun DetailDialog(
    title: String,
    isLoading: Boolean,
    error: String?,
    rows: List<DetailRow>,
    onDismiss: () -> Unit,
) {
    DetailSheet(onDismiss = onDismiss) {
        DetailSheetContent(title = title, isLoading = isLoading, error = error, rows = rows)
    }
}

/** Contenu de [DetailDialog] sans la feuille (testable, réutilisable dans un autre conteneur). */
@Composable
fun DetailSheetContent(
    title: String,
    isLoading: Boolean,
    error: String?,
    rows: List<DetailRow>,
    modifier: Modifier = Modifier,
) {
    val c = PalTheme.colors
    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        Text(
            title,
            style = PalTheme.type.screenTitle,
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp).semantics { heading() },
        )
        DetailRowsList(isLoading = isLoading, error = error, rows = rows)
    }
}

/** Indicateur de chargement centré d'une vue de détail. */
@Composable
fun DetailLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PalTheme.colors.green.solid)
    }
}

/** Erreur de chargement d'une vue de détail, avec bouton Réessayer si [onRetry] est fourni. */
@Composable
fun DetailError(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val c = PalTheme.colors
    EmptyState(
        title = message,
        icon = Icons.Filled.ErrorOutline,
        tone = c.danger,
        modifier = modifier.fillMaxWidth(),
        action = onRetry?.let {
            { PalButton(stringResource(R.string.common_retry), onClick = it, style = PalButtonStyle.Neutral, height = 44.dp) }
        },
    )
}

/**
 * Rendu de la refonte pour une liste de [DetailRow] (liste paresseuse, à placer dans un conteneur
 * de hauteur bornée — feuille du bas, onglet) :
 * - [DetailRow.Section] suivie de [DetailRow.Stats] → une carte titrée de lignes clé/valeur ;
 * - [DetailRow.Section] avant une grille → titre + compteur ; sinon libellé en capitales ;
 * - [DetailRow.Grid] → grille de [InventoryTile] à 5 colonnes (tap = aperçu nom + quantité) ;
 * - [DetailRow.WithImage] → ligne vignette + libellé + état ;
 * - [DetailRow.Kpis] / [DetailRow.Progress] → tuiles de chiffres clés / carte à jauge.
 */
@Composable
fun DetailRowsList(
    isLoading: Boolean,
    error: String?,
    rows: List<DetailRow>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    onRetry: (() -> Unit)? = null,
) {
    when {
        isLoading -> DetailLoading(modifier)
        error != null -> DetailError(error, modifier, onRetry)
        rows.isEmpty() -> EmptyState(stringResource(R.string.players_v2_detail_empty), modifier = modifier.fillMaxWidth(), icon = Icons.Filled.Inbox)
        else -> {
            var selected by remember(rows) { mutableStateOf<SelectedPreview?>(null) }
            val blocks = remember(rows) { buildBlocks(rows) }
            Column(modifier = modifier) {
                selected?.let { preview ->
                    PreviewBar(preview, onClose = { selected = null }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                }
                LazyColumn(
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(blocks) { index, block ->
                        val topGap = if (index > 0 && block.startsGroup) 8.dp else 0.dp
                        Box(Modifier.padding(top = topGap)) {
                            BlockContent(block, onTileClick = { item ->
                                val label = item.quantity?.let { "${item.label} × ${formatThousands(it)}" } ?: item.label
                                selected = SelectedPreview(item.imagePath, label, item.borderColor)
                            })
                        }
                    }
                }
            }
        }
    }
}


/** Blocs d'affichage dérivés des lignes (regroupements Section+Stats, découpage des grilles). */
private sealed interface Block {
    val startsGroup: Boolean get() = false

    data class StatsCard(val section: DetailRow.Section?, val stats: DetailRow.Stats) : Block {
        override val startsGroup get() = true
    }
    data class Header(val section: DetailRow.Section, val compact: Boolean) : Block {
        override val startsGroup get() = true
    }
    data class GridLine(val items: List<InventoryGridItem>) : Block
    data class Single(val row: DetailRow) : Block
}

private fun buildBlocks(rows: List<DetailRow>): List<Block> {
    val out = mutableListOf<Block>()
    var i = 0
    while (i < rows.size) {
        val row = rows[i]
        val next = rows.getOrNull(i + 1)
        when {
            row is DetailRow.Section && next is DetailRow.Stats -> {
                out += Block.StatsCard(row, next)
                i += 2
                continue
            }
            row is DetailRow.Section -> out += Block.Header(row, compact = next !is DetailRow.Grid)
            row is DetailRow.Stats -> out += Block.StatsCard(null, row)
            row is DetailRow.Grid -> row.items.chunked(GRID_COLUMNS).forEach { out += Block.GridLine(it) }
            else -> out += Block.Single(row)
        }
        i++
    }
    return out
}

/** Famille de couleur d'une ligne : [tone] si fourni, sinon correspondance des anciennes couleurs
 * d'accent brutes (PalGreen, PalSky...) vers les familles du thème, pour rester lisible en clair. */
@Composable
fun resolveDetailTone(tone: ToneKey?, accent: Color?): Tone? {
    if (tone != null) return tone(tone)
    val c = PalTheme.colors
    return when (accent) {
        null -> null
        PalGreen -> c.green
        PalSky -> c.sky
        PalAmber -> c.amber
        PalDanger -> c.danger
        PalPurple, Color(0xFFA855F7) -> c.purple
        else -> c.neutral
    }
}

@Composable
private fun BlockContent(block: Block, onTileClick: (InventoryGridItem) -> Unit) {
    val c = PalTheme.colors
    when (block) {
        is Block.StatsCard -> StatsCard(block.section, block.stats)
        is Block.Header -> SectionHeader(block.section, block.compact)
        is Block.GridLine -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            block.items.forEach { item -> InventoryTile(item, onClick = { onTileClick(item) }, modifier = Modifier.weight(1f)) }
            repeat(GRID_COLUMNS - block.items.size) { Spacer(Modifier.weight(1f)) }
        }
        is Block.Single -> when (val row = block.row) {
            is DetailRow.Plain -> Text(
                row.text,
                style = PalTheme.type.body.copy(fontSize = 14.sp),
                color = resolveDetailTone(row.tone, row.accent)?.content ?: c.textBody,
            )
            is DetailRow.WithImage -> ImageRow(row)
            is DetailRow.Kpis -> KpiTiles(row)
            is DetailRow.Progress -> ProgressCard(row)
            else -> Unit
        }
    }
}

@Composable
private fun SectionHeader(section: DetailRow.Section, compact: Boolean) {
    val c = PalTheme.colors
    val tone = resolveDetailTone(section.tone, section.accent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    ) {
        section.icon?.let { Icon(it, contentDescription = null, tint = tone?.content ?: c.textMuted, modifier = Modifier.size(16.dp)) }
        if (compact) {
            Text(section.text.uppercase(), style = PalTheme.type.caption.copy(letterSpacing = 0.6.sp), color = c.textMuted, modifier = Modifier.weight(1f))
        } else {
            Text(section.text, style = PalTheme.type.sectionTitle, color = c.text, modifier = Modifier.weight(1f))
        }
        section.trailing?.let { Text(it, style = PalTheme.type.mono.copy(fontSize = 12.5.sp), color = c.textMuted) }
    }
}

@Composable
private fun StatsCard(section: DetailRow.Section?, stats: DetailRow.Stats) {
    val c = PalTheme.colors
    val tone = resolveDetailTone(section?.tone ?: stats.tone, section?.accent ?: stats.accent)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceRaised, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        if (section != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).semantics { heading() },
            ) {
                section.icon?.let { Icon(it, contentDescription = null, tint = tone?.content ?: c.textMuted, modifier = Modifier.size(15.dp)) }
                Text(section.text.uppercase(), style = PalTheme.type.caption.copy(letterSpacing = 0.6.sp), color = c.textMuted, modifier = Modifier.weight(1f))
                section.trailing?.let { Text(it, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textMuted) }
            }
        }
        stats.items.forEach { (label, value) ->
            PalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 8.dp),
            ) {
                Text(label, style = PalTheme.type.body.copy(fontSize = 14.sp), color = c.textSecondary, modifier = Modifier.weight(1f))
                Text(value, style = PalTheme.type.mono, color = c.text, maxLines = 1)
            }
        }
    }
}

@Composable
private fun KpiTiles(row: DetailRow.Kpis) {
    val c = PalTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        row.items.forEach { (label, value) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .background(c.surfaceRaised, RoundedCornerShape(14.dp))
                    .padding(10.dp),
            ) {
                Text(value, style = PalTheme.type.stat.copy(fontSize = 20.sp, lineHeight = 24.sp), color = c.text, maxLines = 1)
                Text(label, style = PalTheme.type.caption.copy(fontSize = 11.5.sp), color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ProgressCard(row: DetailRow.Progress) {
    val c = PalTheme.colors
    val tone = tone(row.tone)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceRaised, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, style = PalTheme.type.sectionTitle, color = c.text, modifier = Modifier.weight(1f))
            Text("${row.current}/${row.total}", style = PalTheme.type.mono, color = c.text)
        }
        val fraction = if (row.total > 0) row.current / row.total.toFloat() else 0f
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(c.field)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(tone.solid, RoundedCornerShape(6.dp)))
        }
    }
}

@Composable
private fun ImageRow(row: DetailRow.WithImage) {
    val c = PalTheme.colors
    val tint = row.tint?.let { tone(it) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(c.surfaceRaised, RoundedCornerShape(14.dp))
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint?.container ?: c.field)
                .then(if (tint != null) Modifier.border(1.dp, tint.border, RoundedCornerShape(10.dp)) else Modifier),
        ) {
            FallbackAsyncImage(model = row.imagePath, modifier = Modifier.size(34.dp))
        }
        Text(
            row.text,
            style = PalTheme.type.body.copy(fontSize = 14.sp),
            color = if (row.positive) c.text else c.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        row.status?.let {
            Text(
                it,
                style = PalTheme.type.label.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = if (row.positive) c.green.onContainer else c.textMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PreviewBar(preview: SelectedPreview, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val tone = resolveDetailTone(null, preview.accent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tone?.container ?: c.surfaceRaised)
            .border(1.dp, tone?.border ?: c.borderStrong, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.common_close), onClick = onClose)
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        FallbackAsyncImage(model = preview.imagePath, modifier = Modifier.size(32.dp))
        Text(preview.label, style = PalTheme.type.body.copy(fontSize = 14.sp), color = tone?.onContainer ?: c.text, modifier = Modifier.weight(1f))
    }
}
