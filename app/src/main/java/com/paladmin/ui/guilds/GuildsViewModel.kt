package com.paladmin.ui.guilds

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.remote.paldefender.GuildCampDetail
import com.paladmin.data.remote.paldefender.GuildChestSlotRaw
import com.paladmin.data.remote.paldefender.GuildExpeditionsRaw
import com.paladmin.data.remote.paldefender.GuildLaboratoryRaw
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.WorldPos
import com.paladmin.data.local.dataset.ActiveSkillCatalog
import com.paladmin.data.local.dataset.PassiveSkillCatalog
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.repository.HumanRepository
import com.paladmin.data.repository.ItemRepository
import com.paladmin.data.repository.PalRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.ui.components.DetailRow
import com.paladmin.ui.components.InventoryGridItem
import com.paladmin.ui.components.PalGridUiState
import com.paladmin.ui.components.PalGroup
import com.paladmin.ui.components.PalInfo
import com.paladmin.util.basePalImageId
import com.paladmin.util.describe
import com.paladmin.util.pickLocalizedName
import com.paladmin.util.prettifyId
import com.paladmin.util.resolvePalPassive
import com.paladmin.util.resolvePalSkill
import com.paladmin.util.translatePalGender
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
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

enum class GuildDetailKind(@StringRes val titleRes: Int) {
    CHEST(R.string.guild_detail_chest),
    EXPEDITIONS(R.string.guild_detail_expeditions),
    LAB(R.string.guild_detail_lab),
}

data class GuildDetailUiState(
    val kind: GuildDetailKind,
    val guildName: String,
    val isLoading: Boolean = true,
    val rows: List<DetailRow> = emptyList(),
    val error: String? = null,
)

/** Mots-clés anglais de métier (convention d'id Palworld) -> catégorie de labo affichée, dans
 * l'ordre des 9 métiers du jeu. "Electric" avant "Electricity" par prudence sur la casse/troncature. */
private val LAB_RESEARCH_CATEGORY_KEYWORDS: List<Pair<String, Int>> = listOf(
    "Handiwork" to R.string.guild_lab_category_handiwork,
    "Kindling" to R.string.guild_lab_category_kindling,
    "Watering" to R.string.guild_lab_category_watering,
    "Planting" to R.string.guild_lab_category_planting,
    "Electric" to R.string.guild_lab_category_electricity,
    "Lumbering" to R.string.guild_lab_category_lumbering,
    "Mining" to R.string.guild_lab_category_mining,
    "Cooling" to R.string.guild_lab_category_cooling,
    "Medicine" to R.string.guild_lab_category_medicine,
)

