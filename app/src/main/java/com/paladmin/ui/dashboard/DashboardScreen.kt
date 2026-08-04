package com.paladmin.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.IconBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenItemPicker: () -> Unit,
    onOpenPalPicker: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenGuilds: () -> Unit,
    onOpenBans: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenLiveMap: () -> Unit,
    onOpenHumanPicker: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showShutdownDialog by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    val defaultRestartMessage = stringResource(R.string.dashboard_default_restart_message)

    LaunchedEffect(state.errorMessage, state.actionMessage) {
        (state.errorMessage ?: state.actionMessage)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.profile?.name ?: stringResource(R.string.dashboard_title_fallback)) },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(padding),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        IconBadge(icon = Icons.Filled.Dns)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                state.info?.servername?.ifBlank { state.profile?.name ?: "—" } ?: stringResource(R.string.dashboard_info_unavailable),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.dashboard_ip_fmt, state.profile?.host ?: "—"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.info?.let { info ->
                                Text(
                                    stringResource(R.string.dashboard_version_palworld_fmt, info.version),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.palDefenderVersion?.let { version ->
                                Text(
                                    stringResource(R.string.dashboard_version_paldefender_fmt, version),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = stringResource(R.string.dashboard_stat_players),
                        value = state.metrics?.let { "${it.currentplayernum}/${it.maxplayernum}" } ?: "—",
                        accent = MaterialTheme.colorScheme.primaryContainer,
                        onAccent = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.dashboard_stat_fps),
                        value = state.metrics?.serverfps?.toString() ?: "—",
                        accent = MaterialTheme.colorScheme.tertiaryContainer,
                        onAccent = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.dashboard_actions_header)) }

            item {
                Card {
                    Column {
                        ActionRow(Icons.Filled.People, stringResource(R.string.dashboard_action_players), onClick = onOpenPlayers)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Map, stringResource(R.string.dashboard_action_livemap), onClick = onOpenLiveMap)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Inventory2, stringResource(R.string.dashboard_action_give_item), onClick = onOpenItemPicker)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Pets, stringResource(R.string.dashboard_action_give_pal), onClick = onOpenPalPicker)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Face, stringResource(R.string.dashboard_action_give_human), onClick = onOpenHumanPicker)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Groups, stringResource(R.string.dashboard_action_guilds), onClick = onOpenGuilds)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Block, stringResource(R.string.dashboard_action_bans), onClick = onOpenBans)
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Save, stringResource(R.string.dashboard_action_save), onClick = { showSaveConfirm = true })
                        HorizontalDivider()
                        ActionRow(Icons.Filled.Campaign, stringResource(R.string.dashboard_action_broadcast), onClick = onOpenBroadcast, showChevron = true)
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.dashboard_danger_header), color = MaterialTheme.colorScheme.error) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    ActionRow(
                        icon = Icons.Filled.RestartAlt,
                        label = stringResource(R.string.dashboard_action_restart),
                        onClick = { showShutdownDialog = true },
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        showChevron = false,
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    ActionRow(
                        icon = Icons.Filled.Stop,
                        label = stringResource(R.string.dashboard_action_force_stop),
                        onClick = { showStopConfirm = true },
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        showChevron = false,
                    )
                }
            }
        }
    }

    if (showSaveConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.dashboard_save_confirm_title),
            message = stringResource(R.string.dashboard_save_confirm_message),
            onConfirm = { viewModel.save(); showSaveConfirm = false },
            onDismiss = { showSaveConfirm = false },
        )
    }

    if (showShutdownDialog) {
        var message by remember { mutableStateOf(defaultRestartMessage) }
        var waitTime by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.dashboard_action_restart)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.dashboard_restart_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(value = waitTime, onValueChange = { waitTime = it }, label = { Text(stringResource(R.string.dashboard_wait_time_label)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text(stringResource(R.string.dashboard_message_label)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.shutdown(waitTime.toIntOrNull() ?: 30, message)
                        showShutdownDialog = false
                    },
                ) { Text(stringResource(R.string.dashboard_schedule_stop)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showShutdownDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showStopConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.dashboard_force_stop_title),
            message = stringResource(R.string.dashboard_force_stop_message),
            confirmLabel = stringResource(R.string.dashboard_stop_action),
            onConfirm = { viewModel.forceStop(); showStopConfirm = false },
            onDismiss = { showStopConfirm = false },
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
) {
    Card(colors = CardDefaults.cardColors(containerColor = accent), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = onAccent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = onAccent.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f).padding(start = 16.dp))
        if (showChevron) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
