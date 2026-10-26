package com.paladmin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.InitialAvatar
import com.paladmin.ui.components.MonoBadge
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.components.StatTile
import com.paladmin.ui.components.ThinProgress
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone
import kotlinx.coroutines.launch

/** Destinations de la barre de navigation du bas (raccourcis vers les écrans les plus utilisés). */
enum class DashboardTab { Home, Players, Map, Give }

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenItemPicker: () -> Unit,
    onOpenPalPicker: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenGuilds: () -> Unit,
    onOpenBans: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenLiveMap: () -> Unit,
    onOpenHumanPicker: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSftp: () -> Unit,
    onOpenPalCreator: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.dashboard_address_copied)

    var showPowerSheet by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage, state.actionMessage) {
        (state.errorMessage ?: state.actionMessage)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessages()
        }
    }

    DashboardContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DashboardActions(
            onSwitchServer = onBack,
            onRefresh = viewModel::refresh,
            onOpenSettings = onOpenSettings,
            onCopyAddress = { address ->
                clipboard.setText(AnnotatedString(address))
                scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
            },
            onSave = { showSaveConfirm = true },
            onPower = { showPowerSheet = true },
            onEditProfile = onEditProfile,
            onOpenItemPicker = onOpenItemPicker,
            onOpenPalPicker = onOpenPalPicker,
            onOpenHumanPicker = onOpenHumanPicker,
            onOpenPlayers = onOpenPlayers,
            onOpenGuilds = onOpenGuilds,
            onOpenLiveMap = onOpenLiveMap,
            onOpenBans = onOpenBans,
            onOpenBroadcast = onOpenBroadcast,
            onOpenLogs = onOpenLogs,
            onOpenSftp = onOpenSftp,
            onOpenPalCreator = onOpenPalCreator,
        ),
    )

    if (showSaveConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.dashboard_save_confirm_title),
            message = stringResource(R.string.dashboard_save_confirm_message),
            onConfirm = { viewModel.save(); showSaveConfirm = false },
            onDismiss = { showSaveConfirm = false },
        )
    }

    if (showPowerSheet) {
        PowerSheet(
            serverName = state.profile?.name.orEmpty(),
            onlinePlayers = state.metrics?.currentplayernum ?: state.players.size,
            onSchedule = { seconds, message ->
                viewModel.shutdown(seconds, message)
                showPowerSheet = false
            },
            onForceStop = {
                viewModel.forceStop()
                showPowerSheet = false
            },
            onDismiss = { showPowerSheet = false },
        )
    }
}

/** Callbacks de [DashboardContent], regroupés pour garder la signature lisible. */
data class DashboardActions(
    val onSwitchServer: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onCopyAddress: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onPower: () -> Unit = {},
    val onEditProfile: () -> Unit = {},
    val onOpenItemPicker: () -> Unit = {},
    val onOpenPalPicker: () -> Unit = {},
    val onOpenHumanPicker: () -> Unit = {},
    val onOpenPlayers: () -> Unit = {},
    val onOpenGuilds: () -> Unit = {},
    val onOpenLiveMap: () -> Unit = {},
    val onOpenBans: () -> Unit = {},
    val onOpenBroadcast: () -> Unit = {},
    val onOpenLogs: () -> Unit = {},
    val onOpenSftp: () -> Unit = {},
    val onOpenPalCreator: () -> Unit = {},
)

