package com.paladmin.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.theme.PalAdminTheme
import org.junit.Rule
import org.junit.Test

/** Vitrine de la liste des joueurs connectés. */
class PlayersScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    private data class SamplePlayer(val name: String, val level: Int, val ip: String, val ping: Int, val userId: String)

    private val samplePlayers = listOf(
        SamplePlayer("Dorian", 42, "192.168.1.24", 18, "steam_76561198000000001"),
        SamplePlayer("Aelys", 37, "192.168.1.31", 32, "steam_76561198000000002"),
        SamplePlayer("Ryuujin", 25, "192.168.1.45", 54, "gdk_2814639284756192"),
        SamplePlayer("Fennecoeur", 19, "192.168.1.52", 21, "steam_76561198000000004"),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun playersList() {
        paparazzi.snapshot {
            PalAdminTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Joueurs connectés") },
                            navigationIcon = { IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                            actions = { IconButton(onClick = {}) { Icon(Icons.Filled.Refresh, contentDescription = null) } },
                        )
                    },
                ) { padding ->
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                        items(samplePlayers) { player -> PlayerRow(player) }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlayerRow(player: SamplePlayer) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            ListItem(
                headlineContent = { Text(player.name) },
                supportingContent = {
                    Column {
                        Text("Niveau ${player.level} · ${player.ip} · ${player.ping} ms")
                        Text(
                            "ID : ${player.userId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingContent = { IconBadge(icon = Icons.Filled.Person) },
                trailingContent = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                },
            )
        }
    }
}
