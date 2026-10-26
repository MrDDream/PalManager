package com.paladmin.ui.profiles

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paladmin.R
import com.paladmin.data.sftp.SftpBrowseResult
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.data.sftp.sftpParentPath
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalDivider
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.theme.PalTheme

/** Explorateur de dossiers/fichiers distant (SFTP) pour saisir un chemin de log en tapant dessus
 * plutôt qu'en le recopiant à la main — remplace le champ texte par une navigation, avec un bouton
 * pour retenir le dossier courant tel quel (utile pour les logs PalDefender : le dossier entier,
 * pas un fichier précis, voir [com.paladmin.data.sftp.SftpLogClient]). */
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
        SftpPathPickerContent(
            currentPath = currentPath,
            entries = entries,
            isLoading = isLoading,
            error = error,
            onBack = onDismiss,
            onPickCurrent = { onPathSelected(currentPath) },
            onOpenEntry = { entry -> if (entry.isDirectory) currentPath = entry.path else onPathSelected(entry.path) },
            onGoUp = { currentPath = sftpParentPath(currentPath) },
        )
    }
}

/** Contenu sans état du sélecteur (testable). */
@Composable
fun SftpPathPickerContent(
    currentPath: String,
    entries: List<SftpEntry>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPickCurrent: () -> Unit,
    onOpenEntry: (SftpEntry) -> Unit,
    onGoUp: () -> Unit,
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            Column(Modifier.background(c.background)) {
                PalTopBar(title = stringResource(R.string.servers_picker_title), onBack = onBack, closeIcon = true)
                Text(
                    currentPath,
                    style = PalTheme.type.mono.copy(fontSize = 13.sp),
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .background(c.field, RoundedCornerShape(12.dp))
                        .border(1.dp, c.borderStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        },
        bottomBar = {
            BottomActionBar {
                PalButton(
                    text = stringResource(R.string.profile_edit_sftp_pick_folder),
                    onClick = onPickCurrent,
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    height = 52.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(color = c.green.solid, modifier = Modifier.align(Alignment.Center))
                error != null -> EmptyState(
                    title = stringResource(R.string.servers_picker_error_title),
                    message = error,
                    icon = Icons.Filled.WarningAmber,
                    tone = c.danger,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> {
                    val canGoUp = currentPath != "." && currentPath != "/"
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (canGoUp) {
                            item {
                                BrowserRow(
                                    icon = Icons.Filled.SubdirectoryArrowLeft,
                                    iconTint = c.textMuted,
                                    label = stringResource(R.string.profile_edit_sftp_go_up),
                                    chevron = false,
                                    first = true,
                                    last = entries.isEmpty(),
                                    onClick = onGoUp,
                                )
                            }
                        }
                        itemsIndexed(entries, key = { _, entry -> entry.path }) { index, entry ->
                            BrowserRow(
                                icon = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                iconTint = if (entry.isDirectory) c.sky.content else c.textMuted,
                                label = entry.name,
                                mono = !entry.isDirectory,
                                chevron = entry.isDirectory,
                                first = index == 0 && !canGoUp,
                                last = index == entries.lastIndex,
                                onClick = { onOpenEntry(entry) },
                            )
                        }
                        if (entries.isEmpty()) {
                            item {
                                EmptyState(
                                    title = stringResource(R.string.servers_picker_empty),
                                    icon = Icons.Filled.FolderOff,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ligne d'une liste groupée dans une carte : les coins arrondis ne s'appliquent qu'aux extrémités. */
@Composable
private fun BrowserRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
    mono: Boolean = false,
    chevron: Boolean = false,
) {
    val c = PalTheme.colors
    val radius = 20.dp
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    Column(Modifier.clip(shape).background(c.surface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(
                label,
                style = if (mono) PalTheme.type.mono.copy(fontSize = 13.sp) else PalTheme.type.body,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (chevron) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
        }
        if (!last) PalDivider(Modifier.padding(start = 50.dp))
    }
}
