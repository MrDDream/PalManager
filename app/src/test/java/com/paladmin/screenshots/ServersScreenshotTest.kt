package com.paladmin.screenshots

import app.cash.paparazzi.DeviceConfig
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.ThemeMode
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.sftp.SftpEntry
import com.paladmin.ui.crash.CrashScreen
import com.paladmin.ui.profiles.AddEditProfileActions
import com.paladmin.ui.profiles.AddEditProfileContent
import com.paladmin.ui.profiles.CheckResult
import com.paladmin.ui.profiles.CheckStatus
import com.paladmin.ui.profiles.ProfileFormState
import com.paladmin.ui.profiles.ServerListActions
import com.paladmin.ui.profiles.ServerListUiState
import com.paladmin.ui.profiles.ServerProbe
import com.paladmin.ui.profiles.ServerProfileListContent
import com.paladmin.ui.profiles.SftpPathPickerContent
import com.paladmin.ui.profiles.VerifyState
import com.paladmin.ui.settings.AppSettingsActions
import com.paladmin.ui.settings.AppSettingsContent
import com.paladmin.ui.settings.AppSettingsUiState
import com.paladmin.ui.splash.SplashContent
import org.junit.Rule
import org.junit.Test

/** Écrans du groupe "serveurs" (liste, ajout/édition, sélecteur SFTP, paramètres, splash, crash). */
class ServersScreenshotTest {

    @get:Rule
    val paparazzi = palPaparazzi()

    private val dorian = ServerProfile(
        id = 1,
        name = "Serveur de Dorian",
        iconKey = "dns",
        host = "208.115.214.218",
        palworldPort = 8211,
        palDefenderToken = "token",
        sftpUsername = "steam",
        sftpPassword = "x",
    )
    private val test = ServerProfile(id = 2, name = "Serveur test", iconKey = "games", host = "192.168.1.20", palworldPort = 8212)
    private val friends = ServerProfile(id = 3, name = "Les copains", iconKey = "forest", host = "pal.example.org", palworldPort = 8212, palDefenderToken = "t")

    private val listState = ServerListUiState(
        isLoading = false,
        profiles = listOf(dorian, test),
        probes = mapOf(
            1L to ServerProbe.Online(players = 7, maxPlayers = 32, fps = 60, latencyMs = 24),
            2L to ServerProbe.Offline,
        ),
    )

    @Test
    fun serverListDark() {
        paparazzi.snapshotScreen(darkTheme = true) { ServerProfileListContent(listState, ServerListActions()) }
    }

    @Test
    fun serverListLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            ServerProfileListContent(
                listState.copy(
                    profiles = listOf(dorian, test, friends),
                    probes = listState.probes + (3L to ServerProbe.Unauthorized) - 2L,
                ),
                ServerListActions(),
            )
        }
    }

    @Test
    fun serverListEmptyDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            ServerProfileListContent(ServerListUiState(isLoading = false), ServerListActions())
        }
    }

    private val formState = ProfileFormState(
        name = "Serveur de Dorian",
        iconKey = "dns",
        host = "208.115.214.218",
        palworldPort = "8212",
        palworldPassword = "motdepasse",
        palDefenderPort = "17993",
        palDefenderToken = "tokentoken",
        sftpUsername = "steam",
        sftpPassword = "motdepasse",
        sftpPalDefenderLogPath = "/home/steam/Pal/Binaries/Win64/PalDefender/Logs",
        sftpUe4ssLogPath = "/home/steam/Pal/Binaries/Win64/ue4ss/UE4SS.log",
        sftpPalTemplatesPath = "",
        sftpHostKeyFingerprint = "SHA256:q3b9Xk2vV7nqP0yFJm1c8Qe4uRrT0aZzLw5sYdHh6oE",
        isLoading = false,
    )

    private val verifyState = VerifyState(
        palworld = CheckResult(CheckStatus.SUCCESS, latencyMs = 64),
        palDefender = CheckResult(CheckStatus.FAILURE, message = "Failed to connect to /208.115.214.218:17993"),
        sftp = CheckResult(CheckStatus.CHECKING),
    )

    @Test
    fun addServerDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            AddEditProfileContent(isNew = true, state = formState, verifyState = null, actions = AddEditProfileActions(), sftpInitiallyExpanded = false)
        }
    }

    @Test
    fun addServerExpandedDark() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false, screenHeight = 5000))
        paparazzi.snapshotScreen(darkTheme = true) {
            AddEditProfileContent(isNew = false, state = formState, verifyState = verifyState, actions = AddEditProfileActions(), sftpInitiallyExpanded = true)
        }
    }

    @Test
    fun addServerLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            AddEditProfileContent(isNew = true, state = formState.copy(palDefenderPort = ""), verifyState = null, actions = AddEditProfileActions(), sftpInitiallyExpanded = false)
        }
    }

    @Test
    fun sftpPickerDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            SftpPathPickerContent(
                currentPath = "/home/steam/Pal/Binaries/Win64",
                entries = listOf(
                    SftpEntry("PalDefender", "/home/steam/Pal/Binaries/Win64/PalDefender", true),
                    SftpEntry("ue4ss", "/home/steam/Pal/Binaries/Win64/ue4ss", true),
                    SftpEntry("PalServer-Win64-Shipping-Cmd.exe", "/home/steam/Pal/Binaries/Win64/PalServer-Win64-Shipping-Cmd.exe", false),
                    SftpEntry("steam_appid.txt", "/home/steam/Pal/Binaries/Win64/steam_appid.txt", false),
                ),
                isLoading = false,
                error = null,
                onBack = {},
                onPickCurrent = {},
                onOpenEntry = {},
                onGoUp = {},
            )
        }
    }

    @Test
    fun settingsDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            AppSettingsContent(
                state = AppSettingsUiState(themeMode = ThemeMode.DARK, language = AppLanguage.FRENCH, debugLoggingEnabled = true),
                folderName = null,
                version = "0.1.6",
                actions = AppSettingsActions(),
            )
        }
    }

    @Test
    fun settingsLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            AppSettingsContent(
                state = AppSettingsUiState(themeMode = ThemeMode.LIGHT, language = AppLanguage.FRENCH, debugLoggingEnabled = false),
                folderName = null,
                version = "0.1.6",
                actions = AppSettingsActions(),
            )
        }
    }

    @Test
    fun splashDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SplashContent() }
    }

    @Test
    fun crashDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            CrashScreen(
                trace = buildString {
                    appendLine("java.lang.IllegalStateException: Profil introuvable")
                    repeat(12) { appendLine("    at com.paladmin.ui.dashboard.DashboardViewModel.refresh(DashboardViewModel.kt:${60 + it})") }
                },
                onContinue = {},
            )
        }
    }
}
