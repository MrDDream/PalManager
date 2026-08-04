package com.paladmin.screenshots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.ui.components.FilterPanel
import com.paladmin.ui.components.SearchField
import com.paladmin.ui.theme.PalAdminTheme
import org.junit.Rule
import org.junit.Test

/** Vitrine du sélecteur d'items — grille + filtres, avec de vraies icônes issues des assets. */
class ItemPickerScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    private data class SampleItem(val id: String, val nameFr: String, val image: String, val category: String)

    private val sampleItems = listOf(
        SampleItem("PalSphere", "Sphère Pal", "PalSphere.webp", "Sphere"),
        SampleItem("MakeshiftHandgun", "Pistolet rouillé", "MakeshiftHandgun.webp", "Weapon"),
        SampleItem("AssaultRifle_Default1", "Fusil d'assaut", "AssaultRifle_Default1.webp", "Weapon"),
        SampleItem("Arrow_Fire", "Flèche de feu", "Arrow_Fire.webp", "Ammo"),
        SampleItem("AncientHelmet", "Casque antique", "AncientHelmet.webp", "Armor"),
        SampleItem("AIcore", "Noyau IA", "AIcore.webp", "Material"),
        SampleItem("Money", "Pièces", "Money.webp", "Material"),
        SampleItem("ShotgunBullet", "Cartouches de fusil à pompe", "ShotgunBullet.webp", "Ammo"),
        SampleItem("Axe_Tier_01", "Hache en métal", "Axe_Tier_01.webp", "Weapon"),
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Test
    fun itemPicker() {
        paparazzi.snapshot {
            PalAdminTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sélectionner un item") },
                            navigationIcon = { IconButton(onClick = {}) {} },
                        )
                    },
                ) { padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        SearchField(
                            value = "",
                            onValueChange = {},
                            label = "Rechercher",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                        FilterPanel(activeCount = 1, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = false, onClick = {}, label = { Text("Toutes") })
                                FilterChip(selected = true, onClick = {}, label = { Text("Weapon") }, leadingIcon = { Icon(Icons.Filled.GpsFixed, null, modifier = Modifier.size(18.dp)) })
                                FilterChip(selected = false, onClick = {}, label = { Text("Sphere") }, leadingIcon = { Icon(Icons.Filled.Circle, null, modifier = Modifier.size(18.dp)) })
                                FilterChip(selected = false, onClick = {}, label = { Text("Armor") }, leadingIcon = { Icon(Icons.Filled.Shield, null, modifier = Modifier.size(18.dp)) })
                                FilterChip(selected = false, onClick = {}, label = { Text("Ammo") }, leadingIcon = { Icon(Icons.Filled.Adjust, null, modifier = Modifier.size(18.dp)) })
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(sampleItems, key = { it.id }) { item -> ItemCard(item) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemCard(item: SampleItem) {
        Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable {}) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    AssetImage(paparazzi.context, "images/items/${item.image}", modifier = Modifier.fillMaxSize())
                }
                Text(
                    text = item.nameFr,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
