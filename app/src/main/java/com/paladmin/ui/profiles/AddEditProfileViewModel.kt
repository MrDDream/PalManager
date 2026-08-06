package com.paladmin.ui.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.repository.ServerRepository
import com.paladmin.data.sftp.SftpBrowseResult
import com.paladmin.data.sftp.SftpLogClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CheckStatus { CHECKING, SUCCESS, FAILURE }
data class CheckResult(val status: CheckStatus, val message: String? = null)
data class VerifyState(
    val palworld: CheckResult,
    val palDefender: CheckResult,
    val sftp: CheckResult?,
) {
    val isChecking: Boolean
        get() = palworld.status == CheckStatus.CHECKING ||
            palDefender.status == CheckStatus.CHECKING ||
            sftp?.status == CheckStatus.CHECKING
}

data class ProfileFormState(
    val id: Long = 0,
    val name: String = "",
    val iconKey: String = "dns",
    val host: String = "",
    val palworldPort: String = "8212",
    val palworldPassword: String = "",
    val palDefenderPort: String = "17993",
    val palDefenderToken: String = "",
    val sftpPort: String = "22",
    val sftpUsername: String = "",
    val sftpPassword: String = "",
    val sftpPalDefenderLogPath: String = "",
    val sftpUe4ssLogPath: String = "",
    val sftpPalTemplatesPath: String = "",
    /** Pas éditable dans ce formulaire (géré depuis l'écran Logs) — juste transporté pour ne pas
     * l'effacer silencieusement à chaque sauvegarde du profil. */
    val sftpHostKeyFingerprint: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AddEditProfileViewModel @Inject constructor(
    private val repository: ServerRepository,
    private val sftpLogClient: SftpLogClient,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = savedStateHandle.get<String>("profileId")?.toLongOrNull()?.takeIf { it >= 0 } ?: -1
    val isNew: Boolean = profileId < 0

    private val _state = MutableStateFlow(ProfileFormState())
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    private val _verifyState = MutableStateFlow<VerifyState?>(null)
    val verifyState: StateFlow<VerifyState?> = _verifyState.asStateFlow()

    init {
        if (isNew) {
            _state.value = _state.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                repository.getProfile(profileId)?.let { profile ->
                    _state.value = ProfileFormState(
                        id = profile.id,
                        name = profile.name,
                        iconKey = profile.iconKey,
                        host = profile.host,
                        palworldPort = profile.palworldPort.toString(),
                        palworldPassword = profile.palworldPassword,
                        palDefenderPort = profile.palDefenderPort.toString(),
                        palDefenderToken = profile.palDefenderToken,
                        sftpPort = profile.sftpPort.toString(),
                        sftpUsername = profile.sftpUsername,
                        sftpPassword = profile.sftpPassword,
                        sftpPalDefenderLogPath = profile.sftpPalDefenderLogPath,
                        sftpUe4ssLogPath = profile.sftpUe4ssLogPath,
                        sftpPalTemplatesPath = profile.sftpPalTemplatesPath,
                        sftpHostKeyFingerprint = profile.sftpHostKeyFingerprint,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun update(transform: (ProfileFormState) -> ProfileFormState) {
        _state.value = transform(_state.value)
    }

    fun save(onSaved: () -> Unit) {
        val form = _state.value
        if (form.name.isBlank() || form.host.isBlank()) return
        viewModelScope.launch {
            repository.saveProfile(
                ServerProfile(
                    id = form.id,
                    name = form.name.trim(),
                    iconKey = form.iconKey,
                    host = form.host.trim(),
                    palworldPort = form.palworldPort.trim().toIntOrNull() ?: 8212,
                    // Le mot de passe n'est PAS trim() : un espace en fin de mot de passe
                    // pourrait être légitime côté serveur, on ne le modifie jamais silencieusement.
                    palworldPassword = form.palworldPassword,
                    palDefenderPort = form.palDefenderPort.trim().toIntOrNull() ?: 17993,
                    palDefenderToken = form.palDefenderToken,
                    sftpPort = form.sftpPort.trim().toIntOrNull() ?: 22,
                    sftpUsername = form.sftpUsername.trim(),
                    sftpPassword = form.sftpPassword,
                    sftpPalDefenderLogPath = form.sftpPalDefenderLogPath.trim(),
                    sftpUe4ssLogPath = form.sftpUe4ssLogPath.trim(),
                    sftpPalTemplatesPath = form.sftpPalTemplatesPath.trim(),
                    sftpHostKeyFingerprint = form.sftpHostKeyFingerprint,
                ),
            )
            onSaved()
        }
    }

    /** Sélecteur de chemin SFTP de la section Avancées — utilise l'hôte/identifiants du formulaire
     * en cours d'édition (pas encore forcément enregistrés). */
    suspend fun browseSftp(path: String): SftpBrowseResult {
        val form = _state.value
        return sftpLogClient.listDirectory(
            host = form.host.trim(),
            port = form.sftpPort.trim().toIntOrNull() ?: 22,
            username = form.sftpUsername.trim(),
            password = form.sftpPassword,
            path = path,
        )
    }

    /** Teste les identifiants du formulaire en cours d'édition (pas encore forcément enregistrés)
     * contre les vraies API, sans passer par un profil sauvegardé. */
    fun verify() {
        val form = _state.value
        val sftpConfigured = form.sftpUsername.isNotBlank() && form.sftpPassword.isNotBlank()
        _verifyState.value = VerifyState(
            palworld = CheckResult(CheckStatus.CHECKING),
            palDefender = CheckResult(CheckStatus.CHECKING),
            sftp = if (sftpConfigured) CheckResult(CheckStatus.CHECKING) else null,
        )

        val tempProfile = ServerProfile(
            id = form.id,
            name = form.name,
            iconKey = form.iconKey,
            host = form.host.trim(),
            palworldPort = form.palworldPort.trim().toIntOrNull() ?: 8212,
            palworldPassword = form.palworldPassword,
            palDefenderPort = form.palDefenderPort.trim().toIntOrNull() ?: 17993,
            palDefenderToken = form.palDefenderToken,
        )

        viewModelScope.launch {
            val palworldResult = runCatching { palworldClientFactory.create(tempProfile).getInfo() }
                .fold(
                    onSuccess = { CheckResult(CheckStatus.SUCCESS) },
                    onFailure = { CheckResult(CheckStatus.FAILURE, it.message) },
                )
            _verifyState.value = _verifyState.value?.copy(palworld = palworldResult)
        }

        viewModelScope.launch {
            val palDefenderResult = runCatching { palDefenderClientFactory.create(tempProfile).getVersion() }
                .fold(
                    onSuccess = { CheckResult(CheckStatus.SUCCESS) },
                    onFailure = { CheckResult(CheckStatus.FAILURE, it.message) },
                )
            _verifyState.value = _verifyState.value?.copy(palDefender = palDefenderResult)
        }

        if (sftpConfigured) {
            viewModelScope.launch {
                val sftpResult = when (val result = browseSftp(".")) {
                    is SftpBrowseResult.Success -> CheckResult(CheckStatus.SUCCESS)
                    is SftpBrowseResult.Failure -> CheckResult(CheckStatus.FAILURE, result.message)
                }
                _verifyState.value = _verifyState.value?.copy(sftp = sftpResult)
            }
        }
    }

    fun dismissVerify() {
        if (_verifyState.value?.isChecking == true) return
        _verifyState.value = null
    }
}
