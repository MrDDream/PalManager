package com.paladmin.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.data.remote.paldefender.BanEntry
import com.paladmin.data.remote.paldefender.WorldPos
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.bans.BansActions
import com.paladmin.ui.bans.BansContent
import com.paladmin.ui.bans.BansUiState
import com.paladmin.ui.broadcast.BroadcastActions
import com.paladmin.ui.broadcast.BroadcastContent
import com.paladmin.ui.broadcast.BroadcastMode
import com.paladmin.ui.broadcast.BroadcastUiState
import com.paladmin.ui.guilds.GuildCampSummary
import com.paladmin.ui.guilds.GuildListItem
import com.paladmin.ui.guilds.GuildMemberItem
import com.paladmin.ui.guilds.GuildsActions
import com.paladmin.ui.guilds.GuildsContent
import com.paladmin.ui.guilds.GuildsUiState
import com.paladmin.ui.livemap.LiveMapActions
import com.paladmin.ui.livemap.LiveMapBaseMarker
import com.paladmin.ui.livemap.LiveMapContent
import com.paladmin.ui.livemap.LiveMapPlayer
import com.paladmin.ui.livemap.LiveMapTab
import com.paladmin.ui.livemap.LiveMapUiState
import org.junit.Rule
import org.junit.Test

/** Écrans du groupe "Gérer" (Guildes, Carte, Bans, Diffusion) rendus sans ViewModel. */
class ManageScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    private fun pal(name: String) = "file:///android_asset/images/pals/$name.webp"

    private val guildsState = GuildsUiState(
        isLoading = false,
        guilds = listOf(
            GuildListItem(
                guildId = "g1",
                name = "Les Pattes Vertes",
                level = 12,
                adminName = "Dorian",
                members = listOf(
                    GuildMemberItem("Dorian", online = true, isAdmin = true),
                    GuildMemberItem("Aelys", online = true, isAdmin = false),
                    GuildMemberItem("Fennecoeur", online = false, isAdmin = false),
                    GuildMemberItem("Mirou", online = false, isAdmin = false),
                ),
                basePositions = listOf(WorldPos(-300_000.0, 100_000.0), WorldPos(-150_000.0, -200_000.0)),
                camps = listOf(
                    GuildCampSummary(18, 15, listOf("SheepBall", "Anubis", "Alpaca", "BlueDragon", "CatMage").map(::pal)),
                    GuildCampSummary(9, 4, listOf("ChickenPal", "Boar", "BerryGoat", "CactusDoll").map(::pal)),
                ),
                chestUsed = 142,
                chestMax = 240,
                expeditionsFinished = 31,
                labCompleted = 18,
                labInProgress = 1,
            ),
            GuildListItem("g2", "Clan Ryuu", 8, "Ryuujin", listOf(GuildMemberItem("Ryuujin", true, true), GuildMemberItem("Lunaé", false, false)), listOf(WorldPos(0.0, 0.0))),
            GuildListItem("g3", "Vexil & co", 5, "Vexil", listOf(GuildMemberItem("Vexil", false, true)), listOf(WorldPos(0.0, 0.0))),
            GuildListItem("g4", "Guilde sans nom", 1, "Tobias", listOf(GuildMemberItem("Tobias", false, true)), emptyList()),
        ),
    )

    @Test
    fun guildsDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            GuildsContent(guildsState, query = "", expandedGuildId = "g1", actions = GuildsActions())
        }
    }

    @Test
    fun guildsLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            GuildsContent(guildsState, query = "", expandedGuildId = "g1", actions = GuildsActions())
        }
    }

    private val mapState = LiveMapUiState(
        isLoading = false,
        worldPlayers = listOf(
            LiveMapPlayer("Dorian", 42, 38f, 40f, ping = 34, userId = "steam_1"),
            LiveMapPlayer("Aelys", 37, 52f, 36f, ping = 61, userId = "steam_2"),
            LiveMapPlayer("Ryuujin", 25, 60f, 56f, ping = 88, userId = "steam_3"),
            LiveMapPlayer("Fennecoeur", 19, 30f, 62f, ping = 42, userId = "steam_4"),
            LiveMapPlayer("Vexil", 31, 70f, 30f, ping = 120, userId = "steam_5"),
        ),
        focusMarkers = listOf(LiveMapBaseMarker(44f, 44f, false), LiveMapBaseMarker(64f, 63f, false)),
    )

    @Test
    fun liveMapDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            LiveMapContent(mapState, LiveMapTab.WORLD, focusedPlayerName = "Dorian", actions = LiveMapActions())
        }
    }

    @Test
    fun liveMapLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            LiveMapContent(mapState, LiveMapTab.WORLD, focusedPlayerName = null, actions = LiveMapActions())
        }
    }

    private val bansState = BansUiState(
        isLoading = false,
        bans = listOf(
            BanEntry("steam_76561198000000913", "Gr1efer", "Duplication d’objets"),
            BanEntry("gdk_2814639284751010", "NoName_88", ""),
            BanEntry("steam_76561198000000477", "Kevlar", "Destruction de bases"),
        ),
    )

    @Test
    fun bansDark() {
        paparazzi.snapshotScreen(darkTheme = true) { BansContent(bansState, ipInput = "185.220.101.42", actions = BansActions()) }
    }

    @Test
    fun bansEmptyLight() {
        paparazzi.snapshotScreen(darkTheme = false) { BansContent(BansUiState(isLoading = false), ipInput = "", actions = BansActions()) }
    }

    private val broadcastState = BroadcastUiState(
        serverName = "Serveur de Dorian",
        players = listOf("Dorian", "Aelys", "Ryuujin", "Fennecoeur", "Vexil", "Lunaé", "Mirou").map { PalworldPlayer(name = it, userId = "steam_$it") },
        playersLoaded = true,
    )

    @Test
    fun broadcastDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            BroadcastContent(
                broadcastState,
                BroadcastActions(),
                initialMessage = "Redémarrage du serveur dans 10 minutes, pensez à rentrer à la base !",
                initialDangerOpen = true,
            )
        }
    }

    @Test
    fun broadcastPrivateLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            BroadcastContent(
                broadcastState,
                BroadcastActions(),
                initialMode = BroadcastMode.Private,
                initialMessage = "Ta base déborde sur la route, tu peux la déplacer ?",
                initialRecipientId = "steam_Dorian",
            )
        }
    }
}
