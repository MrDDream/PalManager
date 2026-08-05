package com.paladmin.data.remote.paldefender

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Schéma confirmé via la doc PalDefender (Endpoints/version) : {"Version":{"Version":"x.x.x",...}}
 * — imbriqué sous une clé "Version", pas un champ "version" plat comme on l'avait supposé au départ. */
@Serializable
data class PalDefenderVersionInfo(
    @SerialName("Version") val version: String = "",
    @SerialName("VersionLong") val versionLong: String = "",
    @SerialName("Beta") val beta: Boolean = false,
)

@Serializable
data class PalDefenderVersion(@SerialName("Version") val info: PalDefenderVersionInfo = PalDefenderVersionInfo())

@Serializable
data class PalDefenderPlayersResponse(val players: List<PalDefenderPlayer> = emptyList())

@Serializable
data class PalDefenderPlayer(
    val name: String = "",
    val uid: String = "",
    val steamId: String = "",
    val level: Int = 0,
    val guildId: String = "",
)

// Schémas ci-dessous confirmés en conditions réelles (repris du projet PalSite, qui appelle ces
// mêmes endpoints avec succès) : PascalCase côté JSON, endpoints /give/* en batch (liste), pas
// item-par-item.

@Serializable
data class GiveItemEntry(@SerialName("ItemID") val itemId: String, @SerialName("Count") val count: Int)

@Serializable
data class GiveItemsRequest(@SerialName("Items") val items: List<GiveItemEntry>)

@Serializable
data class GivePalEntry(@SerialName("PalID") val palId: String, @SerialName("Level") val level: Int)

@Serializable
data class GivePalRequest(@SerialName("Pals") val pals: List<GivePalEntry>)

@Serializable
data class GivePalTemplateRequest(@SerialName("PalTemplates") val palTemplates: List<String>)

@Serializable
data class GiveProgressionRequest(@SerialName("EXP") val exp: Int)

/** /give/paleggs n'a pas d'équivalent confirmé côté PalSite — schéma non vérifié en conditions réelles. */
@Serializable
data class GivePalEggRequest(val characterId: String, val amount: Int = 1)

@Serializable
data class TechRequest(val techId: String)

/** Schéma confirmé via la doc PalDefender (Endpoints/techs) : {"Meta":{...},"Techs":{"Unlocked":[...]}}. */
@Serializable
data class TechsMeta(
    @SerialName("UnlockedCount") val unlockedCount: Int = 0,
    @SerialName("LockedCount") val lockedCount: Int = 0,
    @SerialName("TotalCount") val totalCount: Int = 0,
)

@Serializable
data class TechsGroup(@SerialName("Unlocked") val unlocked: List<String> = emptyList())

@Serializable
data class TechsResponse(
    @SerialName("Meta") val meta: TechsMeta = TechsMeta(),
    @SerialName("Techs") val techs: TechsGroup = TechsGroup(),
)

/** Schéma confirmé via la doc PalDefender (Endpoints/progression). */
@Serializable
data class ProgressionPlayer(
    val level: Int = 0,
    val exp: Int = 0,
    val unusedStatusPoints: Int = 0,
)

@Serializable
data class ProgressionCurrencies(
    val relics: Map<String, Int> = emptyMap(),
    val technologyPoints: Int = 0,
    val ancientTechnologyPoints: Int = 0,
)

@Serializable
data class ProgressionBosses(
    val towerBossDefeatCounts: Map<String, Int> = emptyMap(),
    val normalBossDefeatFlags: Map<String, Boolean> = emptyMap(),
    val raidBossDefeatCounts: Map<String, Int> = emptyMap(),
    val totalBossDefeatCount: Int = 0,
    val predatorDefeatCount: Int = 0,
)

@Serializable
data class ProgressionCaptures(
    val tribeCaptureCount: Int = 0,
    val palCaptureCounts: Map<String, Int> = emptyMap(),
    val palCaptureBonusCounts: Map<String, Int> = emptyMap(),
    val palButcherCounts: Map<String, Int> = emptyMap(),
)

@Serializable
data class ProgressionActivities(
    val craftItemCounts: Map<String, Int> = emptyMap(),
    val normalDungeonClearCount: Int = 0,
    val fixedDungeonClearCount: Int = 0,
    val oilrigClearCount: Int = 0,
    val palRankUpCounts: Map<String, Int> = emptyMap(),
    val arenaSoloClearCounts: Map<String, Int> = emptyMap(),
    val npcTalkCounts: Map<String, Int> = emptyMap(),
    val fishingCounts: Map<String, Int> = emptyMap(),
    val foundTreasureCount: Int = 0,
    val campConqueredCount: Int = 0,
    val firstFishingComplete: Boolean = false,
)

