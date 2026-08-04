package com.paladmin.ui.players

import com.paladmin.util.describe

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.dataset.TechnologyCatalog
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.PlayerItemsResponse
import com.paladmin.data.remote.paldefender.PlayerMessageRequest
import com.paladmin.data.remote.paldefender.PlayerPalsResponse
import com.paladmin.data.remote.paldefender.PlayerProgressionResponse
import com.paladmin.data.remote.paldefender.TechsResponse
import com.paladmin.data.remote.palworld.KickBanRequest
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.repository.ItemRepository
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayersUiState(
    val isLoading: Boolean = true,
    val players: List<PalworldPlayer> = emptyList(),
    val statusMessage: String? = null,
)

enum class PlayerDetailKind(@StringRes val titleRes: Int) {
    INVENTORY(R.string.player_detail_inventory),
    TEAM(R.string.player_detail_team),
    PROGRESSION(R.string.player_detail_progression),
    TECHS(R.string.player_detail_techs),
}

data class InventoryGridItem(val imagePath: String, val label: String, val quantity: Int? = null)

sealed interface PlayerDetailRow {
    data class Section(val text: String) : PlayerDetailRow
    data class WithImage(val imagePath: String, val text: String) : PlayerDetailRow
    data class Plain(val text: String) : PlayerDetailRow
    data class Grid(val items: List<InventoryGridItem>) : PlayerDetailRow
}

