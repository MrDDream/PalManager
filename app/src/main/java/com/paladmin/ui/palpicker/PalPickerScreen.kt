package com.paladmin.ui.palpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.dataset.PalPartnerSkillDto
import com.paladmin.data.local.dataset.PalStatsDto
import com.paladmin.data.local.dataset.PalWorkSuitabilityDto
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.CardTitle
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.GiveDetailHeader
import com.paladmin.ui.components.GiveDetailSection
import com.paladmin.ui.components.GiveSheet
import com.paladmin.ui.components.GiveStepper
import com.paladmin.ui.components.GiveThumb
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalChip
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.PlayerPickerSheet
import com.paladmin.ui.components.PlayerSearchField
import com.paladmin.ui.components.PlayerTargetChip
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.components.StatTile
import com.paladmin.ui.components.giveFieldColors
import com.paladmin.ui.components.resolveGiveTarget
import com.paladmin.ui.theme.PalTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val palJson = Json { ignoreUnknownKeys = true }

/** Ordre des pastilles d'élément de la maquette (Neutre en premier). */
private val ELEMENT_FILTER_ORDER = listOf("Normal", "Fire", "Water", "Leaf", "Electricity", "Ice", "Earth", "Dark", "Dragon")

private fun palImage(pal: PalEntity) = "file:///android_asset/images/pals/${pal.image}"
private fun elementImage(element: String) = "file:///android_asset/images/elements/$element.webp"
private fun jobImage(job: String) = "file:///android_asset/images/jobs/$job.webp"
private fun PalEntity.displayName() = nameFr.ifBlank { id }

/** Fond teinté d'une vignette de Pal selon son élément (lisible en sombre comme en clair). */
@Composable
fun palElementTint(element: String): Color {
    val c = PalTheme.colors
    val accent = PalLabels.ELEMENT_COLORS[element] ?: c.neutral.solid
    return accent.copy(alpha = if (c.isDark) 0.16f else 0.18f).compositeOver(c.surfaceRaised)
}

private class PalDetails(val stats: PalStatsDto?, val jobs: List<PalWorkSuitabilityDto>, val skill: PalPartnerSkillDto?)

@Composable
private fun rememberPalDetails(pal: PalEntity): PalDetails = remember(pal.id) {
    PalDetails(
        stats = runCatching { palJson.decodeFromString<PalStatsDto?>(pal.statsJson) }.getOrNull(),
        jobs = runCatching { palJson.decodeFromString<List<PalWorkSuitabilityDto>>(pal.workSuitabilitiesJson) }.getOrDefault(emptyList()).filter { it.level > 0 },
        skill = runCatching { palJson.decodeFromString<PalPartnerSkillDto?>(pal.partnerSkillJson) }.getOrNull(),
    )
}

data class PalPickerUiState(
    val query: String = "",
    val selectedElement: String? = null,
    val selectedRarityTier: PalLabels.RarityTier? = null,
    val selectedJob: String? = null,
    val results: List<PalEntity> = emptyList(),
    val target: PalworldPlayer? = null,
    val selectedPal: PalEntity? = null,
    val level: Int = 1,
)

data class PalPickerActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onElementSelected: (String?) -> Unit = {},
    val onOpenFilters: () -> Unit = {},
    val onSelectPal: (PalEntity) -> Unit = {},
    val onLevelChange: (Int) -> Unit = {},
    val onOpenPlayerPicker: () -> Unit = {},
    val onGive: () -> Unit = {},
    val onShowDetails: () -> Unit = {},
    val onOpenTemplate: () -> Unit = {},
)