@Serializable
data class ProgressionGroup(
    @SerialName("Player") val player: ProgressionPlayer = ProgressionPlayer(),
    @SerialName("Currencies") val currencies: ProgressionCurrencies = ProgressionCurrencies(),
    @SerialName("Bosses") val bosses: ProgressionBosses = ProgressionBosses(),
    @SerialName("Captures") val captures: ProgressionCaptures = ProgressionCaptures(),
    @SerialName("Activities") val activities: ProgressionActivities = ProgressionActivities(),
)

@Serializable
data class PlayerProgressionResponse(@SerialName("Progression") val progression: ProgressionGroup = ProgressionGroup())

@Serializable
data class PalIVs(
    @SerialName("Health") val health: Int = 0,
    @SerialName("AttackMelee") val attackMelee: Int = 0,
    @SerialName("AttackShot") val attackShot: Int = 0,
    @SerialName("Defense") val defense: Int = 0,
)

@Serializable
data class PalSoulRanks(
    @SerialName("Health") val health: Int = 0,
    @SerialName("Attack") val attack: Int = 0,
    @SerialName("Defense") val defense: Int = 0,
    @SerialName("CraftSpeed") val craftSpeed: Int = 0,
)

/** Schéma confirmé via la doc PalDefender (Endpoints/pals) — ExtraWorkSuitabilities/
 * DisableWorkPreferences restent ignorés (ignoreUnknownKeys=true), pas utiles à un affichage admin. */
@Serializable
data class PlayerPalInstance(
    @SerialName("PalID") val palId: String = "",
    @SerialName("Nickname") val nickname: String = "",
    @SerialName("Level") val level: Int = 0,
    @SerialName("Gender") val gender: String = "",
    @SerialName("Shiny") val shiny: Boolean = false,
    @SerialName("FriendshipPoints") val friendshipPoints: Int = 0,
    @SerialName("SAN") val sanity: Double = 0.0,
    @SerialName("PhysicalHealth") val physicalHealth: String = "",
    @SerialName("WorkerSick") val workerSick: String = "",
    @SerialName("ActiveSkills") val activeSkills: List<String> = emptyList(),
    @SerialName("LearntSkills") val learntSkills: List<String> = emptyList(),
    @SerialName("Passives") val passives: List<String> = emptyList(),
    @SerialName("HP") val hp: Double? = null,
    @SerialName("SP") val stamina: Double? = null,
    @SerialName("CraftSpeed") val craftSpeed: Int? = null,
    @SerialName("IVs") val ivs: PalIVs? = null,
    @SerialName("PalSouls") val palSouls: PalSoulRanks? = null,
)

@Serializable
data class PlayerPalsGroup(@SerialName("Team") val team: Map<String, PlayerPalInstance> = emptyMap())

@Serializable
data class PlayerPalsMeta(
    @SerialName("TeamCount") val teamCount: Int = 0,
    @SerialName("PalboxCount") val palboxCount: Int = 0,
    @SerialName("BaseCampCount") val baseCampCount: Int = 0,
)

@Serializable
data class PlayerPalsResponse(
    @SerialName("Meta") val meta: PlayerPalsMeta = PlayerPalsMeta(),
    @SerialName("Pals") val pals: PlayerPalsGroup = PlayerPalsGroup(),
)

@Serializable
data class ContainerSlot(@SerialName("ItemID") val itemId: String = "", @SerialName("Count") val count: Int = 0)

@Serializable
data class InventoryContainer(
    @SerialName("UsedSlots") val usedSlots: Int = 0,
    @SerialName("MaxSlots") val maxSlots: Int = 0,
    @SerialName("Slots") val slots: Map<String, ContainerSlot> = emptyMap(),
)

@Serializable
data class InventoryRaw(
    @SerialName("Items") val items: InventoryContainer? = null,
    @SerialName("KeyItems") val keyItems: InventoryContainer? = null,
    @SerialName("Weapons") val weapons: InventoryContainer? = null,
    @SerialName("Armor") val armor: InventoryContainer? = null,
    @SerialName("Food") val food: InventoryContainer? = null,
    @SerialName("DropSlot") val dropSlot: InventoryContainer? = null,
)

@Serializable
data class PlayerItemsResponse(@SerialName("Inventory") val inventory: InventoryRaw? = null)