@Composable
fun DashboardContent(
    state: DashboardUiState,
    actions: DashboardActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    val online = state.info != null
    Scaffold(
        containerColor = c.background,
        topBar = { DashboardHeader(state, online, actions) },
        bottomBar = {
            DashboardBottomNav(
                selected = DashboardTab.Home,
                onSelect = { tab ->
                    when (tab) {
                        DashboardTab.Home -> Unit
                        DashboardTab.Players -> actions.onOpenPlayers()
                        DashboardTab.Map -> actions.onOpenLiveMap()
                        DashboardTab.Give -> actions.onOpenItemPicker()
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading && state.profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.green.solid)
            }
            return@Scaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        ) {
            ServerCard(state, online, actions)

            SectionLabel(stringResource(R.string.dashboard_section_give))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().offset(y = (-6).dp)) {
                GiveTile(stringResource(R.string.dashboard_give_item_short), "file:///android_asset/images/items/PalSphere.webp", c.amber, actions.onOpenItemPicker, Modifier.weight(1f))
                GiveTile(stringResource(R.string.dashboard_give_pal_short), "file:///android_asset/images/pals/SheepBall.webp", c.green, actions.onOpenPalPicker, Modifier.weight(1f))
                GiveTile(stringResource(R.string.dashboard_give_human_short), "file:///android_asset/images/humans/Ambassador01.webp", c.sky, actions.onOpenHumanPicker, Modifier.weight(1f))
            }

            SectionLabel(stringResource(R.string.dashboard_section_manage))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().offset(y = (-6).dp)) {
                ManageTile(stringResource(R.string.dashboard_manage_guilds), Icons.Filled.Shield, actions.onOpenGuilds, Modifier.weight(1f))
                ManageTile(stringResource(R.string.dashboard_manage_map), Icons.Filled.Map, actions.onOpenLiveMap, Modifier.weight(1f))
                ManageTile(stringResource(R.string.dashboard_manage_bans), Icons.Filled.Block, actions.onOpenBans, Modifier.weight(1f))
                ManageTile(stringResource(R.string.dashboard_manage_broadcast), Icons.Filled.Campaign, actions.onOpenBroadcast, Modifier.weight(1f))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.dashboard_section_tools))
                MonoBadge("SFTP", tone = c.sky)
            }
            if (state.profile?.isSftpConfigured == true) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().offset(y = (-6).dp)) {
                    ToolTile(stringResource(R.string.dashboard_tool_logs), Icons.Filled.Terminal, actions.onOpenLogs, Modifier.weight(1f))
                    ToolTile(stringResource(R.string.dashboard_tool_files), Icons.Filled.Folder, actions.onOpenSftp, Modifier.weight(1f))
                    ToolTile(stringResource(R.string.dashboard_tool_creator), Icons.Filled.AddBox, actions.onOpenPalCreator, Modifier.weight(1f))
                }
            } else {
                SftpLockedCard(actions.onEditProfile, Modifier.offset(y = (-6).dp))
            }

            val palworld = state.info?.version?.takeIf { it.isNotBlank() }
            if (palworld != null || state.palDefenderVersion != null) {
                Text(
                    stringResource(R.string.dashboard_versions_fmt, palworld ?: "—", state.palDefenderVersion ?: "—"),
                    style = PalTheme.type.mono.copy(fontSize = 12.sp),
                    color = c.textFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState, online: Boolean, actions: DashboardActions) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(c.background)
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = 12.dp, end = 8.dp),
    ) {
        val switchLabel = stringResource(R.string.cd_switch_server)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = switchLabel, onClick = actions.onSwitchServer)
                .heightIn(min = 44.dp)
                .padding(horizontal = 8.dp),
        ) {
            val dot = if (online) c.green.solid else c.textFaint
            Box(
                Modifier
                    .size(18.dp)
                    .background(dot.copy(alpha = 0.18f), CircleShape)
                    .padding(4.dp)
                    .background(dot, CircleShape),
            )
            Text(
                state.profile?.name ?: stringResource(R.string.dashboard_title_fallback),
                style = PalTheme.type.screenTitle,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        }
        if (state.isLoading) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        } else {
            PalIconButton(Icons.Filled.Refresh, stringResource(R.string.cd_refresh), actions.onRefresh)
        }
        PalIconButton(Icons.Filled.Settings, stringResource(R.string.cd_settings), actions.onOpenSettings)
    }
}

