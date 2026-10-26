package com.paladmin.ui.guilds

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.ui.theme.PalAmber
import com.paladmin.ui.theme.PalDanger
import com.paladmin.ui.theme.PalGreen
import com.paladmin.ui.theme.PalSky
import com.paladmin.data.remote.paldefender.GuildCampDetail
import com.paladmin.data.remote.paldefender.GuildChestSlotRaw
import com.paladmin.data.remote.paldefender.GuildExpeditionsRaw
import com.paladmin.data.remote.paldefender.GuildLaboratoryRaw
import com.paladmin.data.remote.paldefender.PalDefenderClientFactory
import com.paladmin.data.remote.paldefender.WorldPos
import com.paladmin.data.local.db.PalEntity
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
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.util.basePalImageId
import com.paladmin.util.describe
import com.paladmin.util.formatThousands
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
    val baseCount: Int = basePositions.size,
    val camps: List<GuildCampSummary> = emptyList(),
    /** Emplacements occupés / totaux du coffre de guilde, quand le détail les fournit. */
    val chestUsed: Int? = null,
    val chestMax: Int? = null,
    val expeditionsFinished: Int? = null,
    val labCompleted: Int? = null,
    val labInProgress: Int? = null,
)

/** Aperçu d'un camp sur la carte de guilde (le détail complet reste dans PalGridDialog). */
data class GuildCampSummary(
    val level: Int,
    val palCount: Int,
    /** Images des premiers Pals du camp seulement (aperçu, voir [CAMP_PREVIEW_COUNT]). */
    val palImages: List<String>,
)

/** Nombre de Pals montrés en aperçu par camp ; le reste est résumé par "+N". */
const val CAMP_PREVIEW_COUNT = 5

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

/** Les ids de recherche de labo de guilde sont "<code métier><niveau>[_<variante>]" (ex.
 * "Mining3_2", "EmitFlame1") avec les mêmes codes métier que PalWorkSuitability (voir
 * [PalLabels.JOB_LABEL_RES]) — sauf "GenerateEnergy" qui désigne le même métier que
 * "GenerateElectricity" côté suitability (nom différent, même job en jeu). On réutilise ce
 * catalogue déjà traduit plutôt que de maintenir une seconde table de noms de métier qui dérive
 * (c'était le bug précédent : les mots-clés de catégorie ne correspondaient plus aux ids réels). */
private val LAB_JOB_ID_ALIASES: Map<String, String> = mapOf("GenerateEnergy" to "GenerateElectricity")

/** Codes métier reconnus dans un id de recherche, du plus long au plus court pour éviter qu'un
 * préfixe court n'intercepte à tort un id plus spécifique. */
private val LAB_JOB_CODES: List<String> =
    (PalLabels.JOB_LABEL_RES.keys + LAB_JOB_ID_ALIASES.keys).sortedByDescending { it.length }

private val LAB_JOB_STYLE: Map<String, Pair<ImageVector, Color>> = mapOf(
    "Seeding" to (Icons.Filled.Grass to PalGreen),
    "Watering" to (Icons.Filled.WaterDrop to PalSky),
    "EmitFlame" to (Icons.Filled.LocalFireDepartment to PalDanger),
    "GenerateElectricity" to (Icons.Filled.Bolt to PalAmber),
    "Handcraft" to (Icons.Filled.Handyman to PalAmber),
    "Collection" to (Icons.Filled.Eco to PalGreen),
    "Deforest" to (Icons.Filled.Forest to PalGreen),
    "Mining" to (Icons.Filled.Terrain to PalAmber),
    "OilExtraction" to (Icons.Filled.LocalGasStation to PalDanger),
    "ProductMedicine" to (Icons.Filled.LocalHospital to PalSky),
    "Cool" to (Icons.Filled.AcUnit to PalSky),
    "Transport" to (Icons.Filled.LocalShipping to PalGreen),
    "MonsterFarm" to (Icons.Filled.Pets to PalAmber),
)

/** Ordre d'affichage fixe des métiers de labo (les 9 métiers du jeu + "Autres" en dernier pour
 * un id non reconnu), plus lisible qu'un tri alphabétique qui mélangerait les métiers dans un
 * ordre arbitraire selon la langue. */
private val LAB_JOB_ORDER: List<String?> = LAB_JOB_CODES.map { LAB_JOB_ID_ALIASES[it] ?: it }.distinct() + listOf(null)

private data class LabResearchInfo(val jobCode: String?, val level: Int?, val subIndex: Int?)

