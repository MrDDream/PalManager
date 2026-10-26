package com.paladmin.ui.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.InfoNote
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalChip
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.components.SegmentedControl
import com.paladmin.ui.dashboard.PowerSheet
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

enum class BroadcastMode { Announce, Alert, Private }

@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel(),
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteBaseId by remember { mutableStateOf<String?>(null) }
    var showPowerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    BroadcastContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = BroadcastActions(
            onBack = onBack,
            onBroadcast = viewModel::broadcast,
            onAlert = viewModel::alert,
            onPrivateMessage = viewModel::sendPrivateMessage,
            onReloadConfig = viewModel::reloadConfig,
            onOpenPower = { showPowerSheet = true },
            onDeleteBase = { id -> pendingDeleteBaseId = id },
        ),
    )

    pendingDeleteBaseId?.let { baseCampId ->
        ConfirmDialog(
            title = stringResource(R.string.broadcast_delete_base_confirm_title_fmt, baseCampId),
            message = stringResource(R.string.broadcast_delete_base_confirm_message),
            confirmLabel = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteBase(baseCampId); pendingDeleteBaseId = null },
            onDismiss = { pendingDeleteBaseId = null },
        )
    }

    if (showPowerSheet) {
        PowerSheet(
            serverName = state.serverName,
            onlinePlayers = state.players.size,
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

data class BroadcastActions(
    val onBack: () -> Unit = {},
    val onBroadcast: (String) -> Unit = {},
    val onAlert: (String) -> Unit = {},
    val onPrivateMessage: (PalworldPlayer, String) -> Unit = { _, _ -> },
    val onReloadConfig: () -> Unit = {},
    val onOpenPower: () -> Unit = {},
    /** Demande la suppression (l'écran affiche la confirmation). */
    val onDeleteBase: (String) -> Unit = {},
)

/**
 * Formulaire de diffusion (annonce / alerte / message privé), maintenance et zone danger.
 * L'état de saisie vit ici ; les valeurs initiales servent aux captures d'écran.
 */
@Composable
fun BroadcastContent(
    state: BroadcastUiState,
    actions: BroadcastActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialMode: BroadcastMode = BroadcastMode.Announce,
    initialMessage: String = "",
    initialRecipientId: String? = null,
    initialDangerOpen: Boolean = false,
) {
    val c = PalTheme.colors
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var message by rememberSaveable { mutableStateOf(initialMessage) }
    var recipientId by rememberSaveable { mutableStateOf(initialRecipientId) }
    var baseCampId by rememberSaveable { mutableStateOf("") }
    var dangerOpen by rememberSaveable { mutableStateOf(initialDangerOpen) }

    Scaffold(
        containerColor = c.background,
        topBar = { PalTopBar(title = stringResource(R.string.broadcast_title), onBack = actions.onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            val recipient = state.players.firstOrNull { it.userId == recipientId }
            MessageCard(
                state = state,
                mode = mode,
                onModeChange = { mode = it },
                message = message,
                onMessageChange = { message = it },
                recipient = recipient,
                onRecipientChange = { recipientId = it.userId.takeIf { id -> id != recipientId } },
                onSend = {
                    when (mode) {
                        BroadcastMode.Announce -> actions.onBroadcast(message)
                        BroadcastMode.Alert -> actions.onAlert(message)
                        BroadcastMode.Private -> recipient?.let { actions.onPrivateMessage(it, message) }
                    }
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.manage_bc_maintenance))
                MaintenanceRow(
                    icon = Icons.Filled.Sync,
                    tone = c.sky,
                    title = stringResource(R.string.manage_bc_reload),
                    onClick = actions.onReloadConfig,
                )
                MaintenanceRow(
                    icon = Icons.Filled.PowerSettingsNew,
                    tone = c.amber,
                    title = stringResource(R.string.manage_bc_power),
                    subtitle = stringResource(R.string.manage_bc_power_sub),
                    chevron = true,
                    onClick = actions.onOpenPower,
                )
            }

            DangerZone(
                open = dangerOpen,
                onToggle = { dangerOpen = !dangerOpen },
                baseCampId = baseCampId,
                onBaseCampIdChange = { baseCampId = it },
                onDelete = { if (baseCampId.isNotBlank()) actions.onDeleteBase(baseCampId.trim()) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
    state: BroadcastUiState,
    mode: BroadcastMode,
    onModeChange: (BroadcastMode) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    recipient: PalworldPlayer?,
    onRecipientChange: (PalworldPlayer) -> Unit,
    onSend: () -> Unit,
) {
    val c = PalTheme.colors
    val tone = when (mode) {
        BroadcastMode.Announce -> c.sky
        BroadcastMode.Alert -> c.amber
        BroadcastMode.Private -> c.green
    }
    PalCard(shape = RoundedCornerShape(22.dp), spacing = 12.dp, modifier = Modifier.fillMaxWidth()) {
        val modesCd = stringResource(R.string.manage_bc_modes_cd)
        SegmentedControl(
            options = listOf(
                BroadcastMode.Announce to stringResource(R.string.manage_bc_mode_announce),
                BroadcastMode.Alert to stringResource(R.string.manage_bc_mode_alert),
                BroadcastMode.Private to stringResource(R.string.manage_bc_mode_dm),
            ),
            selected = mode,
            onSelect = onModeChange,
            modifier = Modifier.semantics { contentDescription = modesCd },
        )

        if (mode == BroadcastMode.Private) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.manage_bc_recipient), style = PalTheme.type.bodySmall, color = c.textSecondary)
                if (state.players.isEmpty()) {
                    InfoNote(stringResource(R.string.manage_bc_no_players))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.players.forEach { player ->
                            PalChip(
                                label = player.name.ifBlank { player.userId },
                                selected = player.userId == recipient?.userId,
                                onClick = { onRecipientChange(player) },
                            )
                        }
                    }
                }
            }
        }

        PalTextField(
            value = message,
            onValueChange = onMessageChange,
            label = when (mode) {
                BroadcastMode.Announce -> stringResource(R.string.broadcast_message_label)
                BroadcastMode.Alert -> stringResource(R.string.broadcast_alert_label)
                BroadcastMode.Private -> recipient?.let { stringResource(R.string.manage_bc_field_dm_fmt, it.name) }
                    ?: stringResource(R.string.manage_bc_mode_dm)
            },
            singleLine = false,
            minLines = 3,
        )

        PalButton(
            text = when (mode) {
                BroadcastMode.Announce -> if (state.playersLoaded) {
                    stringResource(R.string.manage_bc_send_announce_fmt, state.players.size)
                } else {
                    stringResource(R.string.broadcast_send_action)
                }
                BroadcastMode.Alert -> stringResource(R.string.broadcast_alert_action)
                BroadcastMode.Private -> recipient?.let { stringResource(R.string.manage_bc_send_dm_fmt, it.name) }
                    ?: stringResource(R.string.manage_bc_send_dm)
            },
            onClick = onSend,
            tone = tone,
            icon = Icons.AutoMirrored.Filled.Send,
            enabled = message.isNotBlank() && (mode != BroadcastMode.Private || recipient != null),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MaintenanceRow(
    icon: ImageVector,
    tone: Tone,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    chevron: Boolean = false,
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        IconTile(icon, tone, size = 36.dp, iconSize = 19.dp, radius = 10.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PalTheme.type.body.copy(fontWeight = FontWeight.Medium), color = c.text)
            if (subtitle != null) Text(subtitle, style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp), color = c.textMuted)
        }
        if (chevron) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DangerZone(
    open: Boolean,
    onToggle: () -> Unit,
    baseCampId: String,
    onBaseCampIdChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = PalTheme.colors
    val danger = c.danger
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, danger.border, shape),
    ) {
        val hint = stringResource(if (open) R.string.manage_bc_danger_hide else R.string.manage_bc_danger_show)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(danger.container.copy(alpha = if (c.isDark) 0.55f else 0.8f))
                .clickable(role = Role.Button, onClickLabel = hint, onClick = onToggle)
                .semantics { stateDescription = hint }
                .padding(horizontal = 14.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = danger.onContainer, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.broadcast_danger_header),
                style = PalTheme.type.label.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = danger.onContainer,
                modifier = Modifier.weight(1f),
            )
            Text(hint, style = PalTheme.type.bodySmall, color = danger.onContainer)
        }
        if (open) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(danger.container.copy(alpha = if (c.isDark) 0.28f else 0.35f))
                    .padding(PaddingValues(14.dp)),
            ) {
                PalTextField(
                    value = baseCampId,
                    onValueChange = onBaseCampIdChange,
                    label = stringResource(R.string.broadcast_base_id_label),
                    placeholder = stringResource(R.string.manage_bc_base_placeholder),
                    mono = true,
                )
                PalButton(
                    text = stringResource(R.string.broadcast_delete_base_action),
                    onClick = onDelete,
                    tone = danger,
                    icon = Icons.Filled.DeleteForever,
                    enabled = baseCampId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