@Composable
private fun ServerCard(state: DashboardUiState, online: Boolean, actions: DashboardActions) {
    val c = PalTheme.colors
    val metrics = state.metrics
    PalCard(border = c.border, shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(16.dp), spacing = 14.dp) {
        val host = state.profile?.host.orEmpty()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.dashboard_address).uppercase(), style = PalTheme.type.caption.copy(letterSpacing = 0.5.sp), color = c.textMuted)
                Text(host.ifBlank { "—" }, style = PalTheme.type.mono, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (host.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceRaised)
                        .clickable(role = Role.Button) { actions.onCopyAddress(host) },
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy_address), tint = c.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.dashboard_stat_players),
                value = metrics?.currentplayernum?.toString() ?: "—",
                unit = metrics?.let { "/${it.maxplayernum}" },
                tone = c.green,
                modifier = Modifier.weight(1f),
                footer = {
                    val max = metrics?.maxplayernum ?: 0
                    ThinProgress(if (max > 0) metrics!!.currentplayernum / max.toFloat() else 0f, c.green)
                },
            )
            StatTile(
                label = stringResource(R.string.dashboard_stat_fps),
                value = metrics?.serverfps?.toString() ?: "—",
                tone = c.sky,
                modifier = Modifier.weight(1f),
                footer = {
                    if (metrics != null && metrics.serverframetime > 0) {
                        Text(
                            stringResource(R.string.dashboard_frame_time_fmt, metrics.serverframetime),
                            style = PalTheme.type.caption.copy(fontSize = 11.sp),
                            color = c.sky.onContainer,
                            maxLines = 1,
                        )
                    }
                },
            )
            UptimeTile(metrics?.uptime, metrics?.days, Modifier.weight(1f))
        }

        PlayersSummaryRow(state.players, actions.onOpenPlayers)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PalButton(
                text = stringResource(R.string.dashboard_save_short),
                onClick = actions.onSave,
                icon = Icons.Filled.Save,
                enabled = online,
                modifier = Modifier.weight(1f),
                textStyle = PalTheme.type.button.copy(fontSize = 14.sp),
            )
            PalButton(
                text = stringResource(R.string.dashboard_power),
                onClick = actions.onPower,
                icon = Icons.Filled.PowerSettingsNew,
                tone = c.amber,
                style = PalButtonStyle.Outlined,
                enabled = online,
                modifier = Modifier.weight(1f).background(c.amber.container.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                textStyle = PalTheme.type.button.copy(fontSize = 14.sp),
            )
        }
    }
}