/** Découpe un id de recherche en métier/niveau/variante (ex. "Mining3_2" -> métier "Mining",
 * niveau 3, variante 2) pour permettre de grouper par métier et d'afficher son vrai nom (déjà
 * traduit via [PalLabels.JOB_LABEL_RES]) plutôt que l'id technique brut. Niveau/variante ne sont
 * pas affichés tels quels (voir [formatLab]) mais servent au tri et au calcul de progression. */
private fun parseLabResearchId(id: String): LabResearchInfo {
    val rawCode = LAB_JOB_CODES.firstOrNull { id.startsWith(it) } ?: return LabResearchInfo(null, null, null)
    val jobCode = LAB_JOB_ID_ALIASES[rawCode] ?: rawCode
    val match = Regex("^(\\d+)(?:_(\\d+))?$").find(id.substring(rawCode.length)) ?: return LabResearchInfo(jobCode, null, null)
    return LabResearchInfo(jobCode, match.groupValues[1].toIntOrNull(), match.groupValues[2].toIntOrNull())
}

/** Nombre total de recherches par métier dans l'arbre du Labo de guilde, dénombré à la main sur le
 * diagramme communautaire paldb.cc/en/Pal_Labor_Research_Laboratory (toutes branches confondues :
 * vitesse de travail + branches annexes comme "Recherche accélérée" ou "Amélioration des Pals de
 * base"). Palworld/PalDefender ne publient aucun total officiel — cette estimation vient du
 * comptage des nœuds affichés sur ce diagramme, pas d'un catalogue officiel ; elle peut être décalée
 * de quelques unités si le diagramme regroupe des perks partagées différemment de l'API (d'où le
 * garde-fou `maxOf` dans [formatLab] si jamais le nombre entamé dépasse ce total). Absents de la
 * carte : Collecte/Transport/Exploitation, qui n'apparaissent pas sur ce diagramme (probablement
 * non boostables via le Labo) — ces métiers restent affichés sans dénominateur. */
/** Les 18 expéditions de camp de guilde, dans leur ordre réel en jeu (9 donjons, chacun en version
 * normale puis "Hard" — confirmé par un log de débogage réel : seules les premières de la liste
 * apparaissent dans `missions` tant que les suivantes ne sont pas révélées, dans cet ordre précis).
 * Ids vérifiés contre un log réseau réel (clés "DUNGEON_GRASS".."DUNGEON_WORLDTREEHARD", non
 * documentées par PalDefender). Noms FR/EN récupérés sur les fiches dédiées de paldb.cc (ex.
 * paldb.cc/fr/Verdant_Hollow -> "Caverne des Plaines"), aucune traduction inventée. */
private val EXPEDITION_CATALOG: List<Triple<String, String, String>> = listOf(
    Triple("DUNGEON_GRASS", "Caverne des Plaines", "Verdant Hollow"),
    Triple("DUNGEON_FOREST", "Zone Reculée de la Forêt", "Secret Realm of the Forest"),
    Triple("DUNGEON_VOLCANO", "Grotte Brûlante du Volcan", "Blazing Cavern"),
    Triple("DUNGEON_DESERT", "Ruines Cachées du Désert", "Hidden Sanctum of the Desert"),
    Triple("DUNGEON_SNOW", "Grotte Gelée de la Montagne", "Astral Frost Cavern"),
    Triple("DUNGEON_SAKURAJIMA", "Grotte Mystique de l'Île aux Cerisiers", "Celestial Sakura Cavern"),
    Triple("DUNGEON_DARKISLAND", "Crypte du Paradis Déchu", "Dark Cave of Feybreak"),
    Triple("DUNGEON_SKYISLAND", "Îles Astrales", "Sunreach Isle"),
    Triple("DUNGEON_WORLDTREE", "Ruines de la Cité Souterraine de l'Arbre-Monde", "World Tree Subterrenean City Ruins"),
    Triple("DUNGEON_GRASSHARD", "Hangar de Contrebande du Syndicat de Rayne", "Rayne Syndicate Smuggling Warehouse"),
    Triple("DUNGEON_FORESTHARD", "Marché Noir de la Ligue pour la Protection des Pals", "Free Pal Alliance Illicit Trading Post"),
    Triple("DUNGEON_VOLCANOHARD", "Marché Noir de la Confrérie des Flammes Éternelles", "Eternal Pyre's Forbidden Market"),
    Triple("DUNGEON_DESERTHARD", "Usine Illégale de la Milice Populaire", "PIDF Illegal Factory"),
    Triple("DUNGEON_SNOWHARD", "Laboratoire de l'Unité de Recherche Génétique", "PAL Genetic Research Laboratory"),
    Triple("DUNGEON_SAKURAJIMAHARD", "Base Secrète du Clan des Fleurs Lunaires", "Moonflower's Secret Hideout"),
    Triple("DUNGEON_DARKISLANDHARD", "Vestiges du Paradis Déchu", "Ancient Feybreak Ruins"),
    Triple("DUNGEON_SKYISLANDHARD", "Antre de la Carcasse du Dragon des Terres Astrales", "Sunreach Dragon Husk"),
    Triple("DUNGEON_WORLDTREEHARD", "Domaine Scellé de l'Arbre-Monde", "The World Tree's Forbidden Area"),
)

