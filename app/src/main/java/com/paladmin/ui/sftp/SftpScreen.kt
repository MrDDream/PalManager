package com.paladmin.ui.sftp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.ToolsErrorState
import com.paladmin.ui.components.ToolsHostKeyMismatchState
import com.paladmin.ui.components.ToolsLoadingState
import com.paladmin.ui.components.ToolsNotConfiguredState
import com.paladmin.ui.theme.PalTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var selected by remember { mutableStateOf<SftpEntry?>(null) }
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
    // Changer de dossier abandonne la sélection (l'entrée n'est plus affichée).
    LaunchedEffect(state.currentPath) { selected = null }

    SftpContent(
        profileLoaded = profile != null,
        sftpConfigured = profile?.isSftpConfigured == true,
        hostKeyPinned = !profile?.sftpHostKeyFingerprint.isNullOrBlank(),
        state = state,
        clipboard = clipboard,
        selected = selected,
        snackbarHostState = snackbarHostState,
        actions = SftpActions(
            onBack = onBack,
            onOpenEditProfile = onOpenEditProfile,
            onRefresh = viewModel::refresh,
            onTrustNewKey = viewModel::trustNewHostKey,
            onNavigateUp = viewModel::navigateUp,
            onNavigateTo = viewModel::navigateTo,
            onOpen = viewModel::navigateInto,
            onSelect = { selected = it },
            onUploadFile = { uploadPicker.launch(arrayOf("*/*")) },
            onUploadFolder = { uploadFolderPicker.launch(null) },
            onDownload = { entry ->
                pendingDownloadEntry = entry
                downloadLauncher.launch(entry.name)
            },
            onRename = { renameTarget = it },
            onCopy = { viewModel.copyToClipboard(it); selected = null },
            onCut = { viewModel.cutToClipboard(it); selected = null },
            onDelete = { deleteTarget = it },
            onPaste = viewModel::paste,
            onClearClipboard = viewModel::clearClipboard,
        ),
    )

    renameTarget?.let { entry ->
        NameDialog(
            currentName = entry.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.rename(entry, newName)
                renameTarget = null
                selected = null
            },
        )
    }

    if (pendingUploadUri != null) {
        NameDialog(
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
                PalButton(
                    text = stringResource(R.string.common_delete),
                    onClick = { viewModel.delete(entry); deleteTarget = null; selected = null },
                    tone = PalTheme.colors.danger,
                    height = 44.dp,
                )
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** Callbacks de [SftpContent]. */
data class SftpActions(
    val onBack: () -> Unit = {},
    val onOpenEditProfile: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onTrustNewKey: () -> Unit = {},
    val onNavigateUp: () -> Unit = {},
    val onNavigateTo: (String) -> Unit = {},
    val onOpen: (SftpEntry) -> Unit = {},
    /** null = désélectionner. */
    val onSelect: (SftpEntry?) -> Unit = {},
    val onUploadFile: () -> Unit = {},
    val onUploadFolder: () -> Unit = {},
    val onDownload: (SftpEntry) -> Unit = {},
    val onRename: (SftpEntry) -> Unit = {},
    val onCopy: (SftpEntry) -> Unit = {},
    val onCut: (SftpEntry) -> Unit = {},
    val onDelete: (SftpEntry) -> Unit = {},
    val onPaste: () -> Unit = {},
    val onClearClipboard: () -> Unit = {},
)

private fun isRootPath(path: String) = path == "." || path == "/"

@Composable
fun SftpContent(
    profileLoaded: Boolean,
    sftpConfigured: Boolean,
    hostKeyPinned: Boolean,
    state: SftpUiState,
    clipboard: SftpClipboard?,
    selected: SftpEntry?,
    actions: SftpActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    // La sélection ne survit pas à un rafraîchissement où l'entrée a disparu (renommée, supprimée).
    val selection = selected?.let { s -> state.entries.find { it.path == s.path } }
    Scaffold(
        containerColor = c.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(Modifier.background(c.background)) {
                PalTopBar(
                    title = stringResource(R.string.tools_files_title),
                    onBack = actions.onBack,
                    actions = {
                        if (sftpConfigured) {
                            if (hostKeyPinned && state.hostKeyMismatch == null) HostKeyBadge()
                            PalIconButton(
                                Icons.Filled.Refresh,
                                stringResource(R.string.cd_refresh),
                                onClick = actions.onRefresh,
                                enabled = !state.isBusy && !state.isLoading,
                                tint = c.text,
                            )
                        }
                    },
                )
                if (sftpConfigured && state.hostKeyMismatch == null) {
                    PathBreadcrumb(state.currentPath, onNavigate = actions.onNavigateTo, enabled = !state.isLoading)
                }
                if (state.isBusy) {
                    LinearProgressIndicator(
                        color = c.sky.solid,
                        trackColor = c.sky.container,
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                    )
                }
            }
        },
    ) { padding ->
        val inner = Modifier.fillMaxSize().padding(padding)
        when {
            !profileLoaded -> ToolsLoadingState(inner)
            !sftpConfigured -> ToolsNotConfiguredState(actions.onOpenEditProfile, inner)
            state.hostKeyMismatch != null -> {
                val (expected, actual) = state.hostKeyMismatch
                ToolsHostKeyMismatchState(expected, actual, actions.onTrustNewKey, inner)
            }
            state.error != null -> ToolsErrorState(state.error, actions.onRefresh, inner)
            state.isLoading -> ToolsLoadingState(inner)
            else -> Box(inner) {
                EntryList(state, selection, actions)
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                ) {
                    if (selection != null) {
                        SelectionPanel(selection, enabled = !state.isBusy, actions)
                    } else {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (clipboard != null) ClipboardCard(clipboard, enabled = !state.isBusy, actions)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 4.dp)) {
                                PalButton(
                                    text = stringResource(R.string.sftp_upload_folder),
                                    onClick = actions.onUploadFolder,
                                    style = PalButtonStyle.Neutral,
                                    icon = Icons.Filled.DriveFolderUpload,
                                    enabled = !state.isBusy,
                                    height = 52.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    textStyle = PalTheme.type.button.copy(fontSize = 14.sp),
                                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
                                )
                                PalButton(
                                    text = stringResource(R.string.sftp_upload_cd),
                                    onClick = actions.onUploadFile,
                                    tone = c.sky,
                                    icon = Icons.Filled.UploadFile,
                                    enabled = !state.isBusy,
                                    height = 52.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    textStyle = PalTheme.type.button.copy(fontSize = 14.sp),
                                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
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
private fun HostKeyBadge() {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(c.sky.container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = c.sky.content, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.tools_files_host_key_verified),
            style = PalTheme.type.mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
            color = c.sky.onContainer,
            maxLines = 1,
        )
    }
}

/** Segments cliquables d'un chemin : "." (dossier de connexion) et "/" (racine) deviennent des repères. */
private data class PathSegment(val label: String, val path: String, val isHome: Boolean = false)

private fun pathSegments(path: String): List<PathSegment> {
    val absolute = path.startsWith("/")
    val parts = path.split('/').filter { it.isNotEmpty() }
    val out = mutableListOf<PathSegment>()
    if (absolute) out += PathSegment("/", "/")
    parts.forEachIndexed { index, part ->
        val joined = parts.take(index + 1).joinToString("/")
        val full = if (absolute) "/$joined" else joined
        out += if (!absolute && index == 0 && part == ".") PathSegment("~", full, isHome = true) else PathSegment(part, full)
    }
    if (out.isEmpty()) out += PathSegment("~", ".", isHome = true)
    return out
}

@Composable
private fun PathBreadcrumb(path: String, onNavigate: (String) -> Unit, enabled: Boolean) {
    val c = PalTheme.colors
    val segments = remember(path) { pathSegments(path) }
    val scroll = rememberScrollState()
    LaunchedEffect(path) {
        snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            val last = index == segments.lastIndex
            if (index > 0 && segments[index - 1].label != "/") {
                Text("›", style = PalTheme.type.mono.copy(fontSize = 12.5.sp), color = c.textMuted)
            }
            val description = if (segment.isHome) stringResource(R.string.tools_files_home_cd) else segment.path
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (!last) {
                            Modifier.clickable(enabled = enabled, role = Role.Button) { onNavigate(segment.path) }
                                .semantics { contentDescription = description }
                        } else {
                            Modifier.semantics { contentDescription = description }
                        },
                    )
                    .padding(horizontal = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (last) Modifier.background(c.surfaceRaised, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp) else Modifier,
                ) {
                    if (segment.isHome) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = if (last) c.text else c.textMuted, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            segment.label,
                            style = PalTheme.type.mono.copy(fontSize = 12.5.sp),
                            color = if (last) c.text else c.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryList(state: SftpUiState, selection: SftpEntry?, actions: SftpActions) {
    val c = PalTheme.colors
    val atRoot = isRootPath(state.currentPath)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 180.dp),
    ) {
        item(key = "header") {
            Text(
                pluralStringResource(R.plurals.sftp_entry_count, state.entries.size, state.entries.size),
                style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp),
                color = c.textMuted,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            )
        }
        if (!atRoot) {
            item(key = "parent") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = actions.onNavigateUp)
                        .padding(horizontal = 10.dp),
                ) {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SubdirectoryArrowLeft, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        stringResource(R.string.profile_edit_sftp_go_up),
                        style = PalTheme.type.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                        color = c.textSecondary,
                    )
                }
            }
        }
        if (state.entries.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    EmptyState(stringResource(R.string.sftp_folder_empty), icon = Icons.Filled.FolderOff)
                }
            }
        }
        items(state.entries, key = { it.path }) { entry ->
            EntryRow(
                entry = entry,
                selected = entry.path == selection?.path,
                onClick = {
                    when {
                        entry.isDirectory -> actions.onOpen(entry)
                        entry.path == selection?.path -> actions.onSelect(null)
                        else -> actions.onSelect(entry)
                    }
                },
                onSelect = { actions.onSelect(if (entry.path == selection?.path) null else entry) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(entry: SftpEntry, selected: Boolean, onClick: () -> Unit, onSelect: () -> Unit) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(if (selected) c.sky.container else Color.Transparent)
            .border(1.dp, if (selected) c.sky.border else Color.Transparent, shape)
            .semantics { this.selected = selected }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onSelect)
            .padding(start = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(if (entry.isDirectory) c.sky.container else c.surfaceRaised, RoundedCornerShape(10.dp)),
        ) {
            Icon(
                if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (entry.isDirectory) c.sky.content else c.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                entry.name,
                style = PalTheme.type.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entryMeta(entry)?.let {
                Text(it, style = PalTheme.type.mono.copy(fontSize = 11.5.sp, lineHeight = 15.sp), color = c.textFaint, maxLines = 1)
            }
        }
        PalIconButton(
            Icons.Filled.MoreVert,
            stringResource(R.string.tools_files_select_fmt, entry.name),
            onClick = onSelect,
            tint = c.textMuted,
            iconSize = 20.dp,
        )
    }
}

@Composable
private fun entryMeta(entry: SftpEntry): String? {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val date = entry.modifiedEpochSeconds?.let { seconds ->
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "ddMMHHmm")
        SimpleDateFormat(pattern, locale).format(Date(seconds * 1000))
    }
    val size = if (entry.isDirectory) null else entry.size?.let { formatSize(it, locale) }
    return listOfNotNull(size, date).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun formatSize(bytes: Long, locale: Locale): String {
    val units = listOf(R.string.tools_files_size_kb, R.string.tools_files_size_mb, R.string.tools_files_size_gb)
    if (bytes < 1024) return stringResource(R.string.tools_files_size_b, bytes.toString())
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val number = if (value < 10) String.format(locale, "%.1f", value) else String.format(locale, "%.0f", value)
    return stringResource(units[unit], number)
}

/** Panneau flottant d'actions sur l'entrée sélectionnée (télécharger/copier : fichiers seulement). */
@Composable
private fun SelectionPanel(entry: SftpEntry, enabled: Boolean, actions: SftpActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .background(c.surfaceRaised, shape)
            .border(1.dp, c.borderStrong, shape)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
            Text(
                entry.name,
                style = PalTheme.type.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PalIconButton(Icons.Filled.Close, stringResource(R.string.tools_files_deselect), onClick = { actions.onSelect(null) }, iconSize = 20.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            if (!entry.isDirectory) {
                PanelAction(Icons.Filled.Download, stringResource(R.string.sftp_download), enabled, Modifier.weight(1f)) { actions.onDownload(entry) }
            }
            PanelAction(Icons.Filled.Edit, stringResource(R.string.sftp_rename), enabled, Modifier.weight(1f)) { actions.onRename(entry) }
            if (!entry.isDirectory) {
                PanelAction(Icons.Filled.ContentCopy, stringResource(R.string.sftp_copy), enabled, Modifier.weight(1f)) { actions.onCopy(entry) }
            }
            PanelAction(Icons.Filled.ContentCut, stringResource(R.string.sftp_cut), enabled, Modifier.weight(1f)) { actions.onCut(entry) }
            PanelAction(Icons.Filled.Delete, stringResource(R.string.sftp_delete), enabled, Modifier.weight(1f), danger = true) { actions.onDelete(entry) }
        }
    }
}

@Composable
private fun PanelAction(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val c = PalTheme.colors
    val bg = if (danger) c.danger.container else c.surfaceBar
    val fg = (if (danger) c.danger.onContainer else c.text).let { if (enabled) it else it.copy(alpha = 0.45f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = PalTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Presse-papiers SFTP : rappel de l'entrée copiée/coupée + "Coller ici" dans le dossier courant. */
@Composable
private fun ClipboardCard(clipboard: SftpClipboard, enabled: Boolean, actions: SftpActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .background(c.surfaceRaised, shape)
            .border(1.dp, c.borderStrong, shape)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(
            if (clipboard.isCut) Icons.Filled.ContentCut else Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = c.sky.content,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (clipboard.isCut) R.string.tools_files_clipboard_cut else R.string.tools_files_clipboard_copied),
                style = PalTheme.type.caption,
                color = c.textMuted,
            )
            Text(
                clipboard.entry.name,
                style = PalTheme.type.mono.copy(fontSize = 12.5.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PalButton(
            text = stringResource(R.string.tools_files_paste_here),
            onClick = actions.onPaste,
            tone = c.sky,
            style = PalButtonStyle.Tonal,
            icon = Icons.Filled.ContentPaste,
            enabled = enabled,
            height = 44.dp,
            shape = RoundedCornerShape(12.dp),
            textStyle = PalTheme.type.button.copy(fontSize = 13.5.sp),
        )
        PalIconButton(Icons.Filled.Close, stringResource(R.string.tools_files_clipboard_clear), onClick = actions.onClearClipboard, iconSize = 20.dp)
    }
}

@Composable
private fun NameDialog(
    title: String = stringResource(R.string.sftp_rename),
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { PalTextField(value = name, onValueChange = { name = it }, mono = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            PalButton(
                text = stringResource(R.string.common_save_action),
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                height = 44.dp,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
