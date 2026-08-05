package com.paladmin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R

/** Grille de Pals groupés (équipe joueur = un seul groupe sans en-tête ; camps de guilde = un
 * groupe par base) — cliquer sur un Pal ouvre son détail complet via [PalInfoDialog]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalGridDialog(state: PalGridUiState, onDismiss: () -> Unit) {
    var selectedPal by remember(state) { mutableStateOf<PalInfo?>(null) }
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
