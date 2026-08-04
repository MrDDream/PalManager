package com.paladmin.screenshots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.theme.PalAdminTheme
import org.junit.Rule
import org.junit.Test

/**
 * Rendu de vitrine pour le README GitHub — recompose la mise en page réelle du Dashboard avec des
 * données d'exemple plutôt que le vrai ViewModel (Hilt non disponible dans un test JVM Paparazzi).
 * Aucune modification du code de production : uniquement une reconstruction visuelle fidèle.
 */
class DashboardScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun dashboard() {
        paparazzi.snapshot {
            PalAdminTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Serveur de Dorian") }) },
                ) { padding ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(padding),
                    ) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                    IconBadge(icon = Icons.Filled.Dns)
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text("Palworld Server", style = MaterialTheme.typography.titleMedium)
                                        Text("IP : 208.115.214.218", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Palworld : v0.6.1.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("PalDefender : v1.4.2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                StatTile("Joueurs", "7/32", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                                StatTile("FPS serveur", "60", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                            }
                        }
                        item { SectionHeader("Actions rapides") }
                        item {
                            Card {
                                Column {
                                    ActionRow(Icons.Filled.People, "Joueurs connectés")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Map, "Carte en direct")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Inventory2, "Donner un item")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Pets, "Donner un Pal")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Face, "Donner un PNJ")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Groups, "Guildes")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Block, "Bannissements")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Save, "Sauvegarder le monde")
                                    HorizontalDivider()
                                    ActionRow(Icons.Filled.Campaign, "Diffusion & maintenance", showChevron = true)
                                }
                            }
                        }
                        item { SectionHeader("Zone danger", color = MaterialTheme.colorScheme.error) }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                ActionRow(Icons.Filled.RestartAlt, "Redémarrer le serveur", tint = MaterialTheme.colorScheme.onSecondaryContainer, showChevron = false)
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                ActionRow(Icons.Filled.Stop, "Arrêt immédiat (force)", tint = MaterialTheme.colorScheme.onErrorContainer, showChevron = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, onAccent: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = accent), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = onAccent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = onAccent.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f).padding(start = 16.dp))
        if (showChevron) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
