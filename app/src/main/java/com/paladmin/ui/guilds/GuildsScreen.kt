package com.paladmin.ui.guilds

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.DetailDialog
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.InitialAvatar
import com.paladmin.ui.components.PalGridDialog
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

@Composable
fun GuildsScreen(
    onBack: () -> Unit,
    onOpenLiveMap: (List<Pair<Double, Double>>) -> Unit,
    viewModel: GuildsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val palGrid by viewModel.palGrid.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedGuildId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    GuildsContent(
        state = state,
        query = query,
        expandedGuildId = expandedGuildId,
        snackbarHostState = snackbarHostState,
        actions = GuildsActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onQueryChange = viewModel::onQueryChange,
            onToggle = { id -> expandedGuildId = if (expandedGuildId == id) null else id },
            onOpenMap = { guild -> onOpenLiveMap(guild.basePositions.map { it.x to it.y }) },
            onOpenCampPals = viewModel::openCampPals,
            onOpenDetail = viewModel::openDetail,
        ),
    )

    detail?.let { detailState ->
        DetailDialog(
            title = stringResource(R.string.player_detail_title_fmt, stringResource(detailState.kind.titleRes), detailState.guildName),
            isLoading = detailState.isLoading,
            error = detailState.error,
            rows = detailState.rows,
            onDismiss = viewModel::dismissDetail,
        )
    }

    palGrid?.let { palGridState ->
        PalGridDialog(state = palGridState, onDismiss = viewModel::dismissPalGrid)
    }
}

/** Callbacks de [GuildsContent]. */
data class GuildsActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onToggle: (guildId: String) -> Unit = {},
    val onOpenMap: (GuildListItem) -> Unit = {},
    val onOpenCampPals: (GuildListItem) -> Unit = {},
    val onOpenDetail: (GuildListItem, GuildDetailKind) -> Unit = { _, _ -> },
)

@Composable
fun GuildsContent(
    state: GuildsUiState,
    query: String,
    expandedGuildId: String?,
    actions: GuildsActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(title = stringResource(R.string.guilds_title), onBack = actions.onBack) {
                if (!state.isLoading || state.guilds.isNotEmpty()) {
                    val countCd = stringResource(R.string.manage_guilds_count_cd, state.guilds.size)
                    Text(
                        state.guilds.size.toString(),
                        style = PalTheme.type.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = c.textSecondary,
                        modifier = Modifier
                            .background(c.surfaceRaised, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .semantics { contentDescription = countCd },
                    )
                }
                if (state.isLoading) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                } else {
                    PalIconButton(Icons.Filled.Refresh, stringResource(R.string.cd_refresh), actions.onRefresh)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PalSearchBar(
                value = query,
                onValueChange = actions.onQueryChange,
                placeholder = stringResource(R.string.guilds_search_hint),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )

            when {
                state.isLoading && state.guilds.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.green.solid)
                }
                state.guilds.isEmpty() && query.isNotBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        title = stringResource(R.string.manage_guilds_no_results_fmt, query),
                        icon = Icons.Filled.SearchOff,
                    )
                }
                state.guilds.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = stringResource(R.string.guilds_empty),
                        message = stringResource(R.string.manage_guilds_empty_message),
                        icon = Icons.Filled.Shield,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.guilds, key = { it.guildId }) { guild ->
                        GuildCard(
                            guild = guild,
                            expanded = guild.guildId == expandedGuildId,
                            actions = actions,
                        )
                    }
                }
            }
        }
    }
}

/** Teinte stable par guilde (pastille d'icône), pour distinguer les guildes d'un coup d'œil. */
@Composable
private fun guildTone(guildId: String): Tone {
    val c = PalTheme.colors
    val tones = listOf(c.green, c.amber, c.sky, c.purple, c.danger)
    return tones[Math.floorMod(guildId.hashCode(), tones.size)]
}

