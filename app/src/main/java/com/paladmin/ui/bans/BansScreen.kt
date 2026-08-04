package com.paladmin.ui.bans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.IconBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BansScreen(
    onBack: () -> Unit,
    viewModel: BansViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var ipInput by remember { mutableStateOf("") }
    var showBanIpConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bans_title)) },
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.bans_ip_ban_header), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text(stringResource(R.string.bans_ip_label)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Button(
                                onClick = { if (ipInput.isNotBlank()) showBanIpConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.bans_ban_action))
                            }
                            OutlinedButton(
                                onClick = { if (ipInput.isNotBlank()) viewModel.unbanIp(ipInput) },
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.bans_unban_action))
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.bans_banned_players_header), style = MaterialTheme.typography.titleMedium)
            }

            when {
                state.isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    }
                }
                state.bans.isEmpty() -> item {
                    Text(stringResource(R.string.bans_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(state.bans, key = { it.uid }) { ban ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(ban.name.ifBlank { ban.uid }) },
                            supportingContent = { Text(ban.reason.ifBlank { stringResource(R.string.bans_no_reason) }) },
                            leadingContent = { IconBadge(icon = Icons.Filled.Block, tint = MaterialTheme.colorScheme.error) },
                            trailingContent = {
                                OutlinedButton(onClick = { viewModel.unbanPlayer(ban.uid) }) {
                                    Text(stringResource(R.string.bans_unban_action))
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showBanIpConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.bans_ban_ip_confirm_title_fmt, ipInput),
            message = stringResource(R.string.bans_ban_ip_confirm_message),
            confirmLabel = stringResource(R.string.bans_ban_action),
            onConfirm = { viewModel.banIp(ipInput); showBanIpConfirm = false },
            onDismiss = { showBanIpConfirm = false },
        )
    }
}