private const val EXPEDITION_UNLOCKED_MARK = "✓"
private const val EXPEDITION_LOCKED_MARK = "✕"

private val LAB_JOB_MAX_RESEARCH: Map<String, Int> = mapOf(
    "Handcraft" to 22,
    "EmitFlame" to 20,
    "Watering" to 18,
    "Seeding" to 19,
    "GenerateElectricity" to 18,
    "Deforest" to 18,
    "Mining" to 19,
    "Cool" to 19,
    "ProductMedicine" to 15,
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
            val language = appPreferences.language.first()
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
                                    baseCount = guild.camps.size,
                                    camps = detail?.camps.orEmpty().map { camp ->
                                        GuildCampSummary(
                                            level = camp.level,
                                            palCount = camp.pals.size,
                                            palImages = camp.pals.values.take(CAMP_PREVIEW_COUNT)
                                                .map { resolveCampPal(it.palId, language).imagePath },
                                        )
                                    },
                                    chestUsed = (detail?.items?.get("current") as? JsonPrimitive)?.intOrNull,
                                    chestMax = (detail?.items?.get("max") as? JsonPrimitive)?.intOrNull,
                                    expeditionsFinished = detail?.expeditions?.finished,
                                    // Même règle que formatLab : terminée = 100 %, le reste est en cours.
                                    labCompleted = detail?.laboratory?.researches?.values?.count { it.percentage >= 1.0 },
                                    labInProgress = detail?.laboratory?.researches?.values?.count { it.percentage < 1.0 },
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
                    GuildDetailKind.EXPEDITIONS -> formatExpeditions(detail?.expeditions, language)
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
                val resolved = resolveCampPal(pal.palId, language)
                val palEntry = resolved.palEntry
                val speciesName = resolved.speciesName
                val imagePath = resolved.imagePath
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

    private class ResolvedCampPal(val speciesName: String, val imagePath: String, val palEntry: PalEntity?)

    /** pal_id sert à la fois aux Pals (pals.json) et aux PNJ humains asservis comme ouvriers de
     * camp (humans.json, ex. "Male_Trader01_v25") — npc_id vaut "None" pour ces derniers, ce n'est
     * pas le champ à utiliser. On tente Pal d'abord, puis Humain. */
    private suspend fun resolveCampPal(palId: String, language: AppLanguage): ResolvedCampPal {
        val palEntry = palRepository.getById(basePalImageId(palId))
        if (palEntry != null) {
            return ResolvedCampPal(
                speciesName = pickLocalizedName(palEntry.nameFr, palEntry.nameEn, language),
                imagePath = "file:///android_asset/images/pals/${palEntry.image}",
                palEntry = palEntry,
            )
        }
        val humanEntry = humanRepository.getById(palId)
        return ResolvedCampPal(
            speciesName = humanEntry?.let { pickLocalizedName(it.nameFr, it.nameEn, language) } ?: palId,
            imagePath = humanEntry?.image?.let { "file:///android_asset/images/humans/$it" }
                ?: "file:///android_asset/images/humans/$palId.webp",
            palEntry = null,
        )
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

    private fun formatExpeditions(expeditions: GuildExpeditionsRaw?, language: AppLanguage): List<DetailRow> {
        if (expeditions == null) return listOf(DetailRow.Plain(context.getString(R.string.guild_expeditions_unavailable)))
        val rows = mutableListOf<DetailRow>(
            DetailRow.Section(context.getString(R.string.guild_expeditions_section_title), Icons.Filled.Flag, PalSky),
            DetailRow.Stats(
                listOf(context.getString(R.string.guild_expeditions_finished_label) to formatThousands(expeditions.finished)),
                accent = PalSky,
            ),
        )
        // Seules les missions déjà révélées apparaissent dans expeditions.missions (voir
        // EXPEDITION_CATALOG) — on les affiche dans l'ordre du jeu plutôt que celui, arbitraire, de
        // la map JSON.
        val revealed = EXPEDITION_CATALOG.mapNotNull { (id, nameFr, nameEn) ->
            expeditions.missions[id]?.let { unlocked -> Triple(id, pickLocalizedName(nameFr, nameEn, language), unlocked) }
        }
        if (revealed.isNotEmpty()) {
            rows += DetailRow.Section(context.getString(R.string.guild_expeditions_missions_title), Icons.Filled.Map, PalSky)
            revealed.forEach { (_, name, unlocked) ->
                val prefix = if (unlocked) EXPEDITION_UNLOCKED_MARK else EXPEDITION_LOCKED_MARK
                rows += DetailRow.Plain("$prefix $name", accent = if (unlocked) PalGreen else PalDanger)
            }
        }
        return rows
    }

    /** Palworld/PalDefender ne documentent nulle part le nom ni le bonus exact de chaque id de
     * recherche (vérifié PalDefender docs + paldb.cc + wiki communautaire — voir
     * [parseLabResearchId]) : la page dédiée de paldb.cc affiche bien un barème générique par
     * palier, mais avec moins de nœuds que ce que l'API expose réellement pour un même métier, donc
     * aucune correspondance fiable id -> palier/bonus n'existe. On se limite donc à un résumé par
     * métier plutôt que d'inventer un nom ou un pourcentage de bonus par entrée.
     *
     * L'API ne renvoie que les recherches déjà commencées (ni les verrouillées, ni la liste
     * complète possible pour ce métier) — `entries.size` n'est donc PAS le total réel des
     * recherches du métier, juste celles déjà entamées. Afficher "terminées/total entamé" donnerait
     * presque toujours 100 % (une recherche entamée est vite terminée) et masquerait qu'il reste
     * des recherches à faire : on sépare donc terminées et en cours plutôt qu'un ratio trompeur. */
    private fun formatLab(lab: GuildLaboratoryRaw?): List<DetailRow> {
        if (lab == null) return listOf(DetailRow.Plain(context.getString(R.string.guild_lab_unavailable)))
        val rows = mutableListOf<DetailRow>()
        rows += DetailRow.Section(
            if (lab.currentResearch.isNotBlank()) {
                context.getString(R.string.guild_lab_current_fmt, labJobLabel(lab.currentResearch))
            } else {
                context.getString(R.string.guild_lab_no_research)
            },
            Icons.Filled.Science,
            PalSky,
        )
        if (lab.researches.isNotEmpty()) {
            val grouped = lab.researches.entries.groupBy { parseLabResearchId(it.key).jobCode }
            LAB_JOB_ORDER.filter { grouped.containsKey(it) }.forEach { jobCode ->
                val entries = grouped.getValue(jobCode)
                val sectionLabel = jobCode?.let { context.getString(PalLabels.JOB_LABEL_RES.getValue(it)) }
                    ?: context.getString(R.string.guild_lab_category_other)
                val (icon, color) = jobCode?.let { LAB_JOB_STYLE[it] } ?: (Icons.Filled.Category to PalSky)
                val completed = entries.count { (_, progress) -> progress.percentage >= 1.0 }
                val inProgress = entries.size - completed
                val max = jobCode?.let { LAB_JOB_MAX_RESEARCH[it] }?.let { maxOf(it, completed) }
                val completedValue = if (max != null) "$completed/$max" else completed.toString()
                rows += DetailRow.Section(sectionLabel, icon, color)
                rows += DetailRow.Stats(
                    listOfNotNull(
                        context.getString(R.string.guild_lab_completed_label) to completedValue,
                        if (inProgress > 0) context.getString(R.string.guild_lab_in_progress_label) to inProgress.toString() else null,
                    ),
                    accent = color,
                )
            }
        }
        return rows
    }

    /** Nom du métier (déjà traduit) pour un id de recherche, sans niveau/bonus inventé faute de
     * source fiable (voir [formatLab]) — repli sur l'id humanisé si le métier n'a pas été identifié. */
    private fun labJobLabel(id: String): String =
        parseLabResearchId(id).jobCode?.let { context.getString(PalLabels.JOB_LABEL_RES.getValue(it)) } ?: prettifyId(id)

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
