package com.paladmin.ui.sftp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.sftp.SftpEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpScreen(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    viewModel: SftpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val state by viewModel.state.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var renameTarget by remember { mutableStateOf<SftpEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<SftpEntry?>(null) }
    var pendingDownloadEntry by remember { mutableStateOf<SftpEntry?>(null) }
    var pendingUploadUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingUploadName by remember { mutableStateOf("") }

    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val entry = pendingDownloadEntry
        pendingDownloadEntry = null
        if (uri != null && entry != null) viewModel.download(entry, uri)
    }
    val uploadPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingUploadUri = uri
            pendingUploadName = DocumentFile.fromSingleUri(context, uri)?.name.orEmpty()
        }
    }
    val uploadFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.uploadFolder(uri)
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.currentPath, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (profile?.isSftpConfigured == true) {
                        if (state.isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 12.dp))
                        }
                        if (clipboard != null) {
                            IconButton(onClick = viewModel::paste, enabled = !state.isBusy) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.sftp_paste_cd))
                            }
                        }
                        UploadMenuButton(
                            enabled = !state.isBusy,
                            onUploadFile = { uploadPicker.launch(arrayOf("*/*")) },
                            onUploadFolder = { uploadFolderPicker.launch(null) },
                        )
                        IconButton(onClick = viewModel::refresh, enabled = !state.isBusy) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            profile == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            profile?.isSftpConfigured == false -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(stringResource(R.string.logs_not_configured), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Button(onClick = onOpenEditProfile, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.logs_open_profile_settings))
                    }
                }
            }
            state.hostKeyMismatch != null -> {
                val (expected, actual) = state.hostKeyMismatch!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.logs_host_key_mismatch),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        stringResource(R.string.logs_host_key_fmt, expected, actual),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(onClick = viewModel::trustNewHostKey, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.logs_trust_new_key))
                    }
                }
            }
            state.error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = viewModel::refresh, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.common_retry))
                }
            }
            state.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.entries.isEmpty() && (state.currentPath == "." || state.currentPath == "/") -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.sftp_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    pluralStringResource(R.plurals.sftp_entry_count, state.entries.size, state.entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.currentPath != "." && state.currentPath != "/") {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::navigateUp).padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Filled.SubdirectoryArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.profile_edit_sftp_go_up), modifier = Modifier.padding(start = 16.dp))
                            }
                            HorizontalDivider()
                        }
                    }
                    itemsIndexed(state.entries) { index, entry ->
                        SftpEntryRow(
                            entry = entry,
                            onClick = { viewModel.navigateInto(entry) },
                            onDownload = {
                                pendingDownloadEntry = entry
                                downloadLauncher.launch(entry.name)
                            },
                            onRename = { renameTarget = entry },
                            onCopy = { viewModel.copyToClipboard(entry) },
                            onCut = { viewModel.cutToClipboard(entry) },
                            onDelete = { deleteTarget = entry },
                        )
                        if (index < state.entries.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }

    renameTarget?.let { entry ->
        RenameDialog(
            currentName = entry.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.rename(entry, newName)
                renameTarget = null
            },
        )
    }

    if (pendingUploadUri != null) {
        RenameDialog(
            title = stringResource(R.string.sftp_upload_name_title),
            currentName = pendingUploadName,
            onDismiss = { pendingUploadUri = null },
            onConfirm = { name ->
                pendingUploadUri?.let { viewModel.upload(it, name) }
                pendingUploadUri = null
            },
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.sftp_delete_confirm_title)) },
            text = { Text(stringResource(R.string.sftp_delete_confirm_message_fmt, entry.name)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { viewModel.delete(entry); deleteTarget = null },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun UploadMenuButton(enabled: Boolean, onUploadFile: () -> Unit, onUploadFolder: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(Icons.Filled.DriveFolderUpload, contentDescription = stringResource(R.string.sftp_upload_cd))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sftp_upload_file)) },
                leadingIcon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
                onClick = { expanded = false; onUploadFile() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sftp_upload_folder)) },
                leadingIcon = { Icon(Icons.Filled.DriveFolderUpload, contentDescription = null) },
                onClick = { expanded = false; onUploadFolder() },
            )
        }
    }
}

@Composable
private fun SftpEntryRow(
    entry: SftpEntry,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(entry.name, modifier = Modifier.weight(1f).padding(start = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.sftp_actions_cd))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!entry.isDirectory) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sftp_download)) },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                        onClick = { menuExpanded = false; onDownload() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sftp_copy)) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onCopy() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sftp_rename)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { menuExpanded = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sftp_cut)) },
                    leadingIcon = { Icon(Icons.Filled.ContentCut, contentDescription = null) },
                    onClick = { menuExpanded = false; onCut() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sftp_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    title: String = stringResource(R.string.sftp_rename),
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text(stringResource(R.string.common_save_action))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
