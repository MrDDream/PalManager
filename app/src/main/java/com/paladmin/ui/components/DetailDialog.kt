package com.paladmin.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R

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
    var selectedInfo by remember(rows) { mutableStateOf<Pair<String, String>?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                selectedInfo?.let { (imagePath, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        FallbackAsyncImage(model = imagePath, modifier = Modifier.size(32.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 8.dp))
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
                                is DetailRow.Section -> Text(
                                    row.text,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                )
                                is DetailRow.WithImage -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedInfo = row.imagePath to row.text }
                                        .padding(vertical = 2.dp),
                                ) {
                                    FallbackAsyncImage(model = row.imagePath, modifier = Modifier.size(28.dp))
                                    Text(row.text, modifier = Modifier.padding(start = 8.dp))
                                }
                                is DetailRow.Plain -> Text(row.text, modifier = Modifier.padding(vertical = 2.dp))
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
                                                selectedInfo = gridItem.imagePath to label
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
