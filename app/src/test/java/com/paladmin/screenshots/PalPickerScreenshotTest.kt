package com.paladmin.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.ui.components.FilterPanel
import com.paladmin.ui.components.SearchField
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.ui.theme.PalAdminTheme
import org.junit.Rule
import org.junit.Test

/** Vitrine du sélecteur de Pals — grille + fiche détail (Anubis), avec de vraies icônes issues des assets. */
class PalPickerScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    private data class SamplePal(val id: String, val nameFr: String, val image: String)

    private val samplePals = listOf(
        SamplePal("Alpaca", "Melpaca", "Alpaca.webp"),
        SamplePal("Anubis", "Anubis", "Anubis.webp"),
        SamplePal("CaptainPenguin", "Penking", "CaptainPenguin.webp"),
        SamplePal("Carbunclo", "Lifmunk", "Carbunclo.webp"),
        SamplePal("Garm", "Direhowl", "Garm.webp"),
        SamplePal("JetDragon", "Jetragon", "JetDragon.webp"),
        SamplePal("Kitsunebi", "Foxparks", "Kitsunebi.webp"),
        SamplePal("Penguin", "Pengullet", "Penguin.webp"),
        SamplePal("PinkCat", "Cattiva", "PinkCat.webp"),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun palPickerGrid() {
        paparazzi.snapshot {
            PalAdminTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sélectionner un Pal") },
                            navigationIcon = { IconButton(onClick = {}) {} },
                        )
                    },
                ) { padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        SearchField(value = "", onValueChange = {}, label = "Rechercher", modifier = Modifier.fillMaxWidth().padding(12.dp))
                        FilterPanel(activeCount = 2, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("Élément", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = false, onClick = {}, label = { Text("Tous") })
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(PalLabels.elementLabel("Earth")) },
                                    leadingIcon = { AssetImage(paparazzi.context, "images/elements/Earth.webp", modifier = Modifier.size(18.dp)) },
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(samplePals, key = { it.id }) { pal -> PalCard(pal) }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Test
    fun palDetailDialog() {
        paparazzi.snapshot {
            PalAdminTheme {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Anubis") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.8f)) {
                                AssetImage(paparazzi.context, "images/pals/Anubis.webp", modifier = Modifier.fillMaxSize())
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                                ElementBadge("Earth")
                                RarityBadge()
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                StatChip("PV", 120, Modifier.weight(1f))
                                StatChip("ATQ", 130, Modifier.weight(1f))
                                StatChip("DEF", 100, Modifier.weight(1f))
                            }
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text("Compétence de partenaire", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                ) {
                                    Text("Gardien du Désert", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Lorsqu'activé, Anubis transfère son pouvoir au joueur, convertissant ses attaques en Terre et augmentant son attaque de 30 %.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            Text("Métiers", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                JobChip("Handcraft.webp", "Artisanat niv. 6")
                                JobChip("Mining.webp", "Minage niv. 6")
                                JobChip("Transport.webp", "Transport niv. 4")
                            }
                            Text("ID : Anubis", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                            OutlinedTextField(value = "1", onValueChange = {}, label = { Text("Niveau") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    },
                    confirmButton = { Button(onClick = {}) { Text("Donner") } },
                    dismissButton = { OutlinedButton(onClick = {}) { Text("Annuler") } },
                )
            }
        }
    }

    @Composable
    private fun PalCard(pal: SamplePal) {
        Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable {}) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    AssetImage(paparazzi.context, "images/pals/${pal.image}", modifier = Modifier.fillMaxSize())
                }
                Text(
                    text = pal.nameFr,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    @Composable
    private fun ElementBadge(element: String) {
        val color = PalLabels.ELEMENT_COLORS[element] ?: MaterialTheme.colorScheme.primary
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color.copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            AssetImage(paparazzi.context, "images/elements/$element.webp", modifier = Modifier.size(16.dp))
            Text(PalLabels.elementLabel(element), color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
        }
    }

    @Composable
    private fun RarityBadge() {
        val tier = PalLabels.rarityTier(10)
        Text(
            "Légendaire",
            color = tier.color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.background(tier.color.copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }

    @Composable
    private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun JobChip(jobImage: String, label: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            AssetImage(paparazzi.context, "images/jobs/$jobImage", modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
