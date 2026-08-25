package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paladmin.R

/** Aperçu affiché en tapant une entrée (nom complet + image). accent : reprend la couleur de statut
 * de la tuile taponnée le cas échéant (ex. vert/rouge débloqué/verrouillé d'une technologie) plutôt
 * que toujours le même ambre neutre — sinon l'aperçu d'une techno débloquée pouvait sembler signaler
 * un statut "verrouillé" par erreur alors qu'il ne fait qu'afficher le nom complet. */
private data class SelectedPreview(val imagePath: String, val label: String, val accent: Color? = null)

/** Boîte de dialogue générique pour afficher une liste de [DetailRow] avec état de chargement/erreur —
 * réutilisée par l'inventaire/équipe/progression joueur et le coffre/expéditions/labo de guilde. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailDialog(
    title: String,
    isLoading: Boolean,
    error: String?,
    rows: List<DetailRow>,
    onDismiss: () -> Unit,
) {
    var selectedInfo by remember(rows) { mutableStateOf<SelectedPreview?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                selectedInfo?.let { preview ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                preview.accent?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .let { m -> preview.accent?.let { m.border(1.dp, it.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) } ?: m }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        FallbackAsyncImage(model = preview.imagePath, modifier = Modifier.size(32.dp))
                        Text(
                            preview.label,
                            color = preview.accent ?: MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(top = if (selectedInfo != null) 8.dp else 0.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        error != null -> Text(error)
                        else -> rows.forEach { row ->
                            when (row) {
                                is DetailRow.Section -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                ) {
                                    row.icon?.let {
                                        Icon(
                                            it,
                                            contentDescription = null,
                                            tint = row.accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Text(
                                        row.text,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(start = if (row.icon != null) 6.dp else 0.dp),
                                    )
                                }
                                is DetailRow.WithImage -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedInfo = SelectedPreview(row.imagePath, row.text) }
                                        .padding(vertical = 2.dp),
                                ) {
                                    FallbackAsyncImage(model = row.imagePath, modifier = Modifier.size(28.dp))
                                    Text(row.text, modifier = Modifier.padding(start = 8.dp))
                                }
                                is DetailRow.Plain -> Text(
                                    row.text,
                                    color = row.accent ?: MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                                is DetailRow.Stats -> FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    row.items.forEach { (label, value) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(0.46f)
                                                .background(
                                                    row.accent?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(10.dp),
                                                )
                                                .let { m ->
                                                    row.accent?.let { m.border(1.dp, it.copy(alpha = 0.4f), RoundedCornerShape(10.dp)) } ?: m
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                value,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = row.accent ?: MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                                is DetailRow.Grid -> FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                ) {
                                    row.items.forEach { gridItem ->
                                        InventoryTile(
                                            gridItem,
                                            onClick = {
                                                val label = gridItem.quantity?.let { "${gridItem.label} × $it" } ?: gridItem.label
                                                selectedInfo = SelectedPreview(gridItem.imagePath, label, gridItem.borderColor)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}