@Composable
private fun GuildCard(guild: GuildListItem, expanded: Boolean, actions: GuildsActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val tone = guildTone(guild.guildId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (expanded) c.green.border else c.surface, shape),
    ) {
        val toggleLabel = stringResource(if (expanded) R.string.manage_guilds_collapse else R.string.manage_guilds_expand)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = toggleLabel) { actions.onToggle(guild.guildId) }
                .semantics { stateDescription = toggleLabel }
                .padding(14.dp),
        ) {
            IconTile(Icons.Filled.Shield, tone, size = 44.dp, iconSize = 22.dp, radius = 14.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    guild.name,
                    style = PalTheme.type.sectionTitle.copy(fontSize = 15.5.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.manage_guilds_summary_fmt, guild.level, guild.members.size, guild.baseCount),
                    style = PalTheme.type.bodySmall,
                    color = c.textMuted,
                    maxLines = 2,
                )
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(22.dp),
            )
        }

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            ) {
                if (guild.members.isNotEmpty()) MemberChips(guild.members)
                guild.camps.forEachIndexed { index, camp ->
                    CampPreview(index = index, camp = camp, onClick = { actions.onOpenCampPals(guild) })
                }
                GuildActionGrid(guild, actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberChips(members: List<GuildMemberItem>) {
    val c = PalTheme.colors
    val chief = stringResource(R.string.manage_guilds_role_chief)
    val online = stringResource(R.string.manage_guilds_online)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        members.forEach { member ->
            val name = member.name.ifBlank { "?" }
            val cd = listOfNotNull(name, chief.takeIf { member.isAdmin }, online.takeIf { member.online }).joinToString(", ")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(c.surfaceRaised, CircleShape)
                    .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                    .semantics(mergeDescendants = true) { contentDescription = cd },
            ) {
                InitialAvatar(name = name, size = 24.dp)
                Text(name, style = PalTheme.type.bodySmall, color = c.text, maxLines = 1)
                if (member.isAdmin) {
                    Text(chief, style = PalTheme.type.caption.copy(fontSize = 11.5.sp), color = c.textMuted, maxLines = 1)
                }
                if (member.online) {
                    Box(Modifier.size(7.dp).background(c.green.solid, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun CampPreview(index: Int, camp: GuildCampSummary, onClick: () -> Unit) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.background)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.guilds_action_camp_pals), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.manage_guilds_camp_title_fmt, index + 1),
                style = PalTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.manage_guilds_camp_info_fmt, camp.level, camp.palCount),
                style = PalTheme.type.bodySmall,
                color = c.textMuted,
                maxLines = 1,
            )
        }
        if (camp.palImages.isEmpty()) {
            Text(stringResource(R.string.manage_guilds_camp_empty), style = PalTheme.type.caption, color = c.textFaint)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val tileShape = RoundedCornerShape(12.dp)
                camp.palImages.forEach { image ->
                    Box(Modifier.size(46.dp).background(c.surfaceRaised, tileShape), contentAlignment = Alignment.Center) {
                        FallbackAsyncImage(model = image, modifier = Modifier.size(40.dp))
                    }
                }
                val extra = camp.palCount - camp.palImages.size
                if (extra > 0) {
                    Box(Modifier.size(46.dp).background(c.surfaceRaised, tileShape), contentAlignment = Alignment.Center) {
                        Text("+$extra", style = PalTheme.type.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = c.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildActionGrid(guild: GuildListItem, actions: GuildsActions) {
    val c = PalTheme.colors
    val hasBases = guild.basePositions.isNotEmpty()
    val chestSub = if (guild.chestUsed != null && guild.chestMax != null) {
        stringResource(R.string.manage_guilds_chest_fmt, guild.chestUsed, guild.chestMax)
    } else null
    val expeditionsSub = guild.expeditionsFinished?.let { pluralStringResource(R.plurals.manage_guilds_completed, it, it) }
    val labSub = guild.labCompleted?.let { done ->
        val running = guild.labInProgress ?: 0
        val doneText = pluralStringResource(R.plurals.manage_guilds_completed, done, done)
        if (running > 0) stringResource(R.string.manage_guilds_lab_fmt, doneText, running) else doneText
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GuildActionTile(
                title = stringResource(R.string.manage_guilds_show_on_map),
                subtitle = if (hasBases) null else stringResource(R.string.manage_guilds_no_base),
                icon = Icons.Filled.Map,
                tone = c.green,
                enabled = hasBases,
                onClick = { actions.onOpenMap(guild) },
                modifier = Modifier.weight(1f),
            )
            GuildActionTile(
                title = stringResource(R.string.manage_guilds_chest),
                subtitle = chestSub,
                image = "file:///android_asset/images/technologies/GuildChest.webp",
                onClick = { actions.onOpenDetail(guild, GuildDetailKind.CHEST) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GuildActionTile(
                title = stringResource(R.string.manage_guilds_expeditions),
                subtitle = expeditionsSub,
                image = "file:///android_asset/images/technologies/Expedition.webp",
                onClick = { actions.onOpenDetail(guild, GuildDetailKind.EXPEDITIONS) },
                modifier = Modifier.weight(1f),
            )
            GuildActionTile(
                title = stringResource(R.string.manage_guilds_lab),
                subtitle = labSub,
                image = "file:///android_asset/images/technologies/Lab.webp",
                onClick = { actions.onOpenDetail(guild, GuildDetailKind.LAB) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Case d'action 48 dp : icône ou image du jeu, titre et sous-titre facultatif (compteur réel). */
@Composable
private fun GuildActionTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    image: String? = null,
    tone: Tone? = null,
    enabled: Boolean = true,
) {
    val c = PalTheme.colors
    val bg = tone?.container ?: c.surfaceRaised
    val fg = tone?.onContainer ?: c.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        when {
            image != null -> FallbackAsyncImage(model = image, modifier = Modifier.size(22.dp))
            icon != null -> Icon(icon, contentDescription = null, tint = tone?.content ?: c.textSecondary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PalTheme.type.bodySmall.copy(fontSize = 13.5.sp), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = PalTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 14.sp), color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
