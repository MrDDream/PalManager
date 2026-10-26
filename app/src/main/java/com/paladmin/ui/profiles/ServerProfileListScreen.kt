package com.paladmin.ui.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.paladmin.R
import com.paladmin.data.model.ServerProfile
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.DashedButton
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.InfoNote
import com.paladmin.ui.components.MonoBadge
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.ServerIcons
import com.paladmin.ui.components.StatusPill
import com.paladmin.ui.theme.PalTheme

@Composable
fun ServerProfileListScreen(
    onOpenDashboard: (Long) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ServerProfileListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var profileToDelete by remember { mutableStateOf<ServerProfile?>(null) }

    // Au retour sur la liste (depuis le dashboard, l'édition...), on rafraîchit les statuts périmés.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ServerProfileListContent(
        state = state,
        actions = ServerListActions(
            onOpenDashboard = onOpenDashboard,
            onAddProfile = onAddProfile,
            onEditProfile = onEditProfile,
            onDeleteProfile = { profileToDelete = it },
            onProbe = viewModel::probe,
            onOpenSettings = onOpenSettings,
        ),
    )

    profileToDelete?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.profiles_delete_title_fmt, profile.name),
            message = stringResource(R.string.profiles_delete_message),
            confirmLabel = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteProfile(profile); profileToDelete = null },
            onDismiss = { profileToDelete = null },
        )
    }
}

