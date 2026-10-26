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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.roundToInt

data class LiveMapPlayer(
    val name: String,
    val level: Int,
    val xPercent: Float,
    val yPercent: Float,
    /** Ping en ms tel que renvoyé par l'API REST (0 si inconnu). */
    val ping: Int = 0,
    /** Identifiant stable (couleur du marqueur) ; repli sur le nom. */
    val userId: String = "",
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
    val focusMarkers: List<LiveMapBaseMarker> = emptyList(),
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

    /** "x1,y1;x2,y2;..." — une guilde peut avoir plusieurs camps, voir NavRoutes.liveMap(). */
    private val focusMarkers: List<LiveMapBaseMarker> = savedStateHandle.get<String>("focusPoints")
        .orEmpty()
        .split(";")
        .mapNotNull { pair ->
            val (xRaw, yRaw) = pair.split(",").takeIf { it.size == 2 } ?: return@mapNotNull null
            val x = xRaw.toDoubleOrNull() ?: return@mapNotNull null
            val y = yRaw.toDoubleOrNull() ?: return@mapNotNull null
            val onTree = isOnWorldTree(x, y)
            val (left, top) = if (onTree) worldTreeToScreenPercent(x, y) else worldToScreenPercent(x, y)
            LiveMapBaseMarker(left, top, onTree)
        }

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    private val _state = MutableStateFlow(LiveMapUiState(focusMarkers = focusMarkers))
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
                                tree += LiveMapPlayer(player.name, player.level, left, top, player.ping.roundToInt(), player.userId)
                            } else {
                                val (left, top) = worldToScreenPercent(player.locationX, player.locationY)
                                world += LiveMapPlayer(player.name, player.level, left, top, player.ping.roundToInt(), player.userId)
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
                // Attend le prochain tick, ou une demande de rafraîchissement immédiat (bouton).
                withTimeoutOrNull(POLL_INTERVAL_MS) { refreshRequests.receive() }
            }
        }
    }

    /** Relance tout de suite une lecture des positions au lieu d'attendre le prochain tick. */
    fun refresh() {
        refreshRequests.trySend(Unit)
    }
}
