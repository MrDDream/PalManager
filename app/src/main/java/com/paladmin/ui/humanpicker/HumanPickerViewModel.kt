package com.paladmin.ui.humanpicker

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.db.HumanEntity
import com.paladmin.data.remote.paldefender.GivePalEntry
import com.paladmin.data.remote.paldefender.GivePalRequest
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.repository.HumanRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.util.describe
import com.paladmin.util.requireSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HumanPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val humanRepository: HumanRepository,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _players = MutableStateFlow<List<PalworldPlayer>>(emptyList())
    val players: StateFlow<List<PalworldPlayer>> = _players.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val results: StateFlow<List<HumanEntity>> = _query
        .flatMapLatest { query -> humanRepository.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPlayers()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun loadPlayers() {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palworldClientFactory.create(profile).getPlayers().players }
                .onSuccess { _players.value = it }
        }
    }

    // Un PNJ humain se donne avec exactement la même commande give/pals que les Pals — le
    // PalID est juste celui du personnage humain plutôt qu'une créature.
    fun giveHuman(human: HumanEntity, playerIdentifier: String, level: Int) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .givePal(playerIdentifier, GivePalRequest(pals = listOf(GivePalEntry(palId = human.id, level = level))))
                    .requireSuccess()
            }.onSuccess {
                _statusMessage.value = context.getString(R.string.pal_given_fmt, human.nameFr, level)
            }.onFailure { error ->
                _statusMessage.value = error.describe(context.getString(R.string.item_error_send_failed))
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
