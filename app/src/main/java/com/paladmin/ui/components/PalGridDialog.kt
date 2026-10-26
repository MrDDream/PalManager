package com.paladmin.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme

private const val PAGE_SIZE = 24
private const val COLUMNS = 4

/** Grille de Pals groupés (camps de guilde = un groupe par base) en feuille du bas — taper un Pal
 * ouvre son détail complet via [PalInfoDialog]. Quand [PalGridUiState.searchable] est activé
 * (Palbox, potentiellement des centaines d'entrées), une recherche et une pagination client
 * remplacent l'affichage groupé. */
@Composable
fun PalGridDialog(state: PalGridUiState, onDismiss: () -> Unit) {
    var selectedPal by remember(state) { mutableStateOf<PalInfo?>(null) }
    DetailSheet(onDismiss = onDismiss) {
        PalGridSheetContent(state, onPalClick = { selectedPal = it })
    }
    selectedPal?.let { pal -> PalInfoDialog(pal, onDismiss = { selectedPal = null }) }
}

/** Contenu de [PalGridDialog] sans la feuille (titre + [PalGridContent]). */
@Composable
fun PalGridSheetContent(state: PalGridUiState, onPalClick: (PalInfo) -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        Text(
            state.title,
            style = PalTheme.type.screenTitle,
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp).semantics { heading() },
        )
        PalGridContent(
            groups = state.groups,
            searchable = state.searchable,
            isLoading = state.isLoading,
            error = state.error,
            onPalClick = onPalClick,
        )
    }
}

/**
 * Grille de Pals à 4 colonnes ([PalTile]) dans une liste paresseuse. [searchable] : champ de
 * recherche (espèce ou surnom) + pagination "Page x / y · N Pals" sur l'ensemble des groupes ;
 * sinon chaque groupe s'affiche sous son libellé.
 */
@Composable
fun PalGridContent(
    groups: List<PalGroup>,
    searchable: Boolean,
    isLoading: Boolean,
    error: String?,
    onPalClick: (PalInfo) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    onRetry: (() -> Unit)? = null,
) {
    val c = PalTheme.colors
    when {
        isLoading -> return DetailLoading(modifier)
        error != null -> return DetailError(error, modifier, onRetry)
        groups.all { it.pals.isEmpty() } -> return EmptyState(stringResource(R.string.pal_grid_empty), modifier = modifier.fillMaxWidth(), icon = Icons.Filled.Pets)
    }

    var query by remember(groups) { mutableStateOf("") }
    var page by remember(groups) { mutableIntStateOf(0) }
    val allPals = remember(groups) { groups.flatMap { it.pals } }
    val filtered = remember(allPals, query) {
        if (query.isBlank()) allPals
        else allPals.filter { it.speciesName.contains(query, ignoreCase = true) || it.nickname.contains(query, ignoreCase = true) }
    }
    val pageCount = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val safePage = page.coerceIn(0, pageCount - 1)

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (searchable) {
            item(key = "search") {
                PalSearchBar(
                    value = query,
                    onValueChange = { query = it; page = 0 },
                    placeholder = stringResource(R.string.pal_grid_search_label),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (filtered.isEmpty()) {
                item(key = "nomatch") { EmptyState(stringResource(R.string.pal_grid_no_match), icon = Icons.Filled.SearchOff, modifier = Modifier.fillMaxWidth()) }
            } else {
                val pagePals = filtered.subList(safePage * PAGE_SIZE, minOf((safePage + 1) * PAGE_SIZE, filtered.size))
                items(pagePals.chunked(COLUMNS)) { line -> TileLine(line, onPalClick) }
                item(key = "pager") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        PagerButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            description = stringResource(R.string.pal_grid_prev_page),
                            enabled = safePage > 0,
                            onClick = { page = safePage - 1 },
                        )
                        Text(
                            stringResource(R.string.pal_grid_page_fmt, safePage + 1, pageCount) + " · " +
                                pluralStringResource(R.plurals.players_v2_pal_count, filtered.size, filtered.size),
                            style = PalTheme.type.bodySmall,
                            color = c.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        PagerButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            description = stringResource(R.string.pal_grid_next_page),
                            enabled = safePage < pageCount - 1,
                            onClick = { page = safePage + 1 },
                        )
                    }
                }
            }
        } else {
            groups.forEachIndexed { index, group ->
                if (group.pals.isEmpty()) return@forEachIndexed
                group.label?.let { label ->
                    item(key = "label_$index") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = if (index > 0) 10.dp else 0.dp, bottom = 2.dp).semantics { heading() },
                        ) {
                            Text(label, style = PalTheme.type.sectionTitle, color = c.text, modifier = Modifier.weight(1f))
                            Text(pluralStringResource(R.plurals.players_v2_pal_count, group.pals.size, group.pals.size), style = PalTheme.type.mono.copy(fontSize = PalTheme.type.caption.fontSize), color = c.textMuted)
                        }
                    }
                }
                items(group.pals.chunked(COLUMNS)) { line -> TileLine(line, onPalClick) }
            }
        }
    }
}

@Composable
private fun TileLine(line: List<PalInfo>, onPalClick: (PalInfo) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        line.forEach { pal -> PalTile(pal, onClick = { onPalClick(pal) }, modifier = Modifier.weight(1f)) }
        repeat(COLUMNS - line.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun PagerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    PalIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        tint = c.text,
        modifier = Modifier.border(1.dp, c.borderStrong, RoundedCornerShape(12.dp)),
    )
}
