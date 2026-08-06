package com.paladmin.ui.sftp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.model.ServerProfile
import androidx.documentfile.provider.DocumentFile
import com.paladmin.data.repository.ServerRepository
import com.paladmin.data.sftp.LocalTreeEntry
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.data.sftp.TofuResult
import com.paladmin.data.sftp.SftpLogClient
import com.paladmin.data.sftp.sftpJoin
import com.paladmin.data.sftp.sftpParentPath
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SftpUiState(
    val currentPath: String = ".",
    val entries: List<SftpEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val error: String? = null,
    val hostKeyMismatch: Pair<String, String>? = null,
    val statusMessage: String? = null,
)

data class SftpClipboard(val entry: SftpEntry, val isCut: Boolean)

@HiltViewModel
class SftpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val sftpLogClient: SftpLogClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _profile = MutableStateFlow<ServerProfile?>(null)
    val profile: StateFlow<ServerProfile?> = _profile.asStateFlow()

    private val _state = MutableStateFlow(SftpUiState())
    val state: StateFlow<SftpUiState> = _state.asStateFlow()

    private val _clipboard = MutableStateFlow<SftpClipboard?>(null)
    val clipboard: StateFlow<SftpClipboard?> = _clipboard.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = serverRepository.getProfile(profileId)
            _profile.value = loaded
            if (loaded?.isSftpConfigured == true) load(_state.value.currentPath)
        }
    }

    fun navigateInto(entry: SftpEntry) {
        if (entry.isDirectory) load(entry.path)
    }

    fun navigateUp() = load(sftpParentPath(_state.value.currentPath))

    fun refresh() = load(_state.value.currentPath)

    fun trustNewHostKey() = load(_state.value.currentPath, trustNewKey = true)

    fun consumeStatusMessage() {
        _state.value = _state.value.copy(statusMessage = null)
    }

    fun download(entry: SftpEntry, destination: Uri) {
        val profile = _profile.value ?: return
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val outputStream = context.contentResolver.openOutputStream(destination)
            if (outputStream == null) {
                _state.value = _state.value.copy(isBusy = false, statusMessage = context.getString(R.string.sftp_op_failed_fmt, "destination"))
                return@launch
            }
            val result = outputStream.use { output ->
                sftpLogClient.downloadFile(
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    remotePath = entry.path,
                    knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
                    sink = { input -> input.copyTo(output) },
                )
            }
            handleOpResult(result, successMessage = context.getString(R.string.sftp_downloaded_fmt, entry.name))
        }
    }

    fun upload(source: Uri, fileName: String) {
        val profile = _profile.value ?: return
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val inputStream = context.contentResolver.openInputStream(source)
            if (inputStream == null) {
                _state.value = _state.value.copy(isBusy = false, statusMessage = context.getString(R.string.sftp_op_failed_fmt, fileName))
                return@launch
            }
            val remotePath = sftpJoin(_state.value.currentPath, fileName)
            val result = inputStream.use { input ->
                sftpLogClient.uploadFile(
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    remotePath = remotePath,
                    knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
                    source = input,
                )
            }
            handleOpResult(result, successMessage = context.getString(R.string.sftp_uploaded_fmt, fileName), refreshAfter = true)
        }
    }

    /** [treeUri] vient d'un `OpenDocumentTree` (SAF) — on parcourt l'arborescence locale (dossiers
     * avant leur contenu) puis on envoie le tout en une seule session SSH via [SftpLogClient.uploadTree]. */
    fun uploadFolder(treeUri: Uri) {
        val profile = _profile.value ?: return
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.isDirectory) {
            _state.value = _state.value.copy(statusMessage = context.getString(R.string.sftp_folder_upload_failed))
            return
        }
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val entries = mutableListOf<LocalTreeEntry>()
            collectTree(root, "", entries)
            val result = sftpLogClient.uploadTree(
                host = profile.host,
                port = profile.sftpPort,
                username = profile.sftpUsername,
                password = profile.sftpPassword,
                baseRemotePath = _state.value.currentPath,
                entries = entries,
                knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
            )
            handleOpResult(result, successMessage = context.getString(R.string.sftp_folder_uploaded_fmt, root.name.orEmpty()), refreshAfter = true)
        }
    }

    private fun collectTree(dir: DocumentFile, relativePrefix: String, out: MutableList<LocalTreeEntry>) {
        dir.listFiles().forEach { child ->
            val childName = child.name ?: return@forEach
            val relativePath = if (relativePrefix.isEmpty()) childName else "$relativePrefix/$childName"
            if (child.isDirectory) {
                out += LocalTreeEntry.Directory(relativePath)
                collectTree(child, relativePath, out)
            } else {
                out += LocalTreeEntry.File(relativePath) {
                    context.contentResolver.openInputStream(child.uri) ?: error("openInputStream failed for ${child.uri}")
                }
            }
        }
    }

    fun rename(entry: SftpEntry, newName: String) {
        val profile = _profile.value ?: return
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val newPath = sftpJoin(sftpParentPath(entry.path), newName)
            val result = sftpLogClient.rename(
                host = profile.host,
                port = profile.sftpPort,
                username = profile.sftpUsername,
                password = profile.sftpPassword,
                oldPath = entry.path,
                newPath = newPath,
                knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
            )
            handleOpResult(result, successMessage = context.getString(R.string.sftp_renamed_fmt, entry.name, newName), refreshAfter = true)
        }
    }

    fun copyToClipboard(entry: SftpEntry) {
        _clipboard.value = SftpClipboard(entry, isCut = false)
    }

    fun cutToClipboard(entry: SftpEntry) {
        _clipboard.value = SftpClipboard(entry, isCut = true)
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    /** Coller reste possible plusieurs fois de suite après un Copier (comme sur desktop) ; un
     * Couper vide le presse-papiers dès le premier collage, l'entrée ayant été déplacée. */
    fun paste() {
        val profile = _profile.value ?: return
        val clip = _clipboard.value ?: return
        val destPath = sftpJoin(_state.value.currentPath, clip.entry.name)
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val result = if (clip.isCut) {
                sftpLogClient.rename(
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    oldPath = clip.entry.path,
                    newPath = destPath,
                    knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
                )
            } else {
                sftpLogClient.copyFile(
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    sourcePath = clip.entry.path,
                    destPath = destPath,
                    knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
                )
            }
            if (clip.isCut && result is TofuResult.Success) _clipboard.value = null
            handleOpResult(result, successMessage = context.getString(R.string.sftp_pasted_fmt, clip.entry.name), refreshAfter = true)
        }
    }

    fun delete(entry: SftpEntry) {
        val profile = _profile.value ?: return
        _state.value = _state.value.copy(isBusy = true)
        viewModelScope.launch {
            val result = sftpLogClient.deleteEntry(
                host = profile.host,
                port = profile.sftpPort,
                username = profile.sftpUsername,
                password = profile.sftpPassword,
                path = entry.path,
                isDirectory = entry.isDirectory,
                knownHostKeyFingerprint = profile.sftpHostKeyFingerprint,
            )
            if (_clipboard.value?.entry?.path == entry.path) _clipboard.value = null
            handleOpResult(result, successMessage = context.getString(R.string.sftp_deleted_fmt, entry.name), refreshAfter = true)
        }
    }

    private fun load(path: String, trustNewKey: Boolean = false) {
        val profile = _profile.value ?: return
        _state.value = _state.value.copy(isLoading = true, error = null, hostKeyMismatch = null)
        viewModelScope.launch {
            val known = if (trustNewKey) null else profile.sftpHostKeyFingerprint
            when (
                val result = sftpLogClient.listDirectorySecure(
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    path = path,
                    knownHostKeyFingerprint = known,
                )
            ) {
                is TofuResult.Success -> {
                    persistFingerprintIfNeeded(result.hostKeyFingerprint)
                    _state.value = _state.value.copy(isLoading = false, currentPath = path, entries = result.value)
                }
                is TofuResult.HostKeyMismatch -> {
                    _state.value = _state.value.copy(isLoading = false, hostKeyMismatch = result.expectedFingerprint to result.actualFingerprint)
                }
                is TofuResult.Failure -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun handleOpResult(result: TofuResult<*>, successMessage: String, refreshAfter: Boolean = false) {
        when (result) {
            is TofuResult.Success -> {
                persistFingerprintIfNeeded(result.hostKeyFingerprint)
                _state.value = _state.value.copy(isBusy = false, statusMessage = successMessage)
                if (refreshAfter) load(_state.value.currentPath)
            }
            is TofuResult.HostKeyMismatch -> {
                _state.value = _state.value.copy(isBusy = false, hostKeyMismatch = result.expectedFingerprint to result.actualFingerprint)
            }
            is TofuResult.Failure -> {
                _state.value = _state.value.copy(isBusy = false, statusMessage = result.message)
            }
        }
    }

    private fun persistFingerprintIfNeeded(fingerprint: String) {
        val profile = _profile.value ?: return
        if (fingerprint.isNotBlank() && profile.sftpHostKeyFingerprint != fingerprint) {
            _profile.value = profile.copy(sftpHostKeyFingerprint = fingerprint)
            viewModelScope.launch { serverRepository.updateSftpHostKeyFingerprint(profileId, fingerprint) }
        }
    }
}