@Composable
fun PalPickerScreen(
    onBack: () -> Unit,
    onOpenPalCreator: (() -> Unit)? = null,
    viewModel: PalPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val selectedRarityTier by viewModel.selectedRarityTier.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSftpConfigured by viewModel.isSftpConfigured.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPal by remember { mutableStateOf<PalEntity?>(null) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var targetUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    val target = resolveGiveTarget(players, targetUserId)

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    PalPickerContent(
        state = PalPickerUiState(
            query = query,
            selectedElement = selectedElement,
            selectedRarityTier = selectedRarityTier,
            selectedJob = selectedJob,
            results = results,
            target = target,
            selectedPal = selectedPal,
            level = level,
        ),
        actions = PalPickerActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onElementSelected = viewModel::onElementSelected,
            onOpenFilters = { showFilters = true },
            onSelectPal = { selectedPal = it },
            onLevelChange = { level = it.coerceAtLeast(1) },
            onOpenPlayerPicker = { showPlayerPicker = true; viewModel.loadPlayers() },
            onGive = {
                val pal = selectedPal
                when {
                    pal == null -> Unit
                    target == null -> { showPlayerPicker = true; viewModel.loadPlayers() }
                    else -> viewModel.givePal(pal, target.userId, level)
                }
            },
            onShowDetails = { showDetails = true },
            onOpenTemplate = { showTemplateDialog = true; viewModel.loadTemplates() },
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

    if (showFilters) {
        GiveSheet(onDismiss = { showFilters = false }) {
            PalFilterSheetContent(
                selectedRarityTier = selectedRarityTier,
                selectedJob = selectedJob,
                resultCount = results.size,
                onRarityTierSelected = viewModel::onRarityTierSelected,
                onJobSelected = viewModel::onJobSelected,
                onReset = { viewModel.onRarityTierSelected(null); viewModel.onJobSelected(null) },
                onDone = { showFilters = false },
            )
        }
    }

    val detailPal = selectedPal
    if (showDetails && detailPal != null) {
        GiveSheet(onDismiss = { showDetails = false }) { PalDetailContent(detailPal) }
    }

    if (showTemplateDialog) {
        val templates by viewModel.templates.collectAsState()
        val templatesLoading by viewModel.templatesLoading.collectAsState()
        GivePalTemplateDialog(
            players = players,
            initialPlayer = target,
            isSftpConfigured = isSftpConfigured,
            templates = templates,
            templatesLoading = templatesLoading,
            onOpenPalCreator = onOpenPalCreator?.takeIf { isSftpConfigured }?.let { open -> { showTemplateDialog = false; open() } },
            onDismiss = { showTemplateDialog = false },
            onConfirm = { templateName, player ->
                targetUserId = player.userId
                viewModel.givePalTemplate(templateName, player.userId)
                showTemplateDialog = false
            },
        )
    }
}

@Composable
fun PalPickerContent(
    state: PalPickerUiState,
    actions: PalPickerActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    val extraFilters = listOfNotNull(state.selectedRarityTier, state.selectedJob).size
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(R.string.palpicker_title),
                onBack = actions.onBack,
                actions = {
                    TemplateButton(actions.onOpenTemplate)
                    PlayerTargetChip(state.target, actions.onOpenPlayerPicker, Modifier.padding(start = 2.dp, end = 4.dp), compact = true)
                },
            )
        },
        bottomBar = { PalGiveBar(state, actions) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                PalSearchBar(
                    value = state.query,
                    onValueChange = actions.onQueryChange,
                    placeholder = stringResource(R.string.give_pal_search_hint),
                    trailing = { FilterButton(extraFilters, actions.onOpenFilters) },
                )
                ElementFilterGrid(state.selectedElement, actions.onElementSelected)
            }

            val filtering = state.query.isNotBlank() || state.selectedElement != null || extraFilters > 0
            if (state.results.isEmpty() && filtering) {
                EmptyState(
                    title = stringResource(R.string.give_empty_results_title),
                    message = stringResource(R.string.give_empty_results_message),
                    icon = Icons.Filled.SearchOff,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { pal ->
                        PalGridTile(pal, selected = pal.id == state.selectedPal?.id, onClick = { actions.onSelectPal(pal) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateButton(onClick: () -> Unit) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .border(1.dp, c.borderStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(17.dp))
        Text(stringResource(R.string.palpicker_template_button), style = PalTheme.type.label, color = c.textSecondary, maxLines = 1)
    }
}

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val c = PalTheme.colors
    val cd = if (activeCount > 0) stringResource(R.string.filter_button_count_fmt, activeCount) else stringResource(R.string.filter_button)
    Box {
        PalIconButton(Icons.Filled.FilterList, cd, onClick = onClick, tint = if (activeCount > 0) c.green.content else c.textMuted)
        if (activeCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(16.dp)
                    .background(c.green.solid, CircleShape),
            ) {
                Text(activeCount.toString(), style = PalTheme.type.monoSmall.copy(fontSize = 10.sp), color = c.green.onSolid)
            }
        }
    }
}

@Composable
private fun ElementFilterGrid(selected: String?, onSelect: (String?) -> Unit) {
    val options: List<String?> = listOf<String?>(null) + ELEMENT_FILTER_ORDER
    val groupCd = stringResource(R.string.give_element_group_cd)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = groupCd }) {
        options.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { element -> ElementChip(element, element == selected, { onSelect(element) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ElementChip(element: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val cd = element?.let { PalLabels.elementLabel(it) } ?: stringResource(R.string.give_element_all_cd)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) c.green.container else Color.Transparent)
            .border(1.dp, if (selected) c.green.solid else c.borderStrong, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = cd },
    ) {
        if (element == null) {
            Text(stringResource(R.string.filter_all_masc), style = PalTheme.type.label, color = if (selected) c.text else c.textSecondary)
        } else {
            FallbackAsyncImage(elementImage(element), Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PalGridTile(pal: PalEntity, selected: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.green.container else c.surface)
            .border(2.dp, if (selected) c.green.solid else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(palElementTint(pal.element1)),
        ) {
            FallbackAsyncImage(palImage(pal), Modifier.fillMaxSize(0.92f))
            FallbackAsyncImage(elementImage(pal.element1), Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp))
            if (pal.zukanIndex > 0) {
                Text(
                    stringResource(R.string.give_paldeck_fmt, pal.zukanIndex),
                    style = PalTheme.type.monoSmall.copy(fontSize = 10.sp),
                    color = c.textSecondary,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 5.dp),
                )
            }
        }
        Text(
            pal.displayName(),
            style = PalTheme.type.label,
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PalGiveBar(state: PalPickerUiState, actions: PalPickerActions) {
    val c = PalTheme.colors
    val pal = state.selectedPal
    BottomActionBar {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pal != null) {
                val details = rememberPalDetails(pal)
                GiveThumb(palImage(pal), palElementTint(pal.element1), size = 56.dp, imageSize = 54.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(pal.displayName(), style = PalTheme.type.cardTitle.copy(fontSize = 16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    details.stats?.let { s ->
                        Text(
                            stringResource(R.string.give_stats_fmt, s.hp, s.attack, s.defense),
                            style = PalTheme.type.mono.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                            color = c.textBody,
                            maxLines = 1,
                        )
                    }
                    if (details.jobs.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), maxLines = 1) {
                            details.jobs.forEach { JobLevelChip(it) }
                        }
                    }
                }
                PalIconButton(Icons.Outlined.Info, stringResource(R.string.give_details_cd), onClick = actions.onShowDetails, tint = c.textMuted)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).background(c.surfaceRaised, RoundedCornerShape(14.dp))) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.give_pal_none_selected), style = PalTheme.type.cardTitle.copy(fontSize = 16.sp), color = c.textSecondary)
                    Text(stringResource(R.string.give_pal_none_hint), style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp), color = c.textMuted)
                }
            }
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
                pal == null -> stringResource(R.string.common_give)
                state.target == null -> stringResource(R.string.give_choose_player_cta)
                else -> stringResource(R.string.give_pal_cta_fmt, pal.displayName(), state.level, state.target.name)
            },
            onClick = actions.onGive,
            enabled = pal != null,
            tone = c.green,
            height = 52.dp,
            shape = RoundedCornerShape(16.dp),
            textStyle = PalTheme.type.button.copy(fontSize = 15.5.sp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JobLevelChip(job: PalWorkSuitabilityDto, showLabel: Boolean = false) {
    val c = PalTheme.colors
    val label = PalLabels.jobLabel(job.job)
    val cd = stringResource(R.string.pal_job_level_fmt, label, job.level)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(c.surfaceRaised, CircleShape)
            .padding(start = 2.dp, end = 7.dp, top = 1.dp, bottom = 1.dp)
            .semantics(mergeDescendants = true) { contentDescription = cd },
    ) {
        FallbackAsyncImage(jobImage(job.job), Modifier.size(if (showLabel) 22.dp else 18.dp))
        Text(
            if (showLabel) cd else job.level.toString(),
            style = if (showLabel) PalTheme.type.label.copy(fontSize = 12.5.sp) else PalTheme.type.monoSmall,
            color = c.text,
        )
    }
}

