package com.paladmin.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.palworld.PalworldInfo
import com.paladmin.data.remote.palworld.PalworldMetrics
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.dashboard.DashboardActions
import com.paladmin.ui.dashboard.DashboardContent
import com.paladmin.ui.dashboard.DashboardUiState
import com.paladmin.ui.dashboard.PowerSheetContent
import com.paladmin.ui.theme.PalTheme
import org.junit.Rule
import org.junit.Test

/** Rendu du vrai [DashboardContent] (sans ViewModel) avec des données d'exemple, thèmes sombre et clair. */
class DashboardScreenshotTest {

    @get:Rule
    val paparazzi = palPaparazzi()

    private val sampleState = DashboardUiState(
        isLoading = false,
        profile = ServerProfile(
            name = "Serveur de Dorian",
            host = "208.115.214.218",
            sftpUsername = "steam",
            sftpPassword = "x",
        ),
        info = PalworldInfo(version = "v0.6.1.1", servername = "Palworld Server"),
        metrics = PalworldMetrics(serverfps = 60, currentplayernum = 7, serverframetime = 16.6, maxplayernum = 32, uptime = 310_000, days = 128),
        players = listOf("Dorian", "Aelys", "Ryuujin", "Fennecoeur", "Vexil", "Lunaé", "Mirou").map { PalworldPlayer(name = it, userId = "steam_$it") },
        palDefenderVersion = "v1.4.2",
    )

    @Test
    fun dashboardDark() {
        paparazzi.snapshotScreen(darkTheme = true) { DashboardContent(sampleState, DashboardActions()) }
    }

    @Test
    fun dashboardLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            DashboardContent(sampleState.copy(profile = sampleState.profile!!.copy(sftpUsername = "")), DashboardActions())
        }
    }

    @Test
    fun powerSheetDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            Box(Modifier.fillMaxSize().background(PalTheme.colors.surfaceBar), contentAlignment = Alignment.BottomCenter) {
                PowerSheetContent("Serveur de Dorian", 7, { _, _ -> }, {})
            }
        }
    }
}
