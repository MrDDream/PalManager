package com.paladmin.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.DetailLoading
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.InitialAvatar
import com.paladmin.ui.components.MonoBadge
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalChip
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

internal enum class PendingAction { KICK, BAN, MESSAGE }

/** Plateforme déduite de l'userId (préfixe "steam_" / "gdk_" ou "xbox"), null si inconnue. */
@Composable
internal fun platformLabel(userId: String): String? = when {
    userId.startsWith("steam_", ignoreCase = true) -> stringResource(R.string.players_v2_platform_steam)
    userId.startsWith("gdk_", ignoreCase = true) || userId.startsWith("xbox", ignoreCase = true) -> stringResource(R.string.players_v2_platform_xbox)
    else -> null
}

/** Couleur de latence : vert < 50 ms, ambre < 100 ms, rouge au-delà. */
@Composable
internal fun pingTone(ping: Int): Tone = with(PalTheme.colors) {
    when {
        ping < 50 -> green
        ping < 100 -> amber
        else -> danger
    }
}

/** Callbacks de [PlayersContent] (valeurs par défaut vides pour les aperçus/tests). */
data class PlayersActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSortChange: (PlayerSort) -> Unit = {},
    val onToggle: (PalworldPlayer) -> Unit = {},
    val onOpenTab: (PalworldPlayer, PlayerTab) -> Unit = { _, _ -> },
    val onMessage: (PalworldPlayer) -> Unit = {},
    val onKick: (PalworldPlayer) -> Unit = {},
    val onBan: (PalworldPlayer) -> Unit = {},
)

@Composable
fun PlayersScreen(
    onBack: () -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheet by viewModel.sheet.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var expandedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<PalworldPlayer, PendingAction>?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    PlayersContent(
        state = state,
        expandedUserId = expandedUserId,
        snackbarHostState = snackbarHostState,
        actions = PlayersActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onQueryChange = viewModel::onQueryChange,
            onSortChange = viewModel::onSortChange,
            onToggle = { player -> expandedUserId = if (expandedUserId == player.userId) null else player.userId },
            onOpenTab = viewModel::openSheet,
            onMessage = { pendingAction = it to PendingAction.MESSAGE },
            onKick = { pendingAction = it to PendingAction.KICK },
            onBan = { pendingAction = it to PendingAction.BAN },
        ),
    )

    sheet?.let { sheetState ->
        PlayerSheet(
            state = sheetState,
            onSelectTab = viewModel::selectTab,
            onRetry = viewModel::retryTab,
            onMessage = { pendingAction = sheetState.player to PendingAction.MESSAGE },
            onKick = { pendingAction = sheetState.player to PendingAction.KICK },
            onBan = { pendingAction = sheetState.player to PendingAction.BAN },
            onDismiss = viewModel::dismissSheet,
        )
    }

    pendingAction?.let { (player, action) ->
        PlayerActionDialog(
            player = player,
            action = action,
            onDismiss = { pendingAction = null },
            onConfirm = { text ->
                when (action) {
                    PendingAction.KICK -> viewModel.kick(player, text)
                    PendingAction.BAN -> viewModel.ban(player, text)
                    PendingAction.MESSAGE -> viewModel.sendMessage(player, text)
                }
                // Le joueur exclu/banni quitte le serveur : sa fiche n'a plus lieu d'être ouverte.
                if (action != PendingAction.MESSAGE && sheet?.player?.userId == player.userId) viewModel.dismissSheet()
                pendingAction = null
            },
        )
    }
}

