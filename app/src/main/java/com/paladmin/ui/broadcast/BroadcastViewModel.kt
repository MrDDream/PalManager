package com.paladmin.ui.broadcast

import com.paladmin.util.describe
import com.paladmin.util.requireSuccess

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.remote.paldefender.AlertRequest
import com.paladmin.data.remote.paldefender.BroadcastRequest
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.PlayerMessageRequest
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.remote.palworld.ShutdownRequest
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Contexte affiché autour du formulaire : nom du serveur (feuille Alimentation) et joueurs en
 * ligne (destinataires d'un message privé, nombre de joueurs touchés par une annonce). */
data class BroadcastUiState(
    val serverName: String = "",
    val players: List<PalworldPlayer> = emptyList(),
    /** Faux tant que la liste des joueurs n'a pas pu être lue (on n'affiche alors pas de compteur). */
    val playersLoaded: Boolean = false,
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    private val palworldClientFactory: PalworldClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _state = MutableStateFlow(BroadcastUiState())
    val state: StateFlow<BroadcastUiState> = _state.asStateFlow()

    init {
        loadContext()
    }

    /** Lecture best-effort : un échec ne bloque pas les envois (qui passent par PalDefender). */
    fun loadContext() {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            _state.value = _state.value.copy(serverName = profile.name)
            runCatching { palworldClientFactory.create(profile).getPlayers().players }
                .onSuccess { players -> _state.value = _state.value.copy(players = players, playersLoaded = true) }
        }
    }

    fun broadcast(message: String) = runAction(R.string.broadcast_msg_sent) { api -> api.broadcast(BroadcastRequest(message)) }

    fun alert(message: String) = runAction(R.string.broadcast_alert_sent) { api -> api.alert(AlertRequest(message)) }

    fun deleteBase(baseCampId: String) = runAction(R.string.broadcast_base_deleted) { api -> api.deleteBase(baseCampId) }

    fun reloadConfig() = runAction(R.string.manage_bc_reload_done) { api -> api.reloadConfig() }

    /** Même appel que la fiche joueur (PlayersViewModel.sendMessage). */
    fun sendPrivateMessage(player: PalworldPlayer, message: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .sendPlayerMessage(PlayerMessageRequest(userId = player.userId, message = message))
                    .requireSuccess()
            }.onSuccess {
                _statusMessage.value = context.getString(R.string.players_msg_message_sent_fmt, player.name)
            }.onFailure { error ->
                _statusMessage.value = error.describe(context.getString(R.string.players_error_message_failed))
            }
        }
    }

    /** Mêmes appels REST que la feuille Alimentation du dashboard (DashboardViewModel.shutdown/forceStop). */
    fun shutdown(waitTimeSeconds: Int, message: String) =
        runPalworldAction(R.string.dashboard_msg_shutdown_scheduled) { api -> api.shutdown(ShutdownRequest(waitTimeSeconds, message)) }

    fun forceStop() = runPalworldAction(R.string.dashboard_msg_stopped) { api -> api.stop() }

    private fun runAction(
        successMessageRes: Int,
        block: suspend (com.paladmin.data.remote.paldefender.PalDefenderApiService) -> retrofit2.Response<*>,
    ) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { block(palDefenderClientFactory.create(profile)).requireSuccess() }
                .onSuccess { _statusMessage.value = context.getString(successMessageRes) }
                .onFailure { error -> _statusMessage.value = error.describe(context.getString(R.string.broadcast_error_action_failed)) }
        }
    }

    private fun runPalworldAction(
        successMessageRes: Int,
        block: suspend (com.paladmin.data.remote.palworld.PalworldApiService) -> retrofit2.Response<*>,
    ) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { block(palworldClientFactory.create(profile)).requireSuccess() }
                .onSuccess { _statusMessage.value = context.getString(successMessageRes) }
                .onFailure { error -> _statusMessage.value = error.describe(context.getString(R.string.dashboard_error_action_failed)) }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
