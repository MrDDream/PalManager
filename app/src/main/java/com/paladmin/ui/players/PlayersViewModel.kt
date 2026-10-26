package com.paladmin.ui.players

import com.paladmin.util.basePalImageId
import com.paladmin.util.describe
import com.paladmin.util.formatThousands
import com.paladmin.util.pickLocalizedName
import com.paladmin.util.requireSuccess
import com.paladmin.util.resolvePalPassive
import com.paladmin.util.resolvePalSkill
import com.paladmin.util.translatePalGender

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.dataset.ActiveSkillCatalog
import com.paladmin.data.local.dataset.PassiveSkillCatalog
import com.paladmin.data.local.dataset.TechnologyCatalog
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.remote.paldefender.PalDefenderApiService
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.PlayerItemsResponse
import com.paladmin.data.remote.paldefender.PlayerMessageRequest
import com.paladmin.data.remote.paldefender.PlayerPalInstance
import com.paladmin.data.remote.paldefender.PlayerProgressionResponse
import com.paladmin.data.remote.paldefender.TechsResponse
import com.paladmin.data.remote.palworld.KickBanRequest
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.data.repository.HumanRepository
import com.paladmin.data.repository.ItemRepository
import com.paladmin.data.repository.PalRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.ui.components.DetailRow
import com.paladmin.ui.components.InventoryGridItem
import com.paladmin.ui.components.PalInfo
import com.paladmin.ui.components.PalIvInfo
import com.paladmin.ui.components.PalSoulInfo
import com.paladmin.ui.components.ToneKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tri de la liste des joueurs (chips "Niveau / Latence / Nom" de la maquette). */
enum class PlayerSort { LEVEL, PING, NAME }

data class PlayersUiState(
    val isLoading: Boolean = true,
    val players: List<PalworldPlayer> = emptyList(),
    /** Capacité du serveur (GET /metrics), pour le badge "x/max" — null si indisponible. */
    val maxPlayers: Int? = null,
    val query: String = "",
    val sort: PlayerSort = PlayerSort.LEVEL,
    val statusMessage: String? = null,
) {
    /** Joueurs filtrés (nom, ID ou IP) puis triés selon [sort]. */
    val visiblePlayers: List<PalworldPlayer> get() = filterAndSortPlayers(players, query, sort)
}

