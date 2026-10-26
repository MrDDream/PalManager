package com.paladmin.screenshots

import app.cash.paparazzi.DeviceConfig
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.ui.logs.LogSource
import com.paladmin.ui.logs.LogsActions
import com.paladmin.ui.logs.LogsContent
import com.paladmin.ui.logs.LogsUiState
import com.paladmin.ui.palcreator.CatalogOption
import com.paladmin.ui.palcreator.PalCreatorActions
import com.paladmin.ui.palcreator.PalCreatorContent
import com.paladmin.ui.palcreator.PalCreatorFormState
import com.paladmin.ui.sftp.SftpActions
import com.paladmin.ui.sftp.SftpClipboard
import com.paladmin.ui.sftp.SftpContent
import com.paladmin.ui.sftp.SftpUiState
import org.junit.Rule
import org.junit.Test

/** Rendu des vrais écrans outils SFTP (Logs, Fichiers, Créateur de Pal) sans ViewModel. */
class ToolsScreenshotTest {

    @get:Rule
    val paparazzi = palPaparazzi()

    private val logLines = listOf(
        "[10:02:26][info] Dorian (steam_76561198000000001) connected from 192.168.1.24",
        "[10:02:31][info] Command give_pal executed: Anubis lvl 50 -> Dorian",
        "[10:03:02][warning] Vexil: high latency (112 ms) on 3 consecutive samples",
        "[10:03:40][info] World save completed in 1.8 s",
        "[10:04:12][error] Failed to read templates/anubis_test.json: missing \"Level\" field",
        "[10:04:55][info] Aelys joined guild \"Les Pattes Vertes\"",
        "[10:05:18][warning] Base 7F3A...C21: 41 Pals over the configured limit",
        "[10:06:01][info] Broadcast sent to 7 players",
        "[10:06:44][error] REST API: timeout on /v1/api/metrics (5000 ms)",
        "[10:07:20][info] Ryuujin (gdk_2814...6192) disconnected",
    )

    private val logsState = LogsUiState(lines = logLines, truncated = true, loadedOnce = true)

    @Test
    fun logsDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            LogsContent(true, true, LogSource.PALDEFENDER, logsState, query = "", level = null, actions = LogsActions())
        }
    }

    @Test
    fun logsFilteredLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            LogsContent(true, true, LogSource.UE4SS, logsState, query = "", level = com.paladmin.ui.logs.LogLevel.WARN, actions = LogsActions())
        }
    }

    @Test
    fun logsNotConfiguredDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            LogsContent(true, false, LogSource.PALDEFENDER, LogsUiState(), query = "", level = null, actions = LogsActions())
        }
    }

    private val win64 = "/home/steam/Pal/Binaries/Win64"
    private val t = 1_761_472_320L // 26/10 10:12 UTC

    private val entries = listOf(
        SftpEntry("appcache", "$win64/appcache", true, modifiedEpochSeconds = t - 58_000),
        SftpEntry("config", "$win64/config", true, modifiedEpochSeconds = t - 3_600),
        SftpEntry("logs", "$win64/logs", true, modifiedEpochSeconds = t - 300),
        SftpEntry("PalDefender", "$win64/PalDefender", true, modifiedEpochSeconds = t - 480),
        SftpEntry("ue4ss", "$win64/ue4ss", true, modifiedEpochSeconds = t - 400_000),
        SftpEntry("boost_atomic-vc142-mt-x64-1_83.dll", "$win64/boost_atomic-vc142-mt-x64-1_83.dll", false, 24_576, t - 4_500_000),
        SftpEntry("boost_chrono-vc142-mt-x64-1_83.dll", "$win64/boost_chrono-vc142-mt-x64-1_83.dll", false, 41_984, t - 4_500_000),
        SftpEntry("PalServer-Win64-Shipping-Cmd.exe", "$win64/PalServer-Win64-Shipping-Cmd.exe", false, 148_897_792, t - 700_000),
        SftpEntry("steam_api64.dll", "$win64/steam_api64.dll", false, 300_032, t - 4_500_000),
        SftpEntry("steamclient64.dll", "$win64/steamclient64.dll", false, 23_068_672, t - 4_500_000),
    )

    private val sftpState = SftpUiState(currentPath = win64, entries = entries, isLoading = false)

    @Test
    fun filesSelectionDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            SftpContent(true, true, true, sftpState, clipboard = null, selected = entries[7], actions = SftpActions())
        }
    }

    @Test
    fun filesClipboardLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            SftpContent(true, true, true, sftpState, clipboard = SftpClipboard(entries[8], isCut = false), selected = null, actions = SftpActions())
        }
    }

    @Test
    fun filesHostKeyMismatchDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            SftpContent(
                true, true, true,
                sftpState.copy(hostKeyMismatch = "SHA256:3f9Xc0n2bLq8dJ7r1VtYw4eHk6Zp5aMsQ+uN0gBxR2E" to "SHA256:Qm7eT1yR4oPz9WcX2vBn8LkJh5Gf3Ds6Aa0Ue+IuYtE"),
                clipboard = null, selected = null, actions = SftpActions(),
            )
        }
    }

    private val anubis = PalEntity(
        id = "Anubis", nameFr = "Anubis", nameEn = "Anubis", image = "Anubis.webp", searchText = "anubis",
        element1 = "Earth", rarity = 10, zukanIndex = 139,
    )

    private val form = PalCreatorFormState(
        selectedSpecies = anubis,
        speciesQuery = "Anubis",
        fileName = "anubis_test",
        nickname = "Anubis_test",
        level = "50",
        gender = "Male",
        isBoss = true,
        soulDefense = "10",
        soulCraftSpeed = "0",
        activeSkills = listOf(
            CatalogOption("RockLance", "Lance-Roche", element = "Earth", power = 400, cooldown = 20),
            CatalogOption("SandTornado", "Tornade de Sable", element = "Earth", power = 200, cooldown = 12),
            CatalogOption("StoneCannon", "Canon de Pierre", element = "Earth", power = 70, cooldown = 4),
        ),
        passives = listOf(
            CatalogOption("Legend", "Légende", rank = 4),
            CatalogOption("ElementBoost_Earth_2_PAL", "Empereur Terrestre", rank = 3),
        ),
    )

    @Test
    fun palCreatorDark() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false, screenHeight = 4300))
        paparazzi.snapshotScreen(darkTheme = true) {
            PalCreatorContent(
                profileLoaded = true, sftpConfigured = true, hostKeyMismatch = null, form = form, isSaving = false,
                speciesResults = emptyList(), activeSkillOptions = emptyList(), passiveOptions = emptyList(),
                language = AppLanguage.FRENCH, actions = PalCreatorActions(),
            )
        }
    }

    @Test
    fun palCreatorSpeciesLight() {
        val results = listOf(
            anubis,
            PalEntity("SheepBall", "Moumouton", "Lamball", "SheepBall.webp", "", zukanIndex = 1),
            PalEntity("PinkCat", "Miaoucoeur", "Cattiva", "PinkCat.webp", "", zukanIndex = 2),
        )
        paparazzi.snapshotScreen(darkTheme = false) {
            PalCreatorContent(
                profileLoaded = true, sftpConfigured = true, hostKeyMismatch = null,
                form = PalCreatorFormState(speciesQuery = "a"), isSaving = false,
                speciesResults = results, activeSkillOptions = emptyList(), passiveOptions = emptyList(),
                language = AppLanguage.FRENCH, actions = PalCreatorActions(),
            )
        }
    }
}