@Composable
private fun UptimeTile(uptimeSeconds: Long?, days: Int?, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val dUnit = stringResource(R.string.unit_days_short)
    val hUnit = stringResource(R.string.unit_hours_short)
    val mUnit = stringResource(R.string.unit_minutes_short)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.background(c.amber.container, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Text(stringResource(R.string.dashboard_stat_uptime), style = PalTheme.type.caption, color = c.amber.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val unitStyle = SpanStyle(fontSize = 15.sp, color = c.amber.onContainer)
        val text = buildAnnotatedString {
            if (uptimeSeconds == null) {
                append("—")
            } else {
                val d = uptimeSeconds / 86_400
                val h = (uptimeSeconds % 86_400) / 3_600
                val m = (uptimeSeconds % 3_600) / 60
                when {
                    d > 0 -> { append("$d"); withStyle(unitStyle) { append(dUnit) }; append("$h"); withStyle(unitStyle) { append(hUnit) } }
                    h > 0 -> { append("$h"); withStyle(unitStyle) { append(hUnit) }; append(" $m"); withStyle(unitStyle) { append(mUnit) } }
                    else -> { append("$m"); withStyle(unitStyle) { append(mUnit) } }
                }
            }
        }
        Text(text, style = PalTheme.type.stat, color = c.text, maxLines = 1)
        if (days != null && days > 0) {
            Text(stringResource(R.string.dashboard_ingame_day_fmt, days), style = PalTheme.type.caption.copy(fontSize = 11.sp), color = c.amber.onContainer, maxLines = 1)
        }
    }
}

@Composable
private fun PlayersSummaryRow(players: List<PalworldPlayer>, onClick: () -> Unit) {
    val c = PalTheme.colors
    val names = players.map { it.name.ifBlank { "?" } }
    val summary = when (names.size) {
        0 -> stringResource(R.string.dashboard_players_none)
        1 -> names[0]
        2 -> stringResource(R.string.dashboard_players_two_fmt, names[0], names[1])
        else -> stringResource(R.string.dashboard_players_many_fmt, names[0], names[1], names.size - 2)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (players.isEmpty()) {
            Icon(Icons.Filled.People, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        } else {
            val shown = players.take(if (players.size > 5) 4 else 5)
            Box {
                shown.forEachIndexed { index, player ->
                    InitialAvatar(
                        name = player.name,
                        seed = player.userId.ifBlank { player.name },
                        size = 30.dp,
                        ring = c.surfaceRaised,
                        modifier = Modifier.offset(x = (22 * index).dp),
                    )
                }
                if (players.size > 5) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = (22 * shown.size).dp)
                            .size(30.dp)
                            .background(c.surfaceRaised, CircleShape)
                            .padding(2.dp)
                            .background(c.borderStrong, CircleShape),
                    ) {
                        Text("+${players.size - shown.size}", style = PalTheme.type.caption.copy(fontWeight = FontWeight.Medium), color = c.textSecondary)
                    }
                }
                val count = shown.size + if (players.size > 5) 1 else 0
                Spacer(Modifier.width((22 * (count - 1) + 30).dp).height(30.dp))
            }
        }
        Text(
            summary,
            style = PalTheme.type.body.copy(fontSize = 14.sp),
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GiveTile(label: String, image: String, tone: Tone, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).background(tone.container, RoundedCornerShape(16.dp)),
        ) {
            FallbackAsyncImage(model = image, modifier = Modifier.size(44.dp))
        }
        Text(label, style = PalTheme.type.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ManageTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
    ) {
        IconTile(icon, c.green)
        Text(label, style = PalTheme.type.label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Normal), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ToolTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        IconTile(icon, c.sky, size = 34.dp, iconSize = 18.dp, radius = 10.dp)
        Text(label, style = PalTheme.type.label.copy(fontWeight = FontWeight.Normal), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SftpLockedCard(onConfigure: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    PalCard(modifier = modifier.fillMaxWidth(), spacing = 10.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.Lock, c.sky, size = 34.dp, iconSize = 18.dp, radius = 10.dp)
            Text(stringResource(R.string.dashboard_sftp_locked), style = PalTheme.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
        }
        PalButton(
            text = stringResource(R.string.dashboard_sftp_configure),
            onClick = onConfigure,
            tone = c.sky,
            style = PalButtonStyle.Tonal,
            height = 44.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DashboardBottomNav(selected: DashboardTab, onSelect: (DashboardTab) -> Unit) {
    val c = PalTheme.colors
    val navLabel = stringResource(R.string.nav_main_label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceBar)
            .semantics { contentDescription = navLabel },
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
        ) {
            listOf(
                Triple(DashboardTab.Home, Icons.Filled.Home, R.string.nav_home),
                Triple(DashboardTab.Players, Icons.Filled.People, R.string.nav_players),
                Triple(DashboardTab.Map, Icons.Filled.Map, R.string.nav_map),
                Triple(DashboardTab.Give, Icons.Filled.Redeem, R.string.nav_give),
            ).forEach { (tab, icon, label) ->
                val on = tab == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = on },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 56.dp, height = 30.dp)
                            .background(if (on) c.green.container else Color.Transparent, RoundedCornerShape(15.dp)),
                    ) {
                        Icon(icon, contentDescription = null, tint = if (on) c.green.content else c.textMuted, modifier = Modifier.size(21.dp))
                    }
                    Text(
                        stringResource(label),
                        style = PalTheme.type.caption.copy(fontSize = 11.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (on) c.green.content else c.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
