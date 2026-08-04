package com.paladmin.data.remote.palworld

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PalworldInfo(
    val version: String = "",
    val servername: String = "",
    val description: String = "",
)

@Serializable
data class PalworldPlayersResponse(
    val players: List<PalworldPlayer> = emptyList(),
)

@Serializable
data class PalworldPlayer(
    val name: String = "",
    val playerId: String = "",
    val userId: String = "",
    val ip: String = "",
    val ping: Double = 0.0,
    @SerialName("location_x") val locationX: Double = 0.0,
    @SerialName("location_y") val locationY: Double = 0.0,
    val level: Int = 0,
    @SerialName("building_count") val buildingCount: Int = 0,
)

@Serializable
data class PalworldMetrics(
    val serverfps: Int = 0,
    val currentplayernum: Int = 0,
    val serverframetime: Double = 0.0,
    val maxplayernum: Int = 0,
    val uptime: Long = 0,
)

/** Structure de /settings très large et versionnée par le jeu : gardée en JSON brut, affichée clé/valeur. */
typealias PalworldSettings = JsonObject

@Serializable
data class AnnounceRequest(val message: String)

@Serializable
data class KickBanRequest(val userid: String, val message: String = "")

@Serializable
data class UnbanRequest(val userid: String)

@Serializable
data class ShutdownRequest(val waittime: Int = 30, val message: String = "")
