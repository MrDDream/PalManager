package com.paladmin.ui.players

import com.paladmin.util.basePalImageId
import com.paladmin.util.describe
import com.paladmin.util.pickLocalizedName
import com.paladmin.util.requireSuccess
import com.paladmin.util.resolvePalPassiveName
import com.paladmin.util.resolvePalSkillName
import com.paladmin.util.translatePalGender

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.dataset.ActiveSkillCatalog
import com.paladmin.data.local.dataset.PassiveSkillCatalog
import com.paladmin.data.local.dataset.TechnologyCatalog
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.AppPreferences
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
import com.paladmin.data.repository.PalRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.ui.components.DetailRow
import com.paladmin.ui.components.InventoryGridItem
import com.paladmin.ui.components.PalGridUiState
import com.paladmin.ui.components.PalGroup
import com.paladmin.ui.components.PalInfo
import com.paladmin.ui.components.PalIvInfo
import com.paladmin.ui.components.PalSoulInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayersUiState(
    val isLoading: Boolean = true,
    val players: List<PalworldPlayer> = emptyList(),
    val statusMessage: String? = null,
)

enum class PlayerDetailKind(@StringRes val titleRes: Int) {
    INVENTORY(R.string.player_detail_inventory),
    PROGRESSION(R.string.player_detail_progression),
    TECHS(R.string.player_detail_techs),
}