/** Fiche détail d'un Pal du catalogue (feuille du bas) : éléments, rareté, stats, compétence, métiers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalDetailContent(pal: PalEntity) {
    val c = PalTheme.colors
    val details = rememberPalDetails(pal)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
    ) {
        val subtitle = listOfNotNull(
            pal.zukanIndex.takeIf { it > 0 }?.let { stringResource(R.string.give_paldeck_fmt, it) },
            pal.id,
        ).joinToString("  ·  ")
        GiveDetailHeader(palImage(pal), palElementTint(pal.element1), pal.displayName(), subtitle) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ElementBadge(pal.element1)
                pal.element2?.let { ElementBadge(it) }
                RarityBadge(pal.rarity)
            }
        }
        details.stats?.let { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(stringResource(R.string.pal_stat_hp), s.hp.toString(), c.green, Modifier.weight(1f))
                StatTile(stringResource(R.string.pal_stat_atk), s.attack.toString(), c.amber, Modifier.weight(1f))
                StatTile(stringResource(R.string.pal_stat_def), s.defense.toString(), c.sky, Modifier.weight(1f))
            }
        }
        details.skill?.let { skill ->
            GiveDetailSection(stringResource(R.string.pal_partner_skill_header)) {
                CardTitle(skill.name)
                Text(skill.description, style = PalTheme.type.bodySmall, color = c.textBody)
            }
        }
        if (details.jobs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.pal_jobs_header))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.jobs.forEach { JobLevelChip(it, showLabel = true) }
                }
            }
        }
    }
}

@Composable
private fun ElementBadge(element: String) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(palElementTint(element), CircleShape)
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
    ) {
        FallbackAsyncImage(elementImage(element), Modifier.size(16.dp))
        Text(PalLabels.elementLabel(element), style = PalTheme.type.label.copy(fontSize = 12.sp), color = c.text)
    }
}

@Composable
private fun RarityBadge(rarity: Int) {
    val c = PalTheme.colors
    val tier = PalLabels.rarityTier(rarity)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(tier.color.copy(alpha = 0.16f).compositeOver(c.surfaceRaised), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(Modifier.size(8.dp).background(tier.color, CircleShape))
        Text(stringResource(tier.labelRes), style = PalTheme.type.label.copy(fontSize = 12.sp), color = c.text)
    }
}

/** Contenu de la feuille "Filtres" : rareté et métier (l'élément reste visible sur l'écran). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalFilterSheetContent(
    selectedRarityTier: PalLabels.RarityTier?,
    selectedJob: String?,
    resultCount: Int,
    onRarityTierSelected: (PalLabels.RarityTier?) -> Unit,
    onJobSelected: (String?) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val c = PalTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.FilterList, c.green, size = 44.dp, iconSize = 22.dp, radius = 14.dp)
            Text(stringResource(R.string.filter_button), style = PalTheme.type.screenTitle, color = c.text, modifier = Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.filter_rarity_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PalChip(stringResource(R.string.filter_all_fem), selectedRarityTier == null, { onRarityTierSelected(null) })
                PalLabels.RARITY_TIERS.forEach { tier ->
                    PalChip(
                        stringResource(tier.labelRes),
                        selectedRarityTier == tier,
                        { onRarityTierSelected(tier) },
                        leading = { Box(Modifier.size(8.dp).background(tier.color, CircleShape)) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.filter_job_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PalChip(stringResource(R.string.filter_all_masc), selectedJob == null, { onJobSelected(null) })
                PalLabels.FILTERABLE_JOBS.forEach { job ->
                    PalChip(
                        PalLabels.jobLabel(job),
                        selectedJob == job,
                        { onJobSelected(job) },
                        leading = { FallbackAsyncImage(jobImage(job), Modifier.size(18.dp)) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PalButton(
                stringResource(R.string.give_filters_reset),
                onClick = onReset,
                style = PalButtonStyle.Neutral,
                enabled = selectedRarityTier != null || selectedJob != null,
                modifier = Modifier.weight(1f),
            )
            PalButton(stringResource(R.string.give_filters_show_fmt, resultCount), onClick = onDone, modifier = Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun GivePalTemplateDialog(
    players: List<PalworldPlayer>,
    initialPlayer: PalworldPlayer?,
    isSftpConfigured: Boolean,
    templates: List<String>,
    templatesLoading: Boolean,
    onOpenPalCreator: (() -> Unit)?,
    onDismiss: () -> Unit,
    onConfirm: (templateName: String, player: PalworldPlayer) -> Unit,
) {
    val c = PalTheme.colors
    var templateName by remember { mutableStateOf("") }
    var selectedPlayer by remember { mutableStateOf(initialPlayer) }
    val canGive = templateName.isNotBlank() && selectedPlayer != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceBar,
        title = { Text(stringResource(R.string.pal_template_dialog_title), style = PalTheme.type.screenTitle, color = c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TemplateNameField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    templates = templates,
                    isLoading = isSftpConfigured && templatesLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isSftpConfigured && !templatesLoading && templates.isEmpty()) {
                    Text(stringResource(R.string.pal_template_none_found), style = PalTheme.type.caption, color = c.textMuted)
                }
                PlayerSearchField(
                    players = players,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = { selectedPlayer = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onOpenPalCreator != null) {
                    Spacer(Modifier.height(2.dp))
                    PalButton(
                        stringResource(R.string.give_template_open_creator),
                        onClick = onOpenPalCreator,
                        style = PalButtonStyle.Outlined,
                        icon = Icons.Filled.AddBox,
                        height = 44.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            PalButton(
                stringResource(R.string.common_give),
                onClick = {
                    val player = selectedPlayer
                    if (templateName.isNotBlank() && player != null) onConfirm(templateName.trim(), player)
                },
                enabled = canGive,
                height = 44.dp,
            )
        },
        dismissButton = {
            PalButton(stringResource(R.string.common_cancel), onClick = onDismiss, style = PalButtonStyle.Neutral, height = 44.dp)
        },
    )
}

/** Recherche-comme-tu-tapes façon [PlayerSearchField] plutôt qu'un nuage de chips (illisible et non
 * scrollable dans un dialogue au-delà d'une poignée de templates — vu jusqu'à 50 sur un vrai
 * serveur) : la liste se réduit en tapant, le menu défile tout seul si elle reste longue. Repli sur
 * un simple champ texte si le SFTP n'est pas configuré (pas de liste à proposer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateNameField(
    value: String,
    onValueChange: (String) -> Unit,
    templates: List<String>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = PalTheme.colors
    if (templates.isEmpty()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.pal_template_name_label)) },
            trailingIcon = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.green.solid, strokeWidth = 2.dp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = giveFieldColors(),
            textStyle = PalTheme.type.mono,
            modifier = modifier,
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val results = remember(templates, value) {
        if (value.isBlank()) templates else templates.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(expanded = expanded && results.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(stringResource(R.string.pal_template_name_label)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = giveFieldColors(),
            textStyle = PalTheme.type.mono,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded && results.isNotEmpty(), onDismissRequest = { expanded = false }, containerColor = c.surfaceRaised) {
            results.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, style = PalTheme.type.mono, color = c.text) },
                    onClick = { onValueChange(name); expanded = false },
                )
            }
        }
    }
}
