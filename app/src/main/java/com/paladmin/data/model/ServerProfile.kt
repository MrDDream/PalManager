package com.paladmin.data.model

data class ServerProfile(
    val id: Long = 0,
    val name: String,
    val iconKey: String = "dns",
    /** Hôte partagé par les deux API : sur un même serveur, IP Palworld et PalDefender sont toujours identiques. */
    val host: String,
    val palworldPort: Int = 8212,
    val palworldPassword: String = "",
    val palDefenderPort: Int = 17993,
    val palDefenderToken: String = "",
)
