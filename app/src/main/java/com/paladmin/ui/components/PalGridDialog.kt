package com.paladmin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R

private const val PAGE_SIZE = 30

/** Grille de Pals groupés (équipe joueur = un seul groupe sans en-tête ; camps de guilde = un
 * groupe par base) — cliquer sur un Pal ouvre son détail complet via [PalInfoDialog]. Quand
 * [PalGridUiState.searchable] est activé (Palbox, potentiellement des centaines d'entrées), une
 * recherche et une pagination client remplacent l'affichage groupé. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalGridDialog(state: PalGridUiState, onDismiss: () -> Unit) {
    var selectedPal by remember(state) { mutableStateOf<PalInfo?>(null) }
    var query by remember(state) { mutableStateOf("") }
    var page by remember(state) { mutableIntStateOf(0) }
    LaunchedEffect(query) { page = 0 }

    val allPals = remember(state) { state.groups.flatMap { it.pals } }
    val filtered = remember(allPals, query) {
        if (query.isBlank()) {
            allPals
        } else {
            allPals.filter { it.speciesName.contains(query, ignoreCase = true) || it.nickname.contains(query, ignoreCase = true) }
        }
    }
    val pageCount = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    state.error != null -> Text(state.error)
                    state.groups.all { it.pals.isEmpty() } -> Text(
                        stringResource(R.string.pal_grid_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.searchable -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text(stringResource(R.string.pal_grid_search_label)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                        if (filtered.isEmpty()) {
                            Text(stringResource(R.string.pal_grid_no_match), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                filtered.subList(page * PAGE_SIZE, minOf((page + 1) * PAGE_SIZE, filtered.size))
                                    .forEach { pal -> PalTile(pal, onClick = { selectedPal = pal }) }
                            }
                            if (pageCount > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                ) {
                                    IconButton(onClick = { page-- }, enabled = page > 0) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pal_grid_prev_page))
                                    }
                                    Text(
                                        stringResource(R.string.pal_grid_page_fmt, page + 1, pageCount),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                    IconButton(onClick = { page++ }, enabled = page < pageCount - 1) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.pal_grid_next_page))
                                    }
                                }
                            }
                        }
                    }
                    else -> state.groups.forEach { group ->
                        if (group.pals.isEmpty()) return@forEach
                        group.label?.let {
                            Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            group.pals.forEach { pal -> PalTile(pal, onClick = { selectedPal = pal }) }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
    selectedPal?.let { pal -> PalInfoDialog(pal, onDismiss = { selectedPal = null }) }
}
