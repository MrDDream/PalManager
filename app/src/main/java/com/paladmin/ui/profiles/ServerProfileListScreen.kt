package com.paladmin.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.paladmin.data.model.ServerProfile
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.components.ServerIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerProfileListScreen(
    onOpenDashboard: (Long) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ServerProfileListViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsState()
    var profileToDelete by remember { mutableStateOf<ServerProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_server))
            }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    stringResource(R.string.profiles_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.profiles_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ServerProfileRow(
                        profile = profile,
                        onClick = { onOpenDashboard(profile.id) },
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { profileToDelete = profile },
                    )
                }
            }
        }
    }

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

@Composable
private fun ServerProfileRow(
    profile: ServerProfile,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(profile.name) },
            supportingContent = { Text("${profile.host}:${profile.palworldPort}") },
            leadingContent = { IconBadge(icon = ServerIcons.iconFor(profile.iconKey)) },
            trailingContent = {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
