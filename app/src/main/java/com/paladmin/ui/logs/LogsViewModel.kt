package com.paladmin.ui.logs

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.model.SftpLogDefaults
import com.paladmin.data.repository.ServerRepository
import com.paladmin.data.sftp.SftpLogClient
import com.paladmin.data.sftp.SftpLogResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LogSource(@StringRes val labelRes: Int) {
    PALDEFENDER(R.string.logs_tab_paldefender),
    UE4SS(R.string.logs_tab_ue4ss),
}

data class LogsUiState(
    val isLoading: Boolean = false,
    val lines: List<String> = emptyList(),
    val error: String? = null,
    /** (empreinte connue, empreinte observée) — non null tant que l'utilisateur n'a pas explicitement fait confiance à la nouvelle clé. */
    val hostKeyMismatch: Pair<String, String>? = null,
    val truncated: Boolean = false,
    val loadedOnce: Boolean = false,
)

/** Contrairement à LiveMapScreen (données déjà chargées, onglets = simple filtre côté client),
 * chaque onglet ici nécessite sa propre requête SFTP asynchrone : sélection ET état par onglet
 * vivent donc dans le ViewModel plutôt que dans un `remember` de l'écran, pour ne pas perdre le
 * fil si la sélection change pendant un chargement ou avant que le profil soit prêt. */
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val sftpLogClient: SftpLogClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _profile = MutableStateFlow<ServerProfile?>(null)
    val profile: StateFlow<ServerProfile?> = _profile.asStateFlow()

    private val _selectedTab = MutableStateFlow(LogSource.entries.first())
    val selectedTab: StateFlow<LogSource> = _selectedTab.asStateFlow()

    private val _states = MutableStateFlow(LogSource.entries.associateWith { LogsUiState() })
    val states: StateFlow<Map<LogSource, LogsUiState>> = _states.asStateFlow()

    init {
        viewModelScope.launch {
            _profile.value = serverRepository.getProfile(profileId)
            load(_selectedTab.value)
        }
    }

    fun selectTab(source: LogSource) {
        _selectedTab.value = source
        val current = _states.value[source]
        if (current != null && !current.loadedOnce && !current.isLoading) {
            load(source)
        }
    }

    fun refresh() {
        load(_selectedTab.value)
    }

    /** [trustNewKey] force l'acceptation de la clé hôte actuelle et la ré-épingle — utilisé
     * uniquement depuis l'action "Faire confiance à la nouvelle clé" après un [SftpLogResult.HostKeyMismatch]. */
    fun trustNewHostKey(source: LogSource) = load(source, trustNewKey = true)

    private fun load(source: LogSource, trustNewKey: Boolean = false) {
        val profile = _profile.value ?: return
        if (!profile.isSftpConfigured) return
        val path = pathFor(source, profile)

        updateState(source) { it.copy(isLoading = true, error = null, hostKeyMismatch = null) }
        viewModelScope.launch {
            val knownFingerprint = if (trustNewKey) null else profile.sftpHostKeyFingerprint
            when (
                val result = sftpLogClient.fetchLog(
                    // SFTP et API sur la même machine dans l'immense majorité des cas — pas de
                    // champ hôte séparé, [ServerProfile.host] est réutilisé directement.
                    host = profile.host,
                    port = profile.sftpPort,
                    username = profile.sftpUsername,
                    password = profile.sftpPassword,
                    remotePath = path,
                    knownHostKeyFingerprint = knownFingerprint,
                )
            ) {
                is SftpLogResult.Success -> {
                    if (profile.sftpHostKeyFingerprint != result.hostKeyFingerprint) {
                        serverRepository.updateSftpHostKeyFingerprint(profileId, result.hostKeyFingerprint)
                        _profile.value = profile.copy(sftpHostKeyFingerprint = result.hostKeyFingerprint)
                    }
                    updateState(source) {
                        it.copy(
                            isLoading = false,
                            lines = result.content.split("\n"),
                            truncated = result.truncated,
                            loadedOnce = true,
                        )
                    }
                }
                is SftpLogResult.HostKeyMismatch -> {
                    updateState(source) {
                        it.copy(isLoading = false, hostKeyMismatch = result.expectedFingerprint to result.actualFingerprint, loadedOnce = true)
                    }
                }
                is SftpLogResult.Failure -> {
                    updateState(source) { it.copy(isLoading = false, error = result.message, loadedOnce = true) }
                }
            }
        }
    }

    private fun pathFor(source: LogSource, profile: ServerProfile): String = when (source) {
        LogSource.PALDEFENDER -> profile.sftpPalDefenderLogPath.ifBlank { SftpLogDefaults.PALDEFENDER_LOG_PATH }
        LogSource.UE4SS -> profile.sftpUe4ssLogPath.ifBlank { SftpLogDefaults.UE4SS_LOG_PATH }
    }

    private fun updateState(source: LogSource, transform: (LogsUiState) -> LogsUiState) {
        _states.value = _states.value.toMutableMap().apply { this[source] = transform(this[source] ?: LogsUiState()) }
    }
}