data class PlayerDetailUiState(
    val kind: PlayerDetailKind,
    val playerName: String,
    val isLoading: Boolean = true,
    val rows: List<DetailRow> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PlayersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    private val technologyCatalog: TechnologyCatalog,
    private val activeSkillCatalog: ActiveSkillCatalog,
    private val passiveSkillCatalog: PassiveSkillCatalog,
    private val itemRepository: ItemRepository,
    private val palRepository: PalRepository,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _state = MutableStateFlow(PlayersUiState())
    val state: StateFlow<PlayersUiState> = _state.asStateFlow()

    private val _detail = MutableStateFlow<PlayerDetailUiState?>(null)
    val detail: StateFlow<PlayerDetailUiState?> = _detail.asStateFlow()

    private val _palGrid = MutableStateFlow<PalGridUiState?>(null)
    val palGrid: StateFlow<PalGridUiState?> = _palGrid.asStateFlow()

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
                    .requireSuccess()
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
            val language = appPreferences.language.first()
            runCatching {
                when (kind) {
                    PlayerDetailKind.INVENTORY -> formatInventory(api.getItems(player.userId), language)
                    PlayerDetailKind.PROGRESSION -> formatProgression(api.getProgression(player.userId))
                    PlayerDetailKind.TECHS -> formatTechs(api.getTechs(player.userId), language)
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

    fun openTeam(player: PalworldPlayer) {
        _palGrid.value = PalGridUiState(title = context.getString(R.string.player_detail_title_fmt, context.getString(R.string.player_detail_team), player.name))
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            val language = appPreferences.language.first()
            runCatching { formatTeamPals(api.getPals(player.userId), language) }
                .onSuccess { groups -> _palGrid.value = _palGrid.value?.copy(isLoading = false, groups = groups) }
                .onFailure { error ->
                    _palGrid.value = _palGrid.value?.copy(isLoading = false, error = error.describe(context.getString(R.string.players_error_detail_load_failed)))
                }
        }
    }

    fun dismissPalGrid() {
        _palGrid.value = null
    }

    private suspend fun formatInventory(response: PlayerItemsResponse, language: AppLanguage): List<DetailRow> {
        val inventory = response.inventory ?: return listOf(DetailRow.Plain(context.getString(R.string.inventory_unavailable)))
        val sections = listOf(
            context.getString(R.string.inventory_section_items) to inventory.items,
            context.getString(R.string.inventory_section_key_items) to inventory.keyItems,
            context.getString(R.string.inventory_section_weapons) to inventory.weapons,
            context.getString(R.string.inventory_section_armor) to inventory.armor,
            context.getString(R.string.inventory_section_food) to inventory.food,
            context.getString(R.string.inventory_section_dropslot) to inventory.dropSlot,
        )
        val rows = mutableListOf<DetailRow>()
        sections.forEach { (label, container) ->
            val slots = container?.slots?.values?.filter { it.itemId.isNotBlank() }.orEmpty()
            if (slots.isNotEmpty()) {
                rows += DetailRow.Section("$label (${container?.usedSlots ?: 0}/${container?.maxSlots ?: 0})")
                rows += DetailRow.Grid(
                    slots.map {
                        val itemEntry = itemRepository.getById(it.itemId)
                        val itemName = itemEntry?.let { entry -> pickLocalizedName(entry.nameFr, entry.nameEn, language) } ?: it.itemId
                        val imagePath = itemEntry?.image?.let { image -> "file:///android_asset/images/items/$image" }
                            ?: "file:///android_asset/images/items/${it.itemId}.webp"
                        InventoryGridItem(imagePath = imagePath, label = itemName, quantity = it.count)
                    },
                )
            }
        }
        return rows.ifEmpty { listOf(DetailRow.Plain(context.getString(R.string.inventory_empty))) }
    }

    private suspend fun formatTeamPals(response: PlayerPalsResponse, language: AppLanguage): List<PalGroup> {
        val team = response.pals.team.values
        val infos = team.map { pal ->
            val entry = palRepository.getById(basePalImageId(pal.palId))
            val speciesName = entry?.let { pickLocalizedName(it.nameFr, it.nameEn, language) } ?: pal.palId
            val imagePath = entry?.image?.let { "file:///android_asset/images/pals/$it" }
                ?: "file:///android_asset/images/pals/${basePalImageId(pal.palId)}.webp"
            PalInfo(
                imagePath = imagePath,
                speciesName = speciesName,
                nickname = pal.nickname,
                level = pal.level,
                gender = translatePalGender(context, pal.gender),
                shiny = pal.shiny,
                workerSick = pal.workerSick,
                activeSkills = pal.activeSkills.map { resolvePalSkillName(activeSkillCatalog, itemRepository, it, language) },
                passives = pal.passives.map { resolvePalPassiveName(passiveSkillCatalog, itemRepository, it, language) },
                iv = pal.ivs?.let { PalIvInfo(it.health, maxOf(it.attackMelee, it.attackShot), it.defense) },
                soul = pal.palSouls?.let { PalSoulInfo(it.health, it.attack, it.defense, it.craftSpeed) },
            )
        }
        return listOf(PalGroup(label = null, pals = infos))
    }

    private fun formatProgression(response: PlayerProgressionResponse): List<DetailRow> {
        val progression = response.progression
        val bosses = progression.bosses
        val captures = progression.captures
        val activities = progression.activities
        val rows = mutableListOf<DetailRow>()

        rows += DetailRow.Plain(context.getString(R.string.progression_level_fmt, progression.player.level))
        rows += DetailRow.Plain(context.getString(R.string.progression_exp_fmt, progression.player.exp))
        rows += DetailRow.Plain(context.getString(R.string.progression_unused_points_fmt, progression.player.unusedStatusPoints))

        rows += DetailRow.Section(context.getString(R.string.progression_section_currencies))
        rows += DetailRow.Plain(context.getString(R.string.progression_tech_points_fmt, progression.currencies.technologyPoints))
        rows += DetailRow.Plain(context.getString(R.string.progression_ancient_tech_points_fmt, progression.currencies.ancientTechnologyPoints))
        if (progression.currencies.relics.isNotEmpty()) {
            rows += DetailRow.Plain(context.getString(R.string.progression_relics_fmt, progression.currencies.relics.values.sum()))
        }

        rows += DetailRow.Section(context.getString(R.string.progression_section_bosses))
        rows += DetailRow.Plain(context.getString(R.string.progression_boss_total_fmt, bosses.totalBossDefeatCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_predator_fmt, bosses.predatorDefeatCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_tower_boss_fmt, bosses.towerBossDefeatCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_raid_boss_fmt, bosses.raidBossDefeatCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_normal_boss_fmt, bosses.normalBossDefeatFlags.values.count { it }))

        rows += DetailRow.Section(context.getString(R.string.progression_section_captures))
        rows += DetailRow.Plain(context.getString(R.string.progression_tribe_capture_fmt, captures.tribeCaptureCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_pal_capture_fmt, captures.palCaptureCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_pal_capture_bonus_fmt, captures.palCaptureBonusCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_pal_butcher_fmt, captures.palButcherCounts.values.sum()))

        rows += DetailRow.Section(context.getString(R.string.progression_section_activities))
        rows += DetailRow.Plain(context.getString(R.string.progression_craft_fmt, activities.craftItemCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_normal_dungeon_fmt, activities.normalDungeonClearCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_fixed_dungeon_fmt, activities.fixedDungeonClearCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_oilrig_fmt, activities.oilrigClearCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_pal_rankup_fmt, activities.palRankUpCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_arena_fmt, activities.arenaSoloClearCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_npc_talk_fmt, activities.npcTalkCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_fishing_fmt, activities.fishingCounts.values.sum()))
        rows += DetailRow.Plain(context.getString(R.string.progression_treasure_fmt, activities.foundTreasureCount))
        rows += DetailRow.Plain(context.getString(R.string.progression_camp_conquered_fmt, activities.campConqueredCount))
        rows += DetailRow.Plain(
            context.getString(
                R.string.progression_first_fishing_fmt,
                context.getString(if (activities.firstFishingComplete) R.string.common_yes else R.string.common_no),
            ),
        )

        return rows
    }

    private suspend fun formatTechs(response: TechsResponse, language: AppLanguage): List<DetailRow> {
        val header = context.getString(R.string.techs_unlocked_fmt, response.meta.unlockedCount, response.meta.totalCount)
        val rows = mutableListOf<DetailRow>(DetailRow.Plain(header))

        val resolved = response.techs.unlocked.mapNotNull { id -> technologyCatalog.get(id) }
        resolved.groupBy { it.level }.toSortedMap().forEach { (level, techs) ->
            rows += DetailRow.Section(context.getString(R.string.techs_level_fmt, level))
            rows += DetailRow.Grid(
                techs.map {
                    InventoryGridItem(
                        imagePath = "file:///android_asset/images/technologies/${it.image}",
                        label = pickLocalizedName(it.nameFr, it.nameEn, language).ifBlank { it.id },
                    )
                },
            )
        }
        return rows
    }

    private fun runPalworldAction(
        successMessage: String,
        block: suspend (com.paladmin.data.remote.palworld.PalworldApiService) -> retrofit2.Response<*>,
    ) {
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            runCatching { block(palworldClientFactory.create(profile)).requireSuccess() }
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
