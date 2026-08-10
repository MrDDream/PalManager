package com.paladmin.ui.players

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.DetailDialog
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.components.PalGridDialog

private enum class PendingAction { KICK, BAN, MESSAGE }

private data class ActionDialogCopy(val title: String, val label: String, val confirmLabel: String, val isDangerous: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onBack: () -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val palGrid by viewModel.palGrid.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var menuFor by remember { mutableStateOf<PalworldPlayer?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<PalworldPlayer, PendingAction>?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.players_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }

        if (state.players.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(stringResource(R.string.players_empty), modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(state.players, key = { it.userId }) { player ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(player.name) },
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.players_supporting_fmt, player.level, player.ip, player.ping.toInt()))
                                Text(
                                    stringResource(R.string.common_id_fmt, player.userId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        leadingContent = { IconBadge(icon = Icons.Filled.Person) },
                        trailingContent = {
                            IconButton(onClick = { menuFor = player }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_actions))
                            }
                            DropdownMenu(expanded = menuFor == player, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_message)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                                    onClick = { pendingAction = player to PendingAction.MESSAGE; menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_inventory)) },
                                    leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                                    onClick = { viewModel.openDetail(player, PlayerDetailKind.INVENTORY); menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_team)) },
                                    leadingIcon = { Icon(Icons.Filled.Pets, contentDescription = null) },
                                    onClick = { viewModel.openTeam(player); menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_palbox)) },
                                    leadingIcon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
                                    onClick = { viewModel.openPalbox(player); menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_progression)) },
                                    leadingIcon = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                                    onClick = { viewModel.openDetail(player, PlayerDetailKind.PROGRESSION); menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_techs)) },
                                    leadingIcon = { Icon(Icons.Filled.Science, contentDescription = null) },
                                    onClick = { viewModel.openDetail(player, PlayerDetailKind.TECHS); menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_kick)) },
                                    leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                                    onClick = { pendingAction = player to PendingAction.KICK; menuFor = null },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.players_action_ban)) },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { pendingAction = player to PendingAction.BAN; menuFor = null },
                                )
                            }
                        },
                    )
                }
            }
        }
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
                pendingAction = null
            },
        )
    }

    detail?.let { detailState ->
        DetailDialog(
            title = stringResource(R.string.player_detail_title_fmt, stringResource(detailState.kind.titleRes), detailState.playerName),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerActionDialog(
    player: PalworldPlayer,
    action: PendingAction,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val (title, label, confirmLabel, isDangerous) = when (action) {
        PendingAction.KICK -> ActionDialogCopy(
            stringResource(R.string.players_kick_title_fmt, player.name),
            stringResource(R.string.players_reason_label),
            stringResource(R.string.players_action_kick_short),
            true,
        )
        PendingAction.BAN -> ActionDialogCopy(
            stringResource(R.string.players_ban_title_fmt, player.name),
            stringResource(R.string.players_reason_label),
            stringResource(R.string.players_action_ban),
            true,
        )
        PendingAction.MESSAGE -> ActionDialogCopy(
            stringResource(R.string.players_message_title_fmt, player.name),
            stringResource(R.string.dashboard_message_label),
            stringResource(R.string.players_action_send),
            false,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = action != PendingAction.MESSAGE || text.isNotBlank(),
                colors = if (isDangerous) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