@HiltViewModel
class GuildsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palDefenderClientFactory: PalDefenderClientFactory,
    private val activeSkillCatalog: ActiveSkillCatalog,
    private val passiveSkillCatalog: PassiveSkillCatalog,
    private val itemRepository: ItemRepository,
    private val palRepository: PalRepository,
    private val humanRepository: HumanRepository,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()
    private val json = Json { ignoreUnknownKeys = true }

    private val _allGuilds = MutableStateFlow<List<GuildListItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _detail = MutableStateFlow<GuildDetailUiState?>(null)
    val detail: StateFlow<GuildDetailUiState?> = _detail.asStateFlow()

    private val _palGrid = MutableStateFlow<PalGridUiState?>(null)
    val palGrid: StateFlow<PalGridUiState?> = _palGrid.asStateFlow()

    val state: StateFlow<GuildsUiState> = combine(_allGuilds, _isLoading, _statusMessage, _query) { guilds, isLoading, statusMessage, query ->
        val filtered = if (query.isBlank()) {
            guilds
        } else {
            guilds.filter { guild ->
                guild.name.contains(query, ignoreCase = true) ||
                    guild.members.any { it.name.contains(query, ignoreCase = true) }
            }
        }
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

    fun openDetail(guild: GuildListItem, kind: GuildDetailKind) {
        _detail.value = GuildDetailUiState(kind = kind, guildName = guild.name)
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            val language = appPreferences.language.first()
            runCatching {
                val detail = api.getGuild(guild.guildId).guild
                when (kind) {
                    GuildDetailKind.CHEST -> formatChest(detail?.items, language)
                    GuildDetailKind.EXPEDITIONS -> formatExpeditions(detail?.expeditions)
                    GuildDetailKind.LAB -> formatLab(detail?.laboratory)
                }
            }.onSuccess { rows ->
                _detail.value = _detail.value?.copy(isLoading = false, rows = rows)
            }.onFailure { error ->
                _detail.value = _detail.value?.copy(isLoading = false, error = error.describe(context.getString(R.string.guilds_error_detail_load_failed)))
            }
        }
    }

    fun dismissDetail() {
        _detail.value = null
    }

    fun openCampPals(guild: GuildListItem) {
        _palGrid.value = PalGridUiState(title = context.getString(R.string.guild_detail_camp_pals_title_fmt, guild.name))
        viewModelScope.launch {
            val profile = serverRepository.getProfile(profileId) ?: return@launch
            val api = palDefenderClientFactory.create(profile)
            val language = appPreferences.language.first()
            runCatching { formatCampPals(api.getGuild(guild.guildId).guild?.camps.orEmpty(), language) }
                .onSuccess { groups -> _palGrid.value = _palGrid.value?.copy(isLoading = false, groups = groups) }
                .onFailure { error ->
                    _palGrid.value = _palGrid.value?.copy(isLoading = false, error = error.describe(context.getString(R.string.guilds_error_detail_load_failed)))
                }
        }
    }

    fun dismissPalGrid() {
        _palGrid.value = null
    }

    private suspend fun formatCampPals(camps: List<GuildCampDetail>, language: AppLanguage): List<PalGroup> =
        camps.mapIndexed { index, camp ->
            val label = context.getString(R.string.guild_camp_label_fmt, index + 1, camp.level)
            val pals = camp.pals.values.map { pal ->
                // pal_id sert à la fois aux Pals (pals.json) et aux PNJ humains asservis comme
                // ouvriers de camp (humans.json, ex. "Male_Trader01_v25") — npc_id vaut "None" pour
                // ces derniers, ce n'est pas le champ à utiliser. On tente Pal d'abord, puis Humain.
                val palEntry = palRepository.getById(basePalImageId(pal.palId))
                val (speciesName, imagePath) = if (palEntry != null) {
                    val name = pickLocalizedName(palEntry.nameFr, palEntry.nameEn, language)
                    val image = "file:///android_asset/images/pals/${palEntry.image}"
                    name to image
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
                    activeSkills = pal.activeSkills.map { resolvePalSkill(activeSkillCatalog, itemRepository, it, language) },
                    passives = pal.passives.map { resolvePalPassive(passiveSkillCatalog, itemRepository, it, language) },
                )
            }
            PalGroup(label = label, pals = pals)
        }

    private suspend fun formatChest(items: JsonObject?, language: AppLanguage): List<DetailRow> {
        if (items == null) return listOf(DetailRow.Plain(context.getString(R.string.guild_chest_unavailable)))
        val knownKeys = setOf("container_id", "current", "max")
        val current = (items["current"] as? JsonPrimitive)?.intOrNull ?: 0
        val max = (items["max"] as? JsonPrimitive)?.intOrNull ?: 0
        val slots = items.entries
            .filter { it.key !in knownKeys }
            .mapNotNull { (_, value) -> runCatching { json.decodeFromJsonElement(GuildChestSlotRaw.serializer(), value) }.getOrNull() }
            .filter { it.itemId.isNotBlank() }
        if (slots.isEmpty()) return listOf(DetailRow.Plain(context.getString(R.string.guild_chest_empty)))
        val rows = mutableListOf<DetailRow>(DetailRow.Section(context.getString(R.string.guild_chest_slots_fmt, current, max)))
        rows += DetailRow.Grid(
            slots.map {
                val itemEntry = itemRepository.getById(it.itemId)
                val itemName = itemEntry?.let { entry -> pickLocalizedName(entry.nameFr, entry.nameEn, language) } ?: it.itemId
                val imagePath = itemEntry?.image?.let { image -> "file:///android_asset/images/items/$image" }
                    ?: "file:///android_asset/images/items/${it.itemId}.webp"
                InventoryGridItem(imagePath = imagePath, label = itemName, quantity = it.count)
            },
        )
        return rows
    }

    private fun formatExpeditions(expeditions: GuildExpeditionsRaw?): List<DetailRow> {
        if (expeditions == null) return listOf(DetailRow.Plain(context.getString(R.string.guild_expeditions_unavailable)))
        val released = expeditions.missions.count { it.value }
        return listOf(
            DetailRow.Plain(context.getString(R.string.guild_expeditions_finished_fmt, expeditions.finished)),
            DetailRow.Plain(context.getString(R.string.guild_expeditions_released_fmt, released, expeditions.missions.size)),
        )
    }

    private fun formatLab(lab: GuildLaboratoryRaw?): List<DetailRow> {
        if (lab == null) return listOf(DetailRow.Plain(context.getString(R.string.guild_lab_unavailable)))
        val rows = mutableListOf<DetailRow>()
        rows += DetailRow.Plain(
            if (lab.currentResearch.isNotBlank()) {
                context.getString(R.string.guild_lab_current_fmt, prettifyId(lab.currentResearch))
            } else {
                context.getString(R.string.guild_lab_no_research)
            },
        )
        if (lab.researches.isNotEmpty()) {
            val grouped = lab.researches.entries.groupBy { categorizeResearch(it.key) }
            grouped.toSortedMap().forEach { (category, entries) ->
                rows += DetailRow.Section(category)
                entries.sortedBy { it.key }.forEach { (id, progress) ->
                    val percent = (progress.percentage * 100).toInt()
                    rows += DetailRow.Plain(context.getString(R.string.guild_lab_progress_fmt, prettifyId(id), percent))
                }
            }
        }
        return rows
    }

    /** L'API PalDefender ne documente que des ids de recherche opaques (pas de catalogue nom/bonus
     * publié nulle part — vérifié PalDefender docs + paldb.cc + wiki communautaire) : on catégorise
     * par mot-clé de métier reconnu dans l'id (convention observée sur tous les autres ids Palworld
     * de ce dataset) et on humanise l'id brut en libellé lisible, sans inventer de texte de bonus. */
    private fun categorizeResearch(id: String): String {
        val category = LAB_RESEARCH_CATEGORY_KEYWORDS.firstOrNull { (keyword, _) -> id.contains(keyword, ignoreCase = true) }
        return context.getString(category?.second ?: R.string.guild_lab_category_other)
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
