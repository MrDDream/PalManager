package com.paladmin.ui.guilds

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.WorldPos
import com.paladmin.data.repository.ServerRepository
import com.paladmin.util.describe
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuildMemberItem(val name: String, val online: Boolean, val isAdmin: Boolean)

data class GuildListItem(
    val guildId: String,
    val name: String,
    val level: Int,
    val adminName: String,
    val members: List<GuildMemberItem>,
    val basePositions: List<WorldPos>,
)

data class GuildsUiState(
    val isLoading: Boolean = true,
    val guilds: List<GuildListItem> = emptyList(),
    val statusMessage: String? = null,
)

@HiltViewModel
class GuildsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _allGuilds = MutableStateFlow<List<GuildListItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val state: StateFlow<GuildsUiState> = combine(_allGuilds, _isLoading, _statusMessage, _query) { guilds, isLoading, statusMessage, query ->
        val filtered = if (query.isBlank()) guilds else guilds.filter { it.name.contains(query, ignoreCase = true) }
        GuildsUiState(isLoading = isLoading, guilds = filtered, statusMessage = statusMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuildsUiState())

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            runCatching { api.getGuilds().guilds }
                .onSuccess { guildMap ->
                    val items = coroutineScope {
                        guildMap.map { (guildId, guild) ->
                            async {
                                val detail = runCatching { api.getGuild(guildId).guild }.getOrNull()
                                val adminName = guild.admin?.name.orEmpty()
                                GuildListItem(
                                    guildId = guildId,
                                    name = guild.name.ifBlank { context.getString(R.string.guilds_unnamed) },
                                    level = guild.level,
                                    adminName = adminName,
                                    members = detail?.members?.map {
                                        GuildMemberItem(
                                            name = it.playerName,
                                            online = it.status == "Online",
                                            isAdmin = it.playerName.isNotBlank() && it.playerName == adminName,
                                        )
                                    }.orEmpty(),
                                    basePositions = guild.camps.mapNotNull { it.worldPos },
                                )
                            }
                        }.awaitAll()
                    }.sortedBy { it.name }
                    _allGuilds.value = items
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _statusMessage.value = error.describe(context.getString(R.string.guilds_error_load_failed))
                }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
