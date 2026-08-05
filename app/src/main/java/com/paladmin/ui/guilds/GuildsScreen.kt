package com.paladmin.ui.guilds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.paladmin.ui.components.DetailDialog
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.components.PalGridDialog
import com.paladmin.ui.components.SearchField

@OptIn(ExperimentalMaterial3Api::class)
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
    var menuFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guilds_title)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = stringResource(R.string.guilds_search_hint),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.guilds.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.guilds_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.guilds, key = { it.guildId }) { guild ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconBadge(icon = Icons.Filled.Groups)
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(guild.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(R.string.guilds_level_members_fmt, guild.level, guild.members.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { menuFor = guild.guildId }) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_actions))
                                        }
                                        DropdownMenu(expanded = menuFor == guild.guildId, onDismissRequest = { menuFor = null }) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.guilds_action_location)) },
                                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                                enabled = guild.basePositions.isNotEmpty(),
                                                onClick = {
                                                    onOpenLiveMap(guild.basePositions.map { it.x to it.y })
                                                    menuFor = null
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.guilds_action_camp_pals)) },
                                                leadingIcon = { Icon(Icons.Filled.Pets, contentDescription = null) },
                                                onClick = { viewModel.openCampPals(guild); menuFor = null },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.guilds_action_chest)) },
                                                leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                                                onClick = { viewModel.openDetail(guild, GuildDetailKind.CHEST); menuFor = null },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.guilds_action_expeditions)) },
                                                leadingIcon = { Icon(Icons.Filled.Explore, contentDescription = null) },
                                                onClick = { viewModel.openDetail(guild, GuildDetailKind.EXPEDITIONS); menuFor = null },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.guilds_action_lab)) },
                                                leadingIcon = { Icon(Icons.Filled.Science, contentDescription = null) },
                                                onClick = { viewModel.openDetail(guild, GuildDetailKind.LAB); menuFor = null },
                                            )
                                        }
                                    }
                                }

                                if (guild.members.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        guild.members.forEach { member ->
                                            Text(
                                                "${if (member.online) "🟢" else "⚪"} ${member.name}${if (member.isAdmin) " 👑" else ""}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
