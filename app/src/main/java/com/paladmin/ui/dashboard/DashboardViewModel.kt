package com.paladmin.ui.dashboard

import com.paladmin.util.describe

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldInfo
import com.paladmin.data.remote.palworld.PalworldMetrics
import com.paladmin.data.remote.palworld.ShutdownRequest
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: ServerProfile? = null,
    val info: PalworldInfo? = null,
    val metrics: PalworldMetrics? = null,
    val palDefenderVersion: String? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { appPreferences.setLastProfileId(profileId) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val profile = serverRepository.getProfile(profileId)
            if (profile == null) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = context.getString(R.string.dashboard_error_profile_not_found))
                return@launch
            }
            val palDefenderVersion = runCatching { palDefenderClientFactory.create(profile).getVersion().info.version }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

            runCatching {
                val api = palworldClientFactory.create(profile)
                val info = api.getInfo()
                val metrics = api.getMetrics()
                info to metrics
            }.onSuccess { (info, metrics) ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    profile = profile,
                    info = info,
                    metrics = metrics,
                    palDefenderVersion = palDefenderVersion,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    profile = profile,
                    palDefenderVersion = palDefenderVersion,
                    errorMessage = error.describe(context.getString(R.string.dashboard_error_connection)),
                )
            }
        }
    }

    fun save() = runAction(R.string.dashboard_msg_save_done) { api -> api.save() }

    fun shutdown(waitTimeSeconds: Int, message: String) =
        runAction(R.string.dashboard_msg_shutdown_scheduled) { api -> api.shutdown(ShutdownRequest(waitTimeSeconds, message)) }

    fun forceStop() = runAction(R.string.dashboard_msg_stopped) { api -> api.stop() }

    private fun runAction(successMessageRes: Int, block: suspend (com.paladmin.data.remote.palworld.PalworldApiService) -> Any) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            runCatching {
                val api = palworldClientFactory.create(profile)
                block(api)
            }.onSuccess {
                _state.value = _state.value.copy(actionMessage = context.getString(successMessageRes))
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.describe(context.getString(R.string.dashboard_error_action_failed)))
            }
        }
    }

    fun consumeMessages() {
        _state.value = _state.value.copy(errorMessage = null, actionMessage = null)
    }
}