@Serializable
data class GuildAdmin(val name: String = "")

@Serializable
data class WorldPos(val x: Double = 0.0, val y: Double = 0.0)

@Serializable
data class GuildCamp(val id: String = "", @SerialName("world_pos") val worldPos: WorldPos? = null)

/** Même schéma "Pal" (snake_case) que les Pals de base camp de GET /pals/{id} — juste une
 * convention de nommage différente de PlayerPalInstance (PascalCase, endpoint joueur). */
@Serializable
data class CampPalRaw(
    @SerialName("pal_id") val palId: String = "",
    @SerialName("npc_id") val npcId: String = "",
    val nickname: String = "",
    val gender: String = "",
    val level: Int = 0,
    val shiny: Boolean = false,
    @SerialName("phisical_health") val physicalHealth: String = "",
    @SerialName("worker_sick") val workerSick: String = "",
    val san: Double = 0.0,
    val friendship: Int = 0,
    @SerialName("active_skills") val activeSkills: List<String> = emptyList(),
    @SerialName("learnt_skills") val learntSkills: List<String> = emptyList(),
    val passives: List<String> = emptyList(),
)

/** Camp détaillé (endpoint GET /guild/{id} uniquement — le résumé de GET /guilds n'a que id/world_pos). */
@Serializable
data class GuildCampDetail(
    val id: String = "",
    val level: Int = 0,
    @SerialName("world_pos") val worldPos: WorldPos? = null,
    val state: String = "",
    val pals: Map<String, CampPalRaw> = emptyMap(),
)

@Serializable
data class GuildRaw(
    val name: String = "",
    @SerialName("Level") val level: Int = 1,
    val admin: GuildAdmin? = null,
    val camps: List<GuildCamp> = emptyList(),
)

/** Map keyée par guildId — PAS une liste, contrairement à la plupart des autres endpoints. */
@Serializable
data class GuildsResponse(@SerialName("Guilds") val guilds: Map<String, GuildRaw> = emptyMap())

@Serializable
data class GuildMemberRaw(
    @SerialName("player_uid") val playerUid: String = "",
    @SerialName("player_name") val playerName: String = "",
    val status: String = "",
)

/** L'objet "items" du coffre de guilde mélange des clés fixes (container_id/current/max) et des clés
 * numériques dynamiques (un emplacement par slot) — pas décodable en data class classique, on le
 * garde en JsonObject brut et on l'analyse à la main (voir GuildsViewModel.formatChest). */
@Serializable
data class GuildChestSlotRaw(@SerialName("item_id") val itemId: String = "", val count: Int = 0)

@Serializable
data class GuildExpeditionsRaw(
    val finished: Int = 0,
    val missions: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class GuildResearchProgress(
    @SerialName("work_amount") val workAmount: Double = 0.0,
    @SerialName("required_work_amount") val requiredWorkAmount: Double = 0.0,
    val percentage: Double = 0.0,
)

@Serializable
data class GuildLaboratoryRaw(
    @SerialName("current_research") val currentResearch: String = "",
    val researches: Map<String, GuildResearchProgress> = emptyMap(),
)

@Serializable
data class GuildDetailRaw(
    val name: String = "",
    @SerialName("Level") val level: Int = 1,
    val admin: GuildAdmin? = null,
    @SerialName("camp_count") val campCount: Int = 0,
    val members: List<GuildMemberRaw> = emptyList(),
    val camps: List<GuildCampDetail> = emptyList(),
    val items: JsonObject? = null,
    val expeditions: GuildExpeditionsRaw? = null,
    val laboratory: GuildLaboratoryRaw? = null,
)

@Serializable
data class GuildDetailResponse(@SerialName("Guild") val guild: GuildDetailRaw? = null)

/** /banlist n'a pas d'équivalent confirmé côté PalSite — schéma non vérifié en conditions réelles. */
@Serializable
data class BanListResponse(val bans: List<BanEntry> = emptyList())

@Serializable
data class BanEntry(val uid: String = "", val name: String = "", val reason: String = "")

/** /Broadcast non confirmé en direct (PalSite n'utilise que /Alert), mais même famille d'API : on aligne sur le même format {"Message": ...}. */
@Serializable
data class BroadcastRequest(@SerialName("Message") val message: String)

@Serializable
data class AlertRequest(@SerialName("Message") val message: String)

@Serializable
data class PlayerMessageRequest(
    @SerialName("SendType") val sendType: String = "PlayerChat",
    @SerialName("UserID") val userId: String,
    @SerialName("Message") val message: String,
)
