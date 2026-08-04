package com.paladmin.ui.palpicker

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.paladmin.data.local.dataset.PalPartnerSkillDto
import com.paladmin.data.local.dataset.PalStatsDto
import com.paladmin.data.local.dataset.PalWorkSuitabilityDto
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.FilterPanel
import com.paladmin.ui.components.PlayerSearchField
import com.paladmin.ui.components.SearchField
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val palJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PalPickerScreen(
    onBack: () -> Unit,
    viewModel: PalPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val selectedRarityTier by viewModel.selectedRarityTier.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPal by remember { mutableStateOf<PalEntity?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.palpicker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    OutlinedButton(onClick = { showTemplateDialog = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.palpicker_template_button))
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
                activeCount = listOfNotNull(selectedElement, selectedRarityTier, selectedJob).size,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Text(stringResource(R.string.filter_element_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedElement == null,
                        onClick = { viewModel.onElementSelected(null) },
                        label = { Text(stringResource(R.string.filter_all_masc)) },
                    )
                    PalLabels.ELEMENTS.forEach { element ->
                        FilterChip(
                            selected = selectedElement == element,
                            onClick = { viewModel.onElementSelected(element) },
                            label = { Text(PalLabels.elementLabel(element)) },
                            leadingIcon = {
                                AsyncImage(
                                    model = "file:///android_asset/images/elements/$element.webp",
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }

                Text(stringResource(R.string.filter_rarity_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedRarityTier == null,
                        onClick = { viewModel.onRarityTierSelected(null) },
                        label = { Text(stringResource(R.string.filter_all_fem)) },
                    )
                    PalLabels.RARITY_TIERS.forEach { tier ->
                        FilterChip(
                            selected = selectedRarityTier == tier,
                            onClick = { viewModel.onRarityTierSelected(tier) },
                            label = { Text(stringResource(tier.labelRes)) },
                            leadingIcon = {
                                Box(modifier = Modifier.size(10.dp).background(tier.color, CircleShape))
                            },
                        )
                    }
                }

                Text(stringResource(R.string.filter_job_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedJob == null,
                        onClick = { viewModel.onJobSelected(null) },
                        label = { Text(stringResource(R.string.filter_all_masc)) },
                    )
                    PalLabels.FILTERABLE_JOBS.forEach { job ->
                        FilterChip(
                            selected = selectedJob == job,
                            onClick = { viewModel.onJobSelected(job) },
                            label = { Text(PalLabels.jobLabel(job)) },
                            leadingIcon = {
                                AsyncImage(
                                    model = "file:///android_asset/images/jobs/$job.webp",
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
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
                items(results, key = { it.id }) { pal ->
                    PalCard(pal = pal, onClick = { selectedPal = pal })
                }
            }
        }
    }

    selectedPal?.let { pal ->
        PalDetailDialog(
            pal = pal,
            players = players,
            onDismiss = { selectedPal = null },
            onConfirm = { playerIdentifier, level ->
                viewModel.givePal(pal, playerIdentifier, level)
                selectedPal = null
            },
        )
    }

    if (showTemplateDialog) {
        GivePalTemplateDialog(
            players = players,
            onDismiss = { showTemplateDialog = false },
            onConfirm = { templateName, playerIdentifier ->
                viewModel.givePalTemplate(templateName, playerIdentifier)
                showTemplateDialog = false
            },
        )
    }
}

@Composable
private fun PalCard(pal: PalEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = "file:///android_asset/images/pals/${pal.image}",
                    contentDescription = pal.nameFr,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = pal.nameFr.ifBlank { pal.id },
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
private fun PalDetailDialog(
    pal: PalEntity,
    players: List<PalworldPlayer>,
    onDismiss: () -> Unit,
    onConfirm: (playerIdentifier: String, level: Int) -> Unit,
) {
    var level by remember { mutableStateOf("1") }
    var selectedPlayer by remember { mutableStateOf<PalworldPlayer?>(null) }

    val stats = remember(pal.statsJson) {
        runCatching { palJson.decodeFromString<PalStatsDto?>(pal.statsJson) }.getOrNull()
    }
    val jobs = remember(pal.workSuitabilitiesJson) {
        runCatching { palJson.decodeFromString<List<PalWorkSuitabilityDto>>(pal.workSuitabilitiesJson) }.getOrDefault(emptyList())
    }
    val skill = remember(pal.partnerSkillJson) {
        runCatching { palJson.decodeFromString<PalPartnerSkillDto?>(pal.partnerSkillJson) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pal.nameFr.ifBlank { pal.id }) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.8f)) {
                    AsyncImage(
                        model = "file:///android_asset/images/pals/${pal.image}",
                        contentDescription = pal.nameFr,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    ElementBadge(pal.element1)
                    pal.element2?.let { ElementBadge(it) }
                    RarityBadge(pal.rarity)
                }

                stats?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        StatChip(stringResource(R.string.pal_stat_hp), it.hp, Modifier.weight(1f))
                        StatChip(stringResource(R.string.pal_stat_atk), it.attack, Modifier.weight(1f))
                        StatChip(stringResource(R.string.pal_stat_def), it.defense, Modifier.weight(1f))
                    }
                }

                skill?.let { PartnerSkillBlock(it) }

                if (jobs.isNotEmpty()) {
                    Text(stringResource(R.string.pal_jobs_header), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    FlowRowCompat {
                        jobs.forEach { job ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 6.dp, bottom = 6.dp)
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

                Text(stringResource(R.string.common_id_fmt, pal.id), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow { content() }
}

@Composable
private fun ElementBadge(element: String) {
    val color = PalLabels.ELEMENT_COLORS[element] ?: MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        AsyncImage(
            model = "file:///android_asset/images/elements/$element.webp",
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            PalLabels.elementLabel(element),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun RarityBadge(rarity: Int) {
    val tier = PalLabels.rarityTier(rarity)
    Text(
        stringResource(tier.labelRes),
        color = tier.color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(tier.color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
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

@Composable
private fun PartnerSkillBlock(skill: PalPartnerSkillDto, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(stringResource(R.string.pal_partner_skill_header), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Text(skill.name, style = MaterialTheme.typography.titleSmall)
            Text(
                skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GivePalTemplateDialog(
    players: List<PalworldPlayer>,
    onDismiss: () -> Unit,
    onConfirm: (templateName: String, playerIdentifier: String) -> Unit,
) {
    var templateName by remember { mutableStateOf("") }
    var selectedPlayer by remember { mutableStateOf<PalworldPlayer?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pal_template_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text(stringResource(R.string.pal_template_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                PlayerSearchField(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = { selectedPlayer = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val player = selectedPlayer
                if (templateName.isNotBlank() && player != null) {
                    onConfirm(templateName.trim(), player.userId)
                }
            }) { Text(stringResource(R.string.common_give)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
