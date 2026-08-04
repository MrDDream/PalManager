package com.paladmin.ui.itempicker

import com.paladmin.util.describe

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.db.ItemEntity
import com.paladmin.data.remote.paldefender.GiveItemEntry
import com.paladmin.data.remote.paldefender.GiveItemsRequest
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.repository.ItemRepository
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemRepository: ItemRepository,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _players = MutableStateFlow<List<PalworldPlayer>>(emptyList())
    val players: StateFlow<List<PalworldPlayer>> = _players.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val categories: StateFlow<List<String>> = itemRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<ItemEntity>> = combine(_query, _selectedCategory) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        itemRepository.search(query, category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPlayers()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun loadPlayers() {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palworldClientFactory.create(profile).getPlayers().players }
                .onSuccess { _players.value = it }
        }
    }

    fun giveItem(item: ItemEntity, playerIdentifier: String, amount: Int) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .giveItems(playerIdentifier, GiveItemsRequest(items = listOf(GiveItemEntry(itemId = item.id, count = amount))))
            }.onSuccess {
                _statusMessage.value = context.getString(R.string.item_given_fmt, item.nameFr, amount)
            }.onFailure { error ->
                _statusMessage.value = error.describe(context.getString(R.string.item_error_send_failed))
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
