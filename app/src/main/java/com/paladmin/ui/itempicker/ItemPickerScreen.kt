package com.paladmin.ui.itempicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Paragliding
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.paladmin.R
import com.paladmin.data.local.db.ItemEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.FilterPanel
import com.paladmin.ui.components.PlayerSearchField
import com.paladmin.ui.components.SearchField
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val statsJson = Json { ignoreUnknownKeys = true }

private val CATEGORY_ICONS = mapOf(
    "Weapon" to Icons.Filled.GpsFixed,
    "Sphere" to Icons.Filled.Circle,
    "Sphere_Module" to Icons.Filled.AutoAwesome,
    "Armor" to Icons.Filled.Shield,
    "Accessory" to Icons.Filled.Diamond,
    "Material" to Icons.Filled.Construction,
    "Consumable" to Icons.Filled.Restaurant,
    "Ammo" to Icons.Filled.Adjust,
    "Ingredient" to Icons.Filled.Grass,
    "Key_Items" to Icons.Filled.VpnKey,
    "Glider" to Icons.Filled.Paragliding,
    "Schematic" to Icons.Filled.Description,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemPickerScreen(
    onBack: () -> Unit,
    viewModel: ItemPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.itempicker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = stringResource(R.string.common_search_hint),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )

            FilterPanel(
                activeCount = if (selectedCategory != null) 1 else 0,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text(stringResource(R.string.filter_all_fem)) },
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category) },
                            leadingIcon = {
                                CATEGORY_ICONS[category]?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            },
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.id }) { item ->
                    ItemCard(item = item, onClick = { selectedItem = item })
                }
            }
        }
    }

    selectedItem?.let { item ->
        ItemDetailDialog(
            item = item,
            players = players,
            onDismiss = { selectedItem = null },
            onConfirmGive = { playerIdentifier, amount ->
                viewModel.giveItem(item, playerIdentifier, amount)
                selectedItem = null
            },
        )
    }
}

@Composable
private fun ItemCard(item: ItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = "file:///android_asset/images/items/${item.image}",
                    contentDescription = item.nameFr,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = item.nameFr.ifBlank { item.id },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailDialog(
    item: ItemEntity,
    players: List<PalworldPlayer>,
    onDismiss: () -> Unit,
    onConfirmGive: (playerIdentifier: String, amount: Int) -> Unit,
) {
    var amount by remember { mutableStateOf("1") }
    var selectedPlayer by remember { mutableStateOf<PalworldPlayer?>(null) }

    val stats = remember(item.statsJson) {
        runCatching { statsJson.decodeFromString<Map<String, String>>(item.statsJson) }.getOrDefault(emptyMap())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.nameFr.ifBlank { item.id }) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.8f)) {
                    AsyncImage(
                        model = "file:///android_asset/images/items/${item.image}",
                        contentDescription = item.nameFr,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (item.description.isNotBlank()) {
                    Text(item.description, modifier = Modifier.padding(top = 8.dp))
                }
                if (stats.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        stats.forEach { (label, value) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(label, modifier = Modifier.weight(1f))
                                Text(value)
                            }
                        }
                    }
                }
                Text(stringResource(R.string.common_id_fmt, item.id), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                PlayerSearchField(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = { selectedPlayer = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.item_detail_quantity_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedPlayer?.let { onConfirmGive(it.userId, amount.toIntOrNull() ?: 1) }
            }) { Text(stringResource(R.string.common_give)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