fun filterAndSortPlayers(players: List<PalworldPlayer>, query: String, sort: PlayerSort): List<PalworldPlayer> {
    val q = query.trim()
    val filtered = if (q.isEmpty()) {
        players
    } else {
        players.filter {
            it.name.contains(q, ignoreCase = true) || it.userId.contains(q, ignoreCase = true) ||
                it.playerId.contains(q, ignoreCase = true) || it.ip.contains(q, ignoreCase = true)
        }
    }
    return when (sort) {
        PlayerSort.LEVEL -> filtered.sortedWith(compareByDescending<PalworldPlayer> { it.level }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        PlayerSort.PING -> filtered.sortedBy { it.ping }
        PlayerSort.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}

/** Onglets de la fiche joueur (maquette "Fiche joueur"). */
enum class PlayerTab(@StringRes val labelRes: Int) {
    TEAM(R.string.players_v2_tab_team),
    INVENTORY(R.string.players_v2_tab_inventory),
    PALBOX(R.string.players_v2_tab_palbox),
    PROGRESSION(R.string.players_v2_tab_progress),
    TECHS(R.string.players_v2_tab_techs),
}

/** Onglet rendu en liste de [DetailRow] (inventaire, progression, technologies). */
data class DetailTabState(
    val isLoading: Boolean = true,
    val rows: List<DetailRow> = emptyList(),
    val error: String? = null,
)

/** Équipe et Palbox viennent du même appel (GET /pals) : chargés ensemble. */
data class PalsTabState(
    val isLoading: Boolean = true,
    val team: List<PalInfo> = emptyList(),
    val palbox: List<PalInfo> = emptyList(),
    val error: String? = null,
)

/** Fiche joueur ouverte. Un onglet à null n'a pas encore été demandé (chargé à la première visite). */
data class PlayerSheetUiState(
    val player: PalworldPlayer,
    val tab: PlayerTab,
    val pals: PalsTabState? = null,
    val inventory: DetailTabState? = null,
    val progression: DetailTabState? = null,
    val techs: DetailTabState? = null,
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
    private val humanRepository: HumanRepository,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    private val _state = MutableStateFlow(PlayersUiState())
    val state: StateFlow<PlayersUiState> = _state.asStateFlow()

    private val _sheet = MutableStateFlow<PlayerSheetUiState?>(null)
    val sheet: StateFlow<PlayerSheetUiState?> = _sheet.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palworldClientFactory.create(profile)
            runCatching { api.getPlayers().players }
                .onSuccess { players -> _state.value = _state.value.copy(isLoading = false, players = players) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        statusMessage = error.describe(context.getString(R.string.players_error_load_failed)),
                    )
                }
            // Capacité du serveur pour le badge "x/max" : secondaire, un échec n'est pas signalé.
            runCatching { api.getMetrics().maxplayernum }
                .onSuccess { max -> _state.value = _state.value.copy(maxPlayers = max.takeIf { it > 0 }) }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun onSortChange(sort: PlayerSort) {
        _state.value = _state.value.copy(sort = sort)
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

    // --- Fiche joueur -------------------------------------------------------------------------

    /** Ouvre la fiche de [player] sur [tab] (les boutons Inventaire/Équipe/... de la carte). */
    fun openSheet(player: PalworldPlayer, tab: PlayerTab) {
        val current = _sheet.value
        _sheet.value = if (current != null && current.player.userId == player.userId) {
            current.copy(player = player, tab = tab)
        } else {
            PlayerSheetUiState(player = player, tab = tab)
        }
        ensureLoaded(tab)
    }

    fun selectTab(tab: PlayerTab) {
        val current = _sheet.value ?: return
        _sheet.value = current.copy(tab = tab)
        ensureLoaded(tab)
    }

    /** Relance le chargement de l'onglet courant après une erreur. */
    fun retryTab() {
        val current = _sheet.value ?: return
        _sheet.value = when (current.tab) {
            PlayerTab.TEAM, PlayerTab.PALBOX -> current.copy(pals = null)
            PlayerTab.INVENTORY -> current.copy(inventory = null)
            PlayerTab.PROGRESSION -> current.copy(progression = null)
            PlayerTab.TECHS -> current.copy(techs = null)
        }
        ensureLoaded(current.tab)
    }

    fun dismissSheet() {
        _sheet.value = null
    }

    private fun ensureLoaded(tab: PlayerTab) {
        val current = _sheet.value ?: return
        val player = current.player
        when (tab) {
            PlayerTab.TEAM, PlayerTab.PALBOX -> if (current.pals == null) loadPals(player)
            PlayerTab.INVENTORY -> if (current.inventory == null) {
                loadDetail(player, { it.copy(inventory = DetailTabState()) }, { s, v -> s.copy(inventory = v) }) { api, language ->
                    formatInventory(api.getItems(player.userId), language)
                }
            }
            PlayerTab.PROGRESSION -> if (current.progression == null) {
                loadDetail(player, { it.copy(progression = DetailTabState()) }, { s, v -> s.copy(progression = v) }) { api, _ ->
                    formatProgression(api.getProgression(player.userId))
                }
            }
            PlayerTab.TECHS -> if (current.techs == null) {
                loadDetail(player, { it.copy(techs = DetailTabState()) }, { s, v -> s.copy(techs = v) }) { api, language ->
                    formatTechs(api.getTechs(player.userId), language)
                }
            }
        }
    }

    /** Applique [transform] à la fiche seulement si elle concerne toujours [userId] (une réponse
     * tardive d'un joueur précédent ne doit pas écraser la fiche d'un autre). */
    private fun updateSheet(userId: String, transform: (PlayerSheetUiState) -> PlayerSheetUiState) {
        val current = _sheet.value ?: return
        if (current.player.userId == userId) _sheet.value = transform(current)
    }

    private fun loadDetail(
        player: PalworldPlayer,
        markLoading: (PlayerSheetUiState) -> PlayerSheetUiState,
        set: (PlayerSheetUiState, DetailTabState) -> PlayerSheetUiState,
        load: suspend (PalDefenderApiService, AppLanguage) -> List<DetailRow>,
    ) {
        updateSheet(player.userId, markLoading)
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            val language = appPreferences.language.first()
            runCatching { load(api, language) }
                .onSuccess { rows -> updateSheet(player.userId) { set(it, DetailTabState(isLoading = false, rows = rows)) } }
                .onFailure { error ->
                    val message = error.describe(context.getString(R.string.players_error_detail_load_failed))
                    updateSheet(player.userId) { set(it, DetailTabState(isLoading = false, error = message)) }
                }
        }
    }

    private fun loadPals(player: PalworldPlayer) {
        updateSheet(player.userId) { it.copy(pals = PalsTabState()) }
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            val language = appPreferences.language.first()
            runCatching {
                val response = api.getPals(player.userId)
                formatPals(response.pals.team.values, language) to formatPals(response.pals.palbox.values, language)
            }.onSuccess { (team, palbox) ->
                updateSheet(player.userId) { it.copy(pals = PalsTabState(isLoading = false, team = team, palbox = palbox)) }
            }.onFailure { error ->
                val message = error.describe(context.getString(R.string.players_error_detail_load_failed))
                updateSheet(player.userId) { it.copy(pals = PalsTabState(isLoading = false, error = message)) }
            }
        }
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
                rows += DetailRow.Section(label, trailing = "${container?.usedSlots ?: 0}/${container?.maxSlots ?: 0}")
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

    private suspend fun formatPals(pals: Collection<PlayerPalInstance>, language: AppLanguage): List<PalInfo> = pals.map { pal ->
        // PalID sert aussi aux PNJ humains asservis comme ouvriers (humans.json, ex.
        // "Male_Trader01_v25") — on tente Pal d'abord, puis Humain, comme pour les camps de guilde.
        val palEntry = palRepository.getById(basePalImageId(pal.palId))
        val (speciesName, imagePath) = if (palEntry != null) {
            pickLocalizedName(palEntry.nameFr, palEntry.nameEn, language) to "file:///android_asset/images/pals/${palEntry.image}"
        } else {
            val humanEntry = humanRepository.getById(pal.palId)
            val name = humanEntry?.let { pickLocalizedName(it.nameFr, it.nameEn, language) } ?: pal.palId
            val image = humanEntry?.image?.let { "file:///android_asset/images/humans/$it" }
                ?: "file:///android_asset/images/humans/${pal.palId}.webp"
            name to image
        }
        PalInfo(
            imagePath = imagePath,
            speciesName = speciesName,
            nickname = pal.nickname,
            level = pal.level,
            genderRaw = pal.gender,
            gender = translatePalGender(context, pal.gender),
            shiny = pal.shiny,
            isBoss = pal.palId.startsWith("boss_", ignoreCase = true),
            workerSick = pal.workerSick,
            element = palEntry?.element1,
            exp = pal.exp,
            activeSkills = pal.activeSkills.map { resolvePalSkill(activeSkillCatalog, itemRepository, it, language) },
            passives = pal.passives.map { resolvePalPassive(passiveSkillCatalog, itemRepository, it, language) },
            iv = pal.ivs?.let { PalIvInfo(it.health, maxOf(it.attackMelee, it.attackShot), it.defense) },
            soul = pal.palSouls?.let { PalSoulInfo(it.health, it.attack, it.defense, it.craftSpeed) },
        )
    }

    private fun formatProgression(response: PlayerProgressionResponse): List<DetailRow> {
        val progression = response.progression
        val bosses = progression.bosses
        val captures = progression.captures
        val activities = progression.activities

        fun stat(@StringRes labelRes: Int, value: Int) = context.getString(labelRes) to formatThousands(value)

        val rows = mutableListOf<DetailRow>()

        // Chiffres clés en tête (maquette) — retirés des sections ci-dessous pour ne pas les répéter.
        rows += DetailRow.Kpis(
            listOf(
                stat(R.string.progression_label_level, progression.player.level),
                stat(R.string.progression_label_boss_total, bosses.totalBossDefeatCount),
                stat(R.string.progression_label_pal_capture, captures.palCaptureCounts.values.sum()),
            ),
        )

        rows += DetailRow.Section(context.getString(R.string.progression_section_player), Icons.Filled.TrendingUp, tone = ToneKey.Green)
        rows += DetailRow.Stats(
            listOf(
                context.getString(R.string.progression_label_exp) to formatThousands(progression.player.exp),
                stat(R.string.progression_label_unused_points, progression.player.unusedStatusPoints),
            ),
            tone = ToneKey.Green,
        )

        rows += DetailRow.Section(context.getString(R.string.progression_section_currencies), Icons.Filled.Diamond, tone = ToneKey.Amber)
        rows += DetailRow.Stats(
            listOfNotNull(
                stat(R.string.progression_label_tech_points, progression.currencies.technologyPoints),
                stat(R.string.progression_label_ancient_tech_points, progression.currencies.ancientTechnologyPoints),
                progression.currencies.relics.takeIf { it.isNotEmpty() }
                    ?.let { stat(R.string.progression_label_relics, it.values.sum()) },
            ),
            tone = ToneKey.Amber,
        )

        rows += DetailRow.Section(context.getString(R.string.progression_section_bosses), Icons.Filled.Warning, tone = ToneKey.Danger)
        rows += DetailRow.Stats(
            listOf(
                stat(R.string.progression_label_tower_boss, bosses.towerBossDefeatCounts.values.sum()),
                stat(R.string.progression_label_raid_boss, bosses.raidBossDefeatCounts.values.sum()),
                stat(R.string.progression_label_normal_boss, bosses.normalBossDefeatFlags.values.count { it }),
                stat(R.string.progression_label_predator, bosses.predatorDefeatCount),
            ),
            tone = ToneKey.Danger,
        )

        rows += DetailRow.Section(context.getString(R.string.progression_section_captures), Icons.Filled.Pets, tone = ToneKey.Sky)
        rows += DetailRow.Stats(
            listOf(
                stat(R.string.progression_label_tribe_capture, captures.tribeCaptureCount),
                stat(R.string.progression_label_pal_capture_bonus, captures.palCaptureBonusCounts.values.sum()),
                stat(R.string.progression_label_pal_butcher, captures.palButcherCounts.values.sum()),
            ),
            tone = ToneKey.Sky,
        )

        rows += DetailRow.Section(context.getString(R.string.progression_section_activities), Icons.Filled.Construction, tone = ToneKey.Purple)
        rows += DetailRow.Stats(
            listOf(
                stat(R.string.progression_label_craft, activities.craftItemCounts.values.sum()),
                stat(R.string.progression_label_normal_dungeon, activities.normalDungeonClearCount),
                stat(R.string.progression_label_fixed_dungeon, activities.fixedDungeonClearCount),
                stat(R.string.progression_label_oilrig, activities.oilrigClearCount),
                stat(R.string.progression_label_pal_rankup, activities.palRankUpCounts.values.sum()),
                stat(R.string.progression_label_arena, activities.arenaSoloClearCounts.values.sum()),
                stat(R.string.progression_label_npc_talk, activities.npcTalkCounts.values.sum()),
                stat(R.string.progression_label_fishing, activities.fishingCounts.values.sum()),
                stat(R.string.progression_label_treasure, activities.foundTreasureCount),
                stat(R.string.progression_label_camp_conquered, activities.campConqueredCount),
                context.getString(R.string.progression_label_first_fishing) to
                    context.getString(if (activities.firstFishingComplete) R.string.common_yes else R.string.common_no),
            ),
            tone = ToneKey.Purple,
        )

        return rows
    }

    private suspend fun formatTechs(response: TechsResponse, language: AppLanguage): List<DetailRow> {
        val rows = mutableListOf<DetailRow>(
            DetailRow.Progress(
                label = context.getString(R.string.players_v2_techs_unlocked),
                current = response.meta.unlockedCount,
                total = response.meta.totalCount,
            ),
        )
        val unlockedLabel = context.getString(R.string.players_v2_tech_unlocked)
        val lockedLabel = context.getString(R.string.players_v2_tech_locked)

        // Comparaison insensible à la casse : certains ids de techno.json sont en MAJUSCULES tel
        // que scrapés sur paldb.cc (ex. "PALBOX") alors que l'API PalDefender renvoie une casse
        // différente pour ce même id (ex. "Palbox") — un Set.contains strict ratait ces entrées et
        // les affichait verrouillées alors qu'elles étaient bien débloquées en jeu.
        val unlocked = response.techs.unlocked.mapTo(mutableSetOf()) { it.uppercase() }
        technologyCatalog.all().groupBy { it.level }.toSortedMap().forEach { (level, techs) ->
            rows += DetailRow.Section(context.getString(R.string.techs_level_fmt, level))
            techs.sortedBy { it.isAncient }.forEach {
                val isUnlocked = it.id.uppercase() in unlocked
                // Technologies "ancienne"/boss (points de technologie ancienne) : vignette violette.
                rows += DetailRow.WithImage(
                    imagePath = "file:///android_asset/images/technologies/${it.image}",
                    text = pickLocalizedName(it.nameFr, it.nameEn, language).ifBlank { it.id },
                    status = if (isUnlocked) unlockedLabel else lockedLabel,
                    positive = isUnlocked,
                    tint = if (it.isAncient) ToneKey.Purple else null,
                )
            }
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