/** Liste des joueurs connectés (stateless) : en-tête "x/max", recherche, tri, cartes dépliables. */
@Composable
fun PlayersContent(
    state: PlayersUiState,
    expandedUserId: String?,
    actions: PlayersActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(R.string.players_title),
                onBack = actions.onBack,
                actions = {
                    val count = state.players.size
                    val max = state.maxPlayers
                    val countCd = if (max != null) stringResource(R.string.players_v2_count_cd, count, max) else count.toString()
                    MonoBadge(
                        text = if (max != null) "$count/$max" else count.toString(),
                        tone = c.green,
                        style = PalTheme.type.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.semantics { contentDescription = countCd },
                    )
                    if (state.isLoading && state.players.isNotEmpty()) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.green.solid, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        PalIconButton(Icons.Filled.Refresh, stringResource(R.string.cd_refresh), onClick = actions.onRefresh)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                PalSearchBar(
                    value = state.query,
                    onValueChange = actions.onQueryChange,
                    placeholder = stringResource(R.string.players_v2_search_placeholder),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PlayerSort.LEVEL to R.string.players_v2_sort_level,
                        PlayerSort.PING to R.string.players_v2_sort_ping,
                        PlayerSort.NAME to R.string.players_v2_sort_name,
                    ).forEach { (sort, label) ->
                        PalChip(label = stringResource(label), selected = state.sort == sort, onClick = { actions.onSortChange(sort) })
                    }
                }
            }

            val visible = state.visiblePlayers
            when {
                state.isLoading && state.players.isEmpty() -> DetailLoading()
                state.players.isEmpty() -> EmptyState(
                    title = stringResource(R.string.players_empty),
                    icon = Icons.Filled.Groups,
                    modifier = Modifier.fillMaxWidth(),
                    action = { PalButton(stringResource(R.string.common_retry), onClick = actions.onRefresh, style = PalButtonStyle.Neutral, height = 44.dp) },
                )
                visible.isEmpty() -> EmptyState(
                    title = stringResource(R.string.players_v2_no_match),
                    icon = Icons.Filled.SearchOff,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visible, key = { it.userId.ifBlank { it.name } }) { player ->
                        PlayerCard(player = player, expanded = player.userId == expandedUserId, actions = actions)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlatformBadge(label: String) {
    val c = PalTheme.colors
    Text(
        label,
        style = PalTheme.type.monoSmall.copy(fontSize = 10.5.sp),
        color = c.textSecondary,
        maxLines = 1,
        modifier = Modifier.background(c.divider, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun PlayerCard(player: PalworldPlayer, expanded: Boolean, actions: PlayersActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val ping = player.ping.toInt()
    val pingTone = pingTone(ping)
    val stateLabel = stringResource(if (expanded) R.string.players_v2_state_expanded else R.string.players_v2_state_collapsed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (expanded) c.green.border else c.surface, shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { actions.onToggle(player) }
                .semantics { stateDescription = stateLabel }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            InitialAvatar(player.name, size = 42.dp, seed = player.userId.ifBlank { player.name })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        player.name,
                        style = PalTheme.type.body.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    platformLabel(player.userId)?.let { PlatformBadge(it) }
                }
                val levelPrefix = stringResource(R.string.players_v2_level_ip_fmt, player.level)
                Text(
                    buildAnnotatedString {
                        append(levelPrefix)
                        withStyle(SpanStyle(fontFamily = PalTheme.type.mono.fontFamily)) { append(player.ip) }
                    },
                    style = PalTheme.type.bodySmall,
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val pingCd = stringResource(R.string.players_v2_ping_cd_fmt, ping)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = pingCd },
            ) {
                Box(Modifier.size(7.dp).background(pingTone.content, CircleShape))
                Text(
                    stringResource(R.string.players_v2_ping_fmt, ping),
                    style = PalTheme.type.mono.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                    color = pingTone.content,
                    maxLines = 1,
                )
            }
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            ) {
                SelectionContainer {
                    Text(player.userId, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textFaint)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionTile(stringResource(R.string.players_v2_btn_inventory), Modifier.weight(1f), icon = Icons.Filled.Inventory2) { actions.onOpenTab(player, PlayerTab.INVENTORY) }
                    ActionTile(stringResource(R.string.players_v2_btn_team), Modifier.weight(1f), icon = Icons.Filled.Pets) { actions.onOpenTab(player, PlayerTab.TEAM) }
                    ActionTile(stringResource(R.string.players_v2_btn_palbox), Modifier.weight(1f), image = "file:///android_asset/images/technologies/PALBOX.webp") { actions.onOpenTab(player, PlayerTab.PALBOX) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionTile(stringResource(R.string.players_v2_btn_progression), Modifier.weight(1f), icon = Icons.Filled.TrendingUp) { actions.onOpenTab(player, PlayerTab.PROGRESSION) }
                    ActionTile(
                        stringResource(R.string.players_v2_btn_techs),
                        Modifier.weight(1f),
                        image = "file:///android_asset/images/technologies/Product_WorkBench_SkillUnlock.webp",
                    ) { actions.onOpenTab(player, PlayerTab.TECHS) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModerationButton(stringResource(R.string.players_v2_btn_message), Icons.AutoMirrored.Filled.Message, c.green, Modifier.weight(1f)) { actions.onMessage(player) }
                    ModerationButton(stringResource(R.string.players_v2_btn_kick), Icons.Filled.Logout, c.amber, Modifier.weight(1f)) { actions.onKick(player) }
                    ModerationButton(stringResource(R.string.players_v2_btn_ban), Icons.Filled.Block, c.danger, Modifier.weight(1f)) { actions.onBan(player) }
                }
            }
        }
    }
}

/** Bouton secondaire de la carte joueur (fond "raised", icône ou image du jeu + libellé). */
@Composable
private fun ActionTile(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    image: String? = null,
    onClick: () -> Unit,
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        when {
            image != null -> FallbackAsyncImage(image, Modifier.size(20.dp))
            icon != null -> Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
        Text(text, style = PalTheme.type.bodySmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ModerationButton(text: String, icon: ImageVector, tone: Tone, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PalButton(
        text = text,
        onClick = onClick,
        tone = tone,
        style = PalButtonStyle.Tonal,
        icon = icon,
        height = 44.dp,
        shape = RoundedCornerShape(12.dp),
        textStyle = PalTheme.type.label,
        modifier = modifier,
    )
}

@Composable
private fun PlayerActionDialog(
    player: PalworldPlayer,
    action: PendingAction,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = PalTheme.colors
    var text by remember { mutableStateOf("") }
    val title: String
    val label: String
    val confirmLabel: String
    val tone: Tone
    when (action) {
        PendingAction.KICK -> {
            title = stringResource(R.string.players_kick_title_fmt, player.name)
            label = stringResource(R.string.players_reason_label)
            confirmLabel = stringResource(R.string.players_action_kick_short)
            tone = c.amber
        }
        PendingAction.BAN -> {
            title = stringResource(R.string.players_ban_title_fmt, player.name)
            label = stringResource(R.string.players_reason_label)
            confirmLabel = stringResource(R.string.players_action_ban)
            tone = c.danger
        }
        PendingAction.MESSAGE -> {
            title = stringResource(R.string.players_message_title_fmt, player.name)
            label = stringResource(R.string.dashboard_message_label)
            confirmLabel = stringResource(R.string.players_action_send)
            tone = c.green
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceBar,
        title = { Text(title, style = PalTheme.type.cardTitle, color = c.text) },
        text = {
            PalTextField(
                value = text,
                onValueChange = { text = it },
                label = label,
                singleLine = action != PendingAction.MESSAGE,
                minLines = if (action == PendingAction.MESSAGE) 3 else 1,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PalButton(
                text = confirmLabel,
                onClick = { onConfirm(text) },
                enabled = action != PendingAction.MESSAGE || text.isNotBlank(),
                tone = tone,
                height = 44.dp,
            )
        },
        dismissButton = {
            PalButton(stringResource(R.string.common_cancel), onClick = onDismiss, style = PalButtonStyle.Neutral, height = 44.dp)
        },
    )
}
