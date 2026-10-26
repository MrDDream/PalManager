package com.paladmin.ui.humanpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.dataset.HumanDropDto
import com.paladmin.data.local.dataset.PalStatsDto
import com.paladmin.data.local.dataset.PalWorkSuitabilityDto
import com.paladmin.data.local.db.HumanEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.GiveDetailHeader
import com.paladmin.ui.components.GiveSheet
import com.paladmin.ui.components.GiveStepper
import com.paladmin.ui.components.GiveThumb
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.PlayerPickerSheet
import com.paladmin.ui.components.PlayerTargetChip
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.components.StatTile
import com.paladmin.ui.components.resolveGiveTarget
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.ui.theme.PalTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val humanJson = Json { ignoreUnknownKeys = true }

private fun humanImage(human: HumanEntity) = "file:///android_asset/images/humans/${human.image}"
private fun HumanEntity.displayName() = nameFr.ifBlank { id }

private class HumanDetails(val stats: PalStatsDto?, val jobs: List<PalWorkSuitabilityDto>, val drops: List<HumanDropDto>)

@Composable
private fun rememberHumanDetails(human: HumanEntity): HumanDetails = remember(human.id) {
    HumanDetails(
        stats = runCatching { humanJson.decodeFromString<PalStatsDto?>(human.statsJson) }.getOrNull(),
        jobs = runCatching { humanJson.decodeFromString<List<PalWorkSuitabilityDto>>(human.workSuitabilitiesJson) }.getOrDefault(emptyList()).filter { it.level > 0 },
        drops = runCatching { humanJson.decodeFromString<List<HumanDropDto>>(human.dropsJson) }.getOrDefault(emptyList()),
    )
}

/** Butins d'un PNJ (décodés à la demande — utile pour résoudre leurs icônes). */
fun humanDrops(human: HumanEntity): List<HumanDropDto> =
    runCatching { humanJson.decodeFromString<List<HumanDropDto>>(human.dropsJson) }.getOrDefault(emptyList())

data class HumanPickerUiState(
    val query: String = "",
    val results: List<HumanEntity> = emptyList(),
    val target: PalworldPlayer? = null,
    val selectedHuman: HumanEntity? = null,
    val level: Int = 1,
    /** itemId de butin -> fichier image (assets/images/items). */
    val dropImages: Map<String, String> = emptyMap(),
)

data class HumanPickerActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSelectHuman: (HumanEntity) -> Unit = {},
    val onLevelChange: (Int) -> Unit = {},
    val onOpenPlayerPicker: () -> Unit = {},
    val onGive: () -> Unit = {},
    val onShowDetails: () -> Unit = {},
)

@Composable
fun HumanPickerScreen(
    onBack: () -> Unit,
    viewModel: HumanPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val dropImages by viewModel.dropImages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedHuman by remember { mutableStateOf<HumanEntity?>(null) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var targetUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val target = resolveGiveTarget(players, targetUserId)

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }
    LaunchedEffect(selectedHuman?.id) {
        selectedHuman?.let { viewModel.resolveDropImages(humanDrops(it)) }
    }

    HumanPickerContent(
        state = HumanPickerUiState(
            query = query,
            results = results,
            target = target,
            selectedHuman = selectedHuman,
            level = level,
            dropImages = dropImages,
        ),
        actions = HumanPickerActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onSelectHuman = { selectedHuman = it },
            onLevelChange = { level = it.coerceAtLeast(1) },
            onOpenPlayerPicker = { showPlayerPicker = true; viewModel.loadPlayers() },
            onGive = {
                val human = selectedHuman
                when {
                    human == null -> Unit
                    target == null -> { showPlayerPicker = true; viewModel.loadPlayers() }
                    else -> viewModel.giveHuman(human, target.userId, level)
                }
            },
            onShowDetails = { showDetails = true },
        ),
        snackbarHostState = snackbarHostState,
    )

    if (showPlayerPicker) {
        PlayerPickerSheet(
            players = players,
            selectedUserId = target?.userId,
            onSelect = { targetUserId = it.userId },
            onRefresh = viewModel::loadPlayers,
            onDismiss = { showPlayerPicker = false },
        )
    }

    val detailHuman = selectedHuman
    if (showDetails && detailHuman != null) {
        GiveSheet(onDismiss = { showDetails = false }) { HumanDetailContent(detailHuman, dropImages) }
    }
}