/** Callbacks de [ServerProfileListContent]. */
data class ServerListActions(
    val onOpenDashboard: (Long) -> Unit = {},
    val onAddProfile: () -> Unit = {},
    val onEditProfile: (Long) -> Unit = {},
    val onDeleteProfile: (ServerProfile) -> Unit = {},
    val onProbe: (ServerProfile) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun ServerProfileListContent(state: ServerListUiState, actions: ServerListActions) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = { BrandHeader(onOpenSettings = actions.onOpenSettings) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.green.solid)
            }
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                Text(
                    stringResource(R.string.servers_title),
                    style = PalTheme.type.display,
                    color = c.text,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.profiles.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.servers_profile_count, state.profiles.size, state.profiles.size),
                        style = PalTheme.type.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            if (state.profiles.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.servers_empty_title),
                    message = stringResource(R.string.servers_empty_message),
                    icon = Icons.Filled.Dns,
                    tone = c.green,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.profiles.forEach { profile ->
                ServerCard(
                    profile = profile,
                    probe = state.probes[profile.id] ?: ServerProbe.Checking,
                    actions = actions,
                )
            }

            DashedButton(
                text = stringResource(R.string.servers_add),
                onClick = actions.onAddProfile,
                icon = Icons.Filled.Add,
            )

            // L'astuce SFTP n'a de sens que s'il reste au moins un profil sans SFTP.
            if (state.profiles.isEmpty() || state.profiles.any { !it.isSftpConfigured }) {
                InfoNote(stringResource(R.string.servers_sftp_hint), icon = Icons.Outlined.Info, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun BrandHeader(onOpenSettings: () -> Unit) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(c.background)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp)
            .height(56.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = c.green.content,
            modifier = Modifier.size(34.dp),
        )
        Text(
            stringResource(R.string.servers_brand),
            style = PalTheme.type.brand,
            color = c.text,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        PalIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.cd_settings),
            onClick = onOpenSettings,
            tint = c.textSecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ServerCard(profile: ServerProfile, probe: ServerProbe, actions: ServerListActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(22.dp)
    var menuOpen by remember { mutableStateOf(false) }
    val online = probe is ServerProbe.Online
    val openActionsLabel = stringResource(R.string.servers_open_actions)

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (online) c.green.border else c.border, shape)
            .combinedClickable(
                role = Role.Button,
                onLongClickLabel = openActionsLabel,
                onLongClick = { menuOpen = true },
                onClick = { actions.onOpenDashboard(profile.id) },
            )
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 16.dp),
    ) {
        // En-tête : icône du profil, statut, menu. Le nom et l'hôte ont ensuite toute la largeur.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(
                ServerIcons.iconFor(profile.iconKey),
                tone = if (online) c.green else c.neutral,
                size = 36.dp,
                iconSize = 20.dp,
                radius = 11.dp,
            )
            Spacer(Modifier.weight(1f))
            ProbePill(probe)
            Box {
                PalIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.servers_more_actions, profile.name),
                    onClick = { menuOpen = true },
                    tint = c.textMuted,
                    iconSize = 20.dp,
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_edit)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; actions.onEditProfile(profile.id) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete), color = c.danger.content) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = c.danger.content) },
                        onClick = { menuOpen = false; actions.onDeleteProfile(profile) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(end = 10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile.name,
                    style = PalTheme.type.cardTitle.copy(fontSize = 18.sp),
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${profile.host}:${profile.palworldPort}",
                    style = PalTheme.type.mono.copy(fontSize = 13.sp),
                    color = c.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (probe) {
                is ServerProbe.Online -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MiniStat(probe.players.toString(), "/${probe.maxPlayers}", stringResource(R.string.servers_stat_players), Modifier.weight(1f))
                        MiniStat(probe.fps.toString(), null, stringResource(R.string.servers_stat_fps), Modifier.weight(1f))
                        MiniStat(probe.latencyMs.toString(), stringResource(R.string.servers_stat_ms), stringResource(R.string.servers_stat_latency), Modifier.weight(1f))
                    }
                    ConfigBadges(profile)
                }
                ServerProbe.Checking -> {
                    Text(stringResource(R.string.servers_checking_detail), style = PalTheme.type.bodySmall, color = c.textMuted)
                    ConfigBadges(profile)
                }
                else -> {
                    val detail = when (probe) {
                        ServerProbe.Unauthorized -> stringResource(R.string.servers_denied_detail)
                        is ServerProbe.HttpError -> stringResource(R.string.servers_http_error_detail, probe.code)
                        else -> stringResource(R.string.servers_offline_detail)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(detail, style = PalTheme.type.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
                        PalButton(
                            text = stringResource(R.string.servers_verify),
                            onClick = { actions.onProbe(profile) },
                            style = PalButtonStyle.Neutral,
                            icon = Icons.Filled.Refresh,
                            height = 44.dp,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = PalTheme.type.label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbePill(probe: ServerProbe, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    when (probe) {
        is ServerProbe.Online -> StatusPill(stringResource(R.string.servers_status_online), c.green, modifier, dot = c.green.solid)
        ServerProbe.Checking -> StatusPill(stringResource(R.string.servers_status_checking), c.neutral, modifier, dot = c.textFaint)
        ServerProbe.Unauthorized -> StatusPill(stringResource(R.string.servers_status_denied), c.amber, modifier, dot = c.amber.solid)
        is ServerProbe.HttpError -> StatusPill(stringResource(R.string.servers_status_error), c.amber, modifier, dot = c.amber.solid)
        ServerProbe.Offline -> StatusPill(stringResource(R.string.servers_status_offline), c.neutral, modifier, dot = c.textFaint)
    }
}

@Composable
private fun MiniStat(value: String, unit: String?, label: String, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier) {
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) {
                    withStyle(SpanStyle(fontSize = 13.sp, color = c.textMuted, fontWeight = FontWeight.SemiBold)) { append(unit) }
                }
            },
            style = PalTheme.type.stat.copy(fontSize = 20.sp, lineHeight = 24.sp),
            color = c.text,
            maxLines = 1,
        )
        Text(label, style = PalTheme.type.caption, color = c.textMuted, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigBadges(profile: ServerProfile) {
    val c = PalTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MonoBadge(stringResource(R.string.servers_badge_rest), tone = c.green, pill = false)
        if (profile.palDefenderToken.isNotBlank()) {
            MonoBadge(stringResource(R.string.servers_badge_paldefender), tone = c.green, pill = false)
        }
        if (profile.isSftpConfigured) {
            MonoBadge(stringResource(R.string.servers_badge_sftp), tone = c.sky, pill = false)
        }
    }
}
