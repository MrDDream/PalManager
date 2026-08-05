package com.paladmin.ui.bans

import com.paladmin.util.describe
import com.paladmin.util.requireSuccess

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.remote.paldefender.BanEntry
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.UnbanRequest
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BansUiState(
    val isLoading: Boolean = true,
    val bans: List<BanEntry> = emptyList(),
    val statusMessage: String? = null,
)

@HiltViewModel
class BansViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _state = MutableStateFlow(BansUiState())
    val state: StateFlow<BansUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palDefenderClientFactory.create(profile).getBanList().bans }
                .onSuccess { bans -> _state.value = _state.value.copy(isLoading = false, bans = bans) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        statusMessage = error.describe(context.getString(R.string.bans_error_load_failed)),
                    )
                }
        }
    }

    fun unbanPlayer(uid: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palworldClientFactory.create(profile).unban(UnbanRequest(userid = uid)).requireSuccess() }
                .onSuccess { _state.value = _state.value.copy(statusMessage = context.getString(R.string.bans_msg_unbanned)); refresh() }
                .onFailure { error -> _state.value = _state.value.copy(statusMessage = error.describe(context.getString(R.string.bans_error_generic))) }
        }
    }

    fun banIp(ip: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palDefenderClientFactory.create(profile).banIp(ip).requireSuccess() }
                .onSuccess { _state.value = _state.value.copy(statusMessage = context.getString(R.string.bans_msg_ip_banned_fmt, ip)) }
                .onFailure { error -> _state.value = _state.value.copy(statusMessage = error.describe(context.getString(R.string.bans_error_generic))) }
        }
    }

    fun unbanIp(ip: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palDefenderClientFactory.create(profile).unbanIp(ip).requireSuccess() }
                .onSuccess { _state.value = _state.value.copy(statusMessage = context.getString(R.string.bans_msg_ip_unbanned_fmt, ip)) }
                .onFailure { error -> _state.value = _state.value.copy(statusMessage = error.describe(context.getString(R.string.bans_error_generic))) }
        }
    }

    fun consumeStatusMessage() {
        _state.value = _state.value.copy(statusMessage = null)
    }
}