data class PlayerDetailUiState(
    val kind: PlayerDetailKind,
    val playerName: String,
    val isLoading: Boolean = true,
    val rows: List<PlayerDetailRow> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PlayersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    private val technologyCatalog: TechnologyCatalog,
    private val itemRepository: ItemRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _state = MutableStateFlow(PlayersUiState())
    val state: StateFlow<PlayersUiState> = _state.asStateFlow()

    private val _detail = MutableStateFlow<PlayerDetailUiState?>(null)
    val detail: StateFlow<PlayerDetailUiState?> = _detail.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { palworldClientFactory.create(profile).getPlayers().players }
                .onSuccess { players -> _state.value = _state.value.copy(isLoading = false, players = players) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        statusMessage = error.describe(context.getString(R.string.players_error_load_failed)),
                    )
                }
        }
    }

    fun kick(player: PalworldPlayer, reason: String) = runPalworldAction(context.getString(R.string.players_msg_kicked_fmt, player.name)) { api ->
        api.kick(KickBanRequest(userid = player.userId, message = reason))
    }

    fun ban(player: PalworldPlayer, reason: String) = runPalworldAction(context.getString(R.string.players_msg_banned_fmt, player.name)) { api ->
        api.ban(KickBanRequest(userid = player.userId, message = reason))
    }

    fun sendMessage(player: PalworldPlayer, message: String) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching {
                palDefenderClientFactory.create(profile)
                    .sendPlayerMessage(PlayerMessageRequest(userId = player.userId, message = message))
            }.onSuccess {
                _state.value = _state.value.copy(statusMessage = context.getString(R.string.players_msg_message_sent_fmt, player.name))
            }.onFailure { error ->
                _state.value = _state.value.copy(statusMessage = error.describe(context.getString(R.string.players_error_message_failed)))
            }
        }
    }

    fun openDetail(player: PalworldPlayer, kind: PlayerDetailKind) {
        _detail.value = PlayerDetailUiState(kind = kind, playerName = player.name)
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            runCatching {
                when (kind) {
                    PlayerDetailKind.INVENTORY -> formatInventory(api.getItems(player.userId))
                    PlayerDetailKind.TEAM -> formatTeam(api.getPals(player.userId))
                    PlayerDetailKind.PROGRESSION -> formatProgression(api.getProgression(player.userId))
                    PlayerDetailKind.TECHS -> formatTechs(api.getTechs(player.userId))
                }
            }.onSuccess { rows ->
                _detail.value = _detail.value?.copy(isLoading = false, rows = rows)
            }.onFailure { error ->
                _detail.value = _detail.value?.copy(isLoading = false, error = error.describe(context.getString(R.string.players_error_detail_load_failed)))
            }
        }
    }

    fun dismissDetail() {
        _detail.value = null
    }

    private suspend fun formatInventory(response: PlayerItemsResponse): List<PlayerDetailRow> {
        val inventory = response.inventory ?: return listOf(PlayerDetailRow.Plain(context.getString(R.string.inventory_unavailable)))
        val sections = listOf(
            context.getString(R.string.inventory_section_items) to inventory.items,
            context.getString(R.string.inventory_section_key_items) to inventory.keyItems,
            context.getString(R.string.inventory_section_weapons) to inventory.weapons,
            context.getString(R.string.inventory_section_armor) to inventory.armor,
            context.getString(R.string.inventory_section_food) to inventory.food,
            context.getString(R.string.inventory_section_dropslot) to inventory.dropSlot,
        )
        val rows = mutableListOf<PlayerDetailRow>()
        sections.forEach { (label, container) ->
            val slots = container?.slots?.values?.filter { it.itemId.isNotBlank() }.orEmpty()
            if (slots.isNotEmpty()) {
                rows += PlayerDetailRow.Section("$label (${container?.usedSlots ?: 0}/${container?.maxSlots ?: 0})")
                rows += PlayerDetailRow.Grid(
                    slots.map {
                        val itemName = itemRepository.getById(it.itemId)?.nameFr?.ifBlank { it.itemId } ?: it.itemId
                        InventoryGridItem(imagePath = "file:///android_asset/images/items/${it.itemId}.webp", label = itemName, quantity = it.count)
                    },
                )
            }
        }
        return rows.ifEmpty { listOf(PlayerDetailRow.Plain(context.getString(R.string.inventory_empty))) }
    }

    // Les Pals Boss/Alpha ont un PalID préfixé "BOSS_" (ex. "BOSS_Foxcicle") qui ne correspond à
    // aucune image bundlée (nos assets sont nommés d'après l'espèce de base) — on retire ce préfixe
    // uniquement pour résoudre l'icône, le texte affiché garde le PalID complet.
    private fun basePalImageId(palId: String): String = palId.removePrefix("BOSS_")

    private fun formatTeam(response: PlayerPalsResponse): List<PlayerDetailRow> {
        val team = response.pals.team.values
        if (team.isEmpty()) return listOf(PlayerDetailRow.Plain(context.getString(R.string.team_empty)))
        return team.map { pal ->
            val text = buildString {
                append("${pal.nickname.ifBlank { pal.palId }} · Nv.${pal.level}")
                if (pal.shiny) append(" · ✨ ${context.getString(R.string.team_shiny)}")
                if (pal.gender.isNotBlank()) append(" · ${pal.gender}")
            }
            PlayerDetailRow.WithImage(imagePath = "file:///android_asset/images/pals/${basePalImageId(pal.palId)}.webp", text = text)
        }
    }

    private fun formatProgression(response: PlayerProgressionResponse): List<PlayerDetailRow> {
        val progression = response.progression
        val bosses = progression.bosses
        val captures = progression.captures
        val activities = progression.activities
        val rows = mutableListOf<PlayerDetailRow>()

        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_level_fmt, progression.player.level))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_exp_fmt, progression.player.exp))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_unused_points_fmt, progression.player.unusedStatusPoints))

        rows += PlayerDetailRow.Section(context.getString(R.string.progression_section_currencies))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_tech_points_fmt, progression.currencies.technologyPoints))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_ancient_tech_points_fmt, progression.currencies.ancientTechnologyPoints))
        if (progression.currencies.relics.isNotEmpty()) {
            rows += PlayerDetailRow.Plain(context.getString(R.string.progression_relics_fmt, progression.currencies.relics.values.sum()))
        }

        rows += PlayerDetailRow.Section(context.getString(R.string.progression_section_bosses))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_boss_total_fmt, bosses.totalBossDefeatCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_predator_fmt, bosses.predatorDefeatCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_tower_boss_fmt, bosses.towerBossDefeatCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_raid_boss_fmt, bosses.raidBossDefeatCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_normal_boss_fmt, bosses.normalBossDefeatFlags.values.count { it }))

        rows += PlayerDetailRow.Section(context.getString(R.string.progression_section_captures))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_tribe_capture_fmt, captures.tribeCaptureCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_pal_capture_fmt, captures.palCaptureCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_pal_capture_bonus_fmt, captures.palCaptureBonusCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_pal_butcher_fmt, captures.palButcherCounts.values.sum()))

        rows += PlayerDetailRow.Section(context.getString(R.string.progression_section_activities))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_craft_fmt, activities.craftItemCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_normal_dungeon_fmt, activities.normalDungeonClearCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_fixed_dungeon_fmt, activities.fixedDungeonClearCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_oilrig_fmt, activities.oilrigClearCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_pal_rankup_fmt, activities.palRankUpCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_arena_fmt, activities.arenaSoloClearCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_npc_talk_fmt, activities.npcTalkCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_fishing_fmt, activities.fishingCounts.values.sum()))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_treasure_fmt, activities.foundTreasureCount))
        rows += PlayerDetailRow.Plain(context.getString(R.string.progression_camp_conquered_fmt, activities.campConqueredCount))
        rows += PlayerDetailRow.Plain(
            context.getString(
                R.string.progression_first_fishing_fmt,
                context.getString(if (activities.firstFishingComplete) R.string.common_yes else R.string.common_no),
            ),
        )

        return rows
    }

    private suspend fun formatTechs(response: TechsResponse): List<PlayerDetailRow> {
        val header = context.getString(R.string.techs_unlocked_fmt, response.meta.unlockedCount, response.meta.totalCount)
        val rows = mutableListOf<PlayerDetailRow>(PlayerDetailRow.Plain(header))

        val resolved = response.techs.unlocked.mapNotNull { id -> technologyCatalog.get(id) }
        resolved.groupBy { it.level }.toSortedMap().forEach { (level, techs) ->
            rows += PlayerDetailRow.Section(context.getString(R.string.techs_level_fmt, level))
            rows += PlayerDetailRow.Grid(
                techs.map { InventoryGridItem(imagePath = "file:///android_asset/images/technologies/${it.image}", label = it.nameFr.ifBlank { it.id }) },
            )
        }
        return rows
    }

    private fun runPalworldAction(
        successMessage: String,
        block: suspend (com.paladmin.data.remote.palworld.PalworldApiService) -> Any,
    ) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { block(palworldClientFactory.create(profile)) }
                .onSuccess {
                    _state.value = _state.value.copy(statusMessage = successMessage)
                    refresh()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(statusMessage = error.describe(context.getString(R.string.players_error_action_failed)))
                }
        }
    }

    fun consumeStatusMessage() {
        _state.value = _state.value.copy(statusMessage = null)
    }
}
