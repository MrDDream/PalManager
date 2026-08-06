package com.paladmin.ui.profiles

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paladmin.R
import com.paladmin.data.sftp.SftpBrowseResult
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.data.sftp.sftpParentPath

/** Explorateur de dossiers/fichiers distant (SFTP) pour saisir un chemin de log en tapant dessus
 * plutôt qu'en le recopiant à la main — remplace le champ texte par une navigation, avec un bouton
 * pour retenir le dossier courant tel quel (utile pour les logs PalDefender : le dossier entier,
 * pas un fichier précis, voir [com.paladmin.data.sftp.SftpLogClient]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpPathPickerDialog(
    initialPath: String,
    browse: suspend (String) -> SftpBrowseResult,
    onDismiss: () -> Unit,
    onPathSelected: (String) -> Unit,
) {
    var currentPath by remember { mutableStateOf(initialPath.ifBlank { "." }) }
    var entries by remember { mutableStateOf<List<SftpEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath) {
        isLoading = true
        error = null
        when (val result = browse(currentPath)) {
            is SftpBrowseResult.Success -> {
                entries = result.entries
                isLoading = false
            }
            is SftpBrowseResult.Failure -> {
                error = result.message
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(currentPath, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Button(
                        onClick = { onPathSelected(currentPath) },
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    ) {
                        Icon(Icons.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.profile_edit_sftp_pick_folder))
                    }

                    when {
                        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        error != null -> Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (currentPath != "." && currentPath != "/") {
                                item {
                                    BrowserRow(
                                        icon = Icons.Filled.SubdirectoryArrowLeft,
                                        label = stringResource(R.string.profile_edit_sftp_go_up),
                                        onClick = { currentPath = sftpParentPath(currentPath) },
                                    )
                                }
                            }
                            items(entries) { entry ->
                                BrowserRow(
                                    icon = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                    label = entry.name,
                                    onClick = {
                                        if (entry.isDirectory) currentPath = entry.path else onPathSelected(entry.path)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.padding(start = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
