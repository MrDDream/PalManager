package com.paladmin.ui.humanpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.paladmin.R
import com.paladmin.data.local.dataset.HumanDropDto
import com.paladmin.data.local.dataset.PalStatsDto
import com.paladmin.data.local.dataset.PalWorkSuitabilityDto
import com.paladmin.data.local.db.HumanEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.PlayerSearchField
import com.paladmin.ui.components.SearchField
import com.paladmin.ui.palpicker.PalLabels
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val humanJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HumanPickerScreen(
    onBack: () -> Unit,
    viewModel: HumanPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedHuman by remember { mutableStateOf<HumanEntity?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.humanpicker_title)) },
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.id }) { human ->
                    HumanCard(human = human, onClick = { selectedHuman = human })
                }
            }
        }
    }

    selectedHuman?.let { human ->
        GiveHumanDialog(
            human = human,
            players = players,
            onDismiss = { selectedHuman = null },
            onConfirm = { playerIdentifier, level ->
                viewModel.giveHuman(human, playerIdentifier, level)
                selectedHuman = null
            },
        )
    }
}

@Composable
private fun HumanCard(human: HumanEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = "file:///android_asset/images/humans/${human.image}",
                    contentDescription = human.nameFr,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = human.nameFr.ifBlank { human.id },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GiveHumanDialog(
    human: HumanEntity,
    players: List<PalworldPlayer>,
    onDismiss: () -> Unit,
    onConfirm: (playerIdentifier: String, level: Int) -> Unit,
) {
    var level by remember { mutableStateOf("1") }
    var selectedPlayer by remember { mutableStateOf<PalworldPlayer?>(null) }

    val stats = remember(human.statsJson) {
        runCatching { humanJson.decodeFromString<PalStatsDto?>(human.statsJson) }.getOrNull()
    }
    val jobs = remember(human.workSuitabilitiesJson) {
        runCatching { humanJson.decodeFromString<List<PalWorkSuitabilityDto>>(human.workSuitabilitiesJson) }.getOrDefault(emptyList())
    }
    val drops = remember(human.dropsJson) {
        runCatching { humanJson.decodeFromString<List<HumanDropDto>>(human.dropsJson) }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(human.nameFr.ifBlank { human.id }) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.8f)) {
                    AsyncImage(
                        model = "file:///android_asset/images/humans/${human.image}",
                        contentDescription = human.nameFr,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                stats?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        HumanStatChip(stringResource(R.string.pal_stat_hp), it.hp, Modifier.weight(1f))
                        HumanStatChip(stringResource(R.string.pal_stat_atk), it.attack, Modifier.weight(1f))
                        HumanStatChip(stringResource(R.string.pal_stat_def), it.defense, Modifier.weight(1f))
                    }
                }

                if (jobs.isNotEmpty()) {
                    Text(stringResource(R.string.pal_jobs_header), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        jobs.forEach { job ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                AsyncImage(
                                    model = "file:///android_asset/images/jobs/${job.job}.webp",
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    stringResource(R.string.pal_job_level_fmt, PalLabels.jobLabel(job.job), job.level),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }

                if (drops.isNotEmpty()) {
                    Text(stringResource(R.string.human_drops_header), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    drops.forEach { drop ->
                        val quantity = if (drop.maxQuantity > drop.minQuantity) "${drop.minQuantity}–${drop.maxQuantity}" else "${drop.minQuantity}"
                        Text(
                            stringResource(R.string.human_drop_line_fmt, drop.name.ifBlank { drop.itemId }, quantity, drop.probability),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text(stringResource(R.string.common_id_fmt, human.id), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

                PlayerSearchField(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = { selectedPlayer = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(R.string.pal_level_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedPlayer?.let { onConfirm(it.userId, level.toIntOrNull() ?: 1) }
            }) { Text(stringResource(R.string.common_give)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun HumanStatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
