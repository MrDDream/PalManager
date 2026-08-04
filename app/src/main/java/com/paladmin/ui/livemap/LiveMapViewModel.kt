package com.paladmin.ui.livemap

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.repository.ServerRepository
import com.paladmin.util.describe
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveMapPlayer(
    val name: String,
    val level: Int,
    val xPercent: Float,
    val yPercent: Float,
)

data class LiveMapBaseMarker(
    val xPercent: Float,
    val yPercent: Float,
    val onWorldTree: Boolean,
)

data class LiveMapUiState(
    val isLoading: Boolean = true,
    val worldPlayers: List<LiveMapPlayer> = emptyList(),
    val treePlayers: List<LiveMapPlayer> = emptyList(),
    val focusMarker: LiveMapBaseMarker? = null,
    val errorMessage: String? = null,
)

private const val POLL_INTERVAL_MS = 5000L

@HiltViewModel
class LiveMapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val focusMarker: LiveMapBaseMarker? = run {
        val x = savedStateHandle.get<String>("focusX")?.toDoubleOrNull()
        val y = savedStateHandle.get<String>("focusY")?.toDoubleOrNull()
        if (x == null || y == null) return@run null
        val onTree = isOnWorldTree(x, y)
        val (left, top) = if (onTree) worldTreeToScreenPercent(x, y) else worldToScreenPercent(x, y)
        LiveMapBaseMarker(left, top, onTree)
    }

    private val _state = MutableStateFlow(LiveMapUiState(focusMarker = focusMarker))
    val state: StateFlow<LiveMapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId)
            if (profile == null) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = context.getString(R.string.livemap_error_profile_not_found))
                return@launch
            }
            val api = palworldClientFactory.create(profile)
            while (true) {
                runCatching { api.getPlayers().players }
                    .onSuccess { players ->
                        val world = mutableListOf<LiveMapPlayer>()
                        val tree = mutableListOf<LiveMapPlayer>()
                        players.forEach { player ->
                            if (isOnWorldTree(player.locationX, player.locationY)) {
                                val (left, top) = worldTreeToScreenPercent(player.locationX, player.locationY)
                                tree += LiveMapPlayer(player.name, player.level, left, top)
                            } else {
                                val (left, top) = worldToScreenPercent(player.locationX, player.locationY)
                                world += LiveMapPlayer(player.name, player.level, left, top)
                            }
                        }
                        _state.value = _state.value.copy(
                            isLoading = false,
                            worldPlayers = world,
                            treePlayers = tree,
                            errorMessage = null,
                        )
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = error.describe(context.getString(R.string.livemap_error_connection)),
                        )
                    }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
}