@Composable
fun HumanPickerContent(
    state: HumanPickerUiState,
    actions: HumanPickerActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(R.string.humanpicker_title),
                onBack = actions.onBack,
                actions = { PlayerTargetChip(state.target, actions.onOpenPlayerPicker, Modifier.padding(end = 8.dp)) },
            )
        },
        bottomBar = { HumanGiveBar(state, actions) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PalSearchBar(
                value = state.query,
                onValueChange = actions.onQueryChange,
                placeholder = stringResource(R.string.common_search_hint),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
            if (state.results.isEmpty() && state.query.isNotBlank()) {
                EmptyState(
                    title = stringResource(R.string.give_empty_results_title),
                    message = stringResource(R.string.give_empty_results_message),
                    icon = Icons.Filled.SearchOff,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { human ->
                        HumanRow(
                            human = human,
                            selected = human.id == state.selectedHuman?.id,
                            onClick = { actions.onSelectHuman(human) },
                            onShowDetails = actions.onShowDetails,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HumanRow(human: HumanEntity, selected: Boolean, onClick: () -> Unit, onShowDetails: () -> Unit) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val stats = rememberHumanDetails(human).stats
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.sky.solid.copy(alpha = 0.10f).compositeOver(c.surface) else c.surface)
            .border(2.dp, if (selected) c.sky.solid else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 8.dp, end = if (selected) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        GiveThumb(humanImage(human), c.sky.container, size = 56.dp, imageSize = 54.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                human.displayName(),
                style = PalTheme.type.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
                color = c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (stats != null) {
                Text(
                    stringResource(R.string.give_stats_fmt, stats.hp, stats.attack, stats.defense).replace(" · ", "  ·  "),
                    style = PalTheme.type.mono.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                    color = c.textMuted,
                    maxLines = 1,
                )
            }
        }
        if (selected) {
            PalIconButton(Icons.Outlined.Info, stringResource(R.string.give_details_cd), onClick = onShowDetails, tint = c.sky.content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HumanGiveBar(state: HumanPickerUiState, actions: HumanPickerActions) {
    val c = PalTheme.colors
    val human = state.selectedHuman
    BottomActionBar {
        if (human != null) {
            val drops = rememberHumanDetails(human).drops
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.human_drops_header), color = c.textMuted)
                if (drops.isEmpty()) {
                    Text(stringResource(R.string.give_npc_no_drops), style = PalTheme.type.bodySmall, color = c.textMuted)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxLines = 2) {
                        drops.forEach { DropChip(it, state.dropImages[it.itemId]) }
                    }
                }
            }
        } else {
            Text(stringResource(R.string.give_npc_none_selected), style = PalTheme.type.bodySmall, color = c.textMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(stringResource(R.string.pal_level_label), modifier = Modifier.weight(1f))
            GiveStepper(
                value = state.level,
                onValueChange = actions.onLevelChange,
                decreaseCd = stringResource(R.string.give_level_down_cd),
                increaseCd = stringResource(R.string.give_level_up_cd),
                groupCd = stringResource(R.string.pal_level_label),
                valueStyle = PalTheme.type.cardTitle.copy(fontSize = 16.sp),
            )
        }
        PalButton(
            text = when {
                human == null -> stringResource(R.string.common_give)
                state.target == null -> stringResource(R.string.give_choose_player_cta)
                else -> stringResource(R.string.give_npc_cta_fmt, state.target.name)
            },
            onClick = actions.onGive,
            enabled = human != null,
            tone = c.sky,
            height = 52.dp,
            shape = RoundedCornerShape(16.dp),
            textStyle = PalTheme.type.button.copy(fontSize = 15.5.sp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun dropQuantity(drop: HumanDropDto) =
    if (drop.maxQuantity > drop.minQuantity) "${drop.minQuantity}–${drop.maxQuantity}" else "${drop.minQuantity}"

@Composable
private fun DropChip(drop: HumanDropDto, image: String?) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(c.surfaceRaised, CircleShape)
            .padding(start = if (image != null) 4.dp else 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (image != null) FallbackAsyncImage("file:///android_asset/images/items/$image", Modifier.size(22.dp))
        Text(
            stringResource(R.string.give_npc_drop_fmt, drop.name.ifBlank { drop.itemId }, dropQuantity(drop), drop.probability),
            style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
            color = c.text,
            maxLines = 1,
        )
    }
}

/** Fiche détail d'un PNJ (feuille du bas) : stats, métiers, butin complet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HumanDetailContent(human: HumanEntity, dropImages: Map<String, String> = emptyMap()) {
    val c = PalTheme.colors
    val details = rememberHumanDetails(human)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
    ) {
        GiveDetailHeader(humanImage(human), c.sky.container, human.displayName(), human.id)
        details.stats?.let { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(stringResource(R.string.pal_stat_hp), s.hp.toString(), c.green, Modifier.weight(1f))
                StatTile(stringResource(R.string.pal_stat_atk), s.attack.toString(), c.amber, Modifier.weight(1f))
                StatTile(stringResource(R.string.pal_stat_def), s.defense.toString(), c.sky, Modifier.weight(1f))
            }
        }
        if (details.jobs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.pal_jobs_header))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.jobs.forEach { job ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(c.surfaceRaised, CircleShape).padding(start = 3.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                        ) {
                            FallbackAsyncImage("file:///android_asset/images/jobs/${job.job}.webp", Modifier.size(22.dp))
                            Text(stringResource(R.string.pal_job_level_fmt, PalLabels.jobLabel(job.job), job.level), style = PalTheme.type.label.copy(fontSize = 12.5.sp), color = c.text)
                        }
                    }
                }
            }
        }
        if (details.drops.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.human_drops_header))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.drops.forEach { DropChip(it, dropImages[it.itemId]) }
                }
            }
        }
    }
}
