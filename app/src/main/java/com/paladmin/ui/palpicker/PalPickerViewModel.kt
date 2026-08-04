package com.paladmin.ui.palpicker

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.dataset.PalWorkSuitabilityDto
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.remote.paldefender.GivePalEntry
import com.paladmin.data.remote.paldefender.GivePalRequest
import com.paladmin.data.remote.paldefender.GivePalTemplateRequest
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.repository.PalRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.util.describe
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val palPickerJson = Json { ignoreUnknownKeys = true }

private data class PalFilters(val query: String, val element: String?, val tier: PalLabels.RarityTier?, val job: String?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PalPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val palRepository: PalRepository,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedElement = MutableStateFlow<String?>(null)
    val selectedElement: StateFlow<String?> = _selectedElement.asStateFlow()

    private val _selectedRarityTier = MutableStateFlow<PalLabels.RarityTier?>(null)
    val selectedRarityTier: StateFlow<PalLabels.RarityTier?> = _selectedRarityTier.asStateFlow()

    private val _selectedJob = MutableStateFlow<String?>(null)
    val selectedJob: StateFlow<String?> = _selectedJob.asStateFlow()

    private val _players = MutableStateFlow<List<PalworldPlayer>>(emptyList())
    val players: StateFlow<List<PalworldPlayer>> = _players.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val results: StateFlow<List<PalEntity>> = combine(_query, _selectedElement, _selectedRarityTier, _selectedJob) { query, element, tier, job ->
        PalFilters(query, element, tier, job)
    }.flatMapLatest { filters ->
        palRepository.search(filters.query, filters.element, filters.tier?.min, filters.tier?.max).map { list ->
            val job = filters.job ?: return@map list
            list.filter { pal -> palHasJob(pal, job) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun palHasJob(pal: PalEntity, job: String): Boolean {
        val jobs = runCatching {
            palPickerJson.decodeFromString<List<PalWorkSuitabilityDto>>(pal.workSuitabilitiesJson)
        }.getOrDefault(emptyList())
        return jobs.any { it.job == job && it.level > 0 }
    }

    init {
        loadPlayers()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onElementSelected(element: String?) {
        _selectedElement.value = element
    }

    fun onRarityTierSelected(tier: PalLabels.RarityTier?) {
        _selectedRarityTier.value = tier
    }

    fun onJobSelected(job: String?) {
        _selectedJob.value = job
    }

    fun loadPlayers() {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palworldClientFactory.create(profile).getPlayers().players }
                .onSuccess { _players.value = it }
        }
    }

    fun givePal(pal: PalEntity, playerIdentifier: String, level: Int) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .givePal(playerIdentifier, GivePalRequest(pals = listOf(GivePalEntry(palId = pal.id, level = level))))
            }.onSuccess {
                _statusMessage.value = context.getString(R.string.pal_given_fmt, pal.nameFr, level)
            }.onFailure { error ->
                _statusMessage.value = error.describe(context.getString(R.string.item_error_send_failed))
            }
        }
    }

    fun givePalTemplate(templateName: String, playerIdentifier: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .givePalTemplate(playerIdentifier, GivePalTemplateRequest(palTemplates = listOf(templateName)))
            }.onSuccess {
                _statusMessage.value = context.getString(R.string.pal_template_given_fmt, templateName)
            }.onFailure { error ->
                _statusMessage.value = error.describe(context.getString(R.string.pal_error_template_failed))
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
