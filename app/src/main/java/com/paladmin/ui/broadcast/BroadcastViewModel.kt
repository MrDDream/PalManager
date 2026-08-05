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
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun broadcast(message: String) = runAction(R.string.broadcast_msg_sent) { api -> api.broadcast(BroadcastRequest(message)) }

    fun alert(message: String) = runAction(R.string.broadcast_alert_sent) { api -> api.alert(AlertRequest(message)) }

    fun deleteBase(baseCampId: String) = runAction(R.string.broadcast_base_deleted) { api -> api.deleteBase(baseCampId) }

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

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
