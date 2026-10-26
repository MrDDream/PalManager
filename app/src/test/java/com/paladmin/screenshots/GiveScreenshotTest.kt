package com.paladmin.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.paladmin.data.local.db.HumanEntity
import com.paladmin.data.local.db.ItemEntity
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.PlayerPickerContent
import com.paladmin.ui.humanpicker.HumanPickerActions
import com.paladmin.ui.humanpicker.HumanPickerContent
import com.paladmin.ui.humanpicker.HumanPickerUiState
import com.paladmin.ui.itempicker.ItemDetailContent
import com.paladmin.ui.itempicker.ItemPickerActions
import com.paladmin.ui.itempicker.ItemPickerContent
import com.paladmin.ui.itempicker.ItemPickerUiState
import com.paladmin.ui.palpicker.PalDetailContent
import com.paladmin.ui.palpicker.PalFilterSheetContent
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.ui.palpicker.PalPickerActions
import com.paladmin.ui.palpicker.PalPickerContent
import com.paladmin.ui.palpicker.PalPickerUiState
import com.paladmin.ui.theme.PalTheme
import org.junit.Rule
import org.junit.Test

/** Écrans "Donner" (item, Pal, PNJ) rendus sans ViewModel avec de vraies données du catalogue. */
class GiveScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false))

    private val players = listOf(
        PalworldPlayer(name = "Dorian", userId = "steam_1", level = 42),
        PalworldPlayer(name = "Aelys", userId = "steam_2", level = 37),
        PalworldPlayer(name = "Ryuujin", userId = "steam_3", level = 51),
        PalworldPlayer(name = "Fennecoeur", userId = "steam_4", level = 18),
    )

    private fun item(id: String, name: String, category: String, description: String = "", stats: String = "{}") =
        ItemEntity(id = id, nameFr = name, nameEn = name, category = category, image = "$id.webp", searchText = id, description = description, statsJson = stats)

    private val items = listOf(
        item("PalSphere", "Sphère de Pal", "Sphere"),
        item(
            "PalSphere_Mega", "Méga Sphère", "Sphere",
            description = "Sphère permettant de capturer des Pals.\r\nSes performances améliorées permettent de capturer des Pals plus puissants.",
            stats = """{"Technologies":"14","Force de capture":"14"}""",
        ),
        item("PalSphere_Giga", "Giga Sphère", "Sphere"),
        item("PalSphere_Legend", "Sphère Légendaire", "Sphere"),
        item("Wood", "Bois", "Material"),
        item("Stone", "Pierre", "Material"),
        item("Pal_crystal_S", "Fragment de paldium", "Material"),
        item("CopperIngot", "Lingot de métal", "Material"),
        item("StealIngot", "Lingot de paluminium", "Material"),
        item("Polymer", "Polymère", "Material"),
        item("Money", "Pièce d'or", "Material"),
        item("AssaultRifle_Default1", "Fusil d'assaut", "Weapon"),
        item("Launcher_Default", "Lance-fusées", "Weapon"),
        item("Musket", "Fusil à mousquet", "Weapon"),
        item("BowGun", "Arbalète", "Weapon"),
        item("AIcore", "Noyau IA", "Material"),
    )

    private val categories = listOf("Accessory", "Ammo", "Armor", "Consumable", "Glider", "Ingredient", "Key_Items", "Material", "Schematic", "Sphere", "Sphere_Module", "Weapon")

    private fun pal(id: String, name: String, element: String, zukan: Int, rarity: Int, stats: Triple<Int, Int, Int>, jobs: String, element2: String? = null) = PalEntity(
        id = id, nameFr = name, nameEn = name, image = "$id.webp", searchText = id,
        element1 = element, element2 = element2, rarity = rarity, zukanIndex = zukan,
        statsJson = """{"hp":${stats.first},"attack":${stats.second},"defense":${stats.third}}""",
        workSuitabilitiesJson = jobs,
        partnerSkillJson = """{"name":"Gardien du Désert","description":"Lorsqu'activé, Anubis se bat aux côtés du joueur. Augmente la défense du joueur et des Pals de l'équipe."}""",
    )

    private val pals = listOf(
        pal("SheepBall", "Lamball", "Normal", 1, 1, Triple(70, 70, 70), """[{"job":"Handcraft","level":1},{"job":"Transport","level":1},{"job":"MonsterFarm","level":1}]"""),
        pal("PinkCat", "Cattiva", "Normal", 2, 1, Triple(70, 70, 70), """[{"job":"Handcraft","level":1}]"""),
        pal("Carbunclo", "Lifmunk", "Leaf", 4, 1, Triple(75, 70, 70), """[{"job":"Seeding","level":1}]"""),
        pal("Penguin", "Pengullet", "Water", 17, 1, Triple(70, 70, 70), """[{"job":"Watering","level":1}]""", element2 = "Ice"),
        pal("CaptainPenguin", "Penking", "Water", 18, 6, Triple(95, 70, 95), """[{"job":"Watering","level":2}]""", element2 = "Ice"),
        pal("DreamDemon", "Daedream", "Dark", 22, 1, Triple(70, 100, 60), """[{"job":"Handcraft","level":1}]"""),
        pal("Kitsunebi", "Foxparks", "Fire", 29, 1, Triple(65, 70, 70), """[{"job":"EmitFlame","level":1}]"""),
        pal("Garm", "Direhowl", "Normal", 33, 2, Triple(80, 110, 75), """[{"job":"Collection","level":1}]"""),
        pal("ElecCat", "Sparkit", "Electricity", 42, 1, Triple(60, 60, 70), """[{"job":"GenerateElectricity","level":1}]"""),
        pal("GrassPanda", "Mossanda", "Leaf", 102, 6, Triple(100, 100, 90), """[{"job":"Seeding","level":3}]"""),
        pal("Anubis", "Anubis", "Earth", 139, 10, Triple(120, 130, 100), """[{"job":"Handcraft","level":6},{"job":"Mining","level":6},{"job":"Transport","level":4}]"""),
        pal("ElecPanda", "Grizzbolt", "Electricity", 185, 8, Triple(120, 120, 105), """[{"job":"GenerateElectricity","level":5}]"""),
    )

    private fun human(id: String, name: String, hp: Int, atk: Int, def: Int, drops: String = "[]") = HumanEntity(
        id = id, nameFr = name, nameEn = name, image = "$id.webp", searchText = id,
        statsJson = """{"hp":$hp,"attack":$atk,"defense":$def}""",
        workSuitabilitiesJson = """[{"job":"Handcraft","level":2},{"job":"Mining","level":1}]""",
        dropsJson = drops,
    )

    private val humans = listOf(
        human(
            "BountyTrader", "Garde", 4900, 700, 650,
            """[{"itemId":"Pal_Sphere","name":"Sphère de Pal","minQuantity":1,"maxQuantity":2,"probability":40},{"itemId":"Gold_Coin","name":"Pièce d'or","minQuantity":50,"maxQuantity":200,"probability":40}]""",
        ),
        human("Blacksmith01", "Forgeron en Activité", 4900, 700, 650),
        human("Ambassador01", "Ambassadeur de la Ville du Désert", 2900, 700, 350),
        human("CaravanLeader01", "Capitaine de Caravane", 2900, 700, 350),
        human("Epicure01", "Chasseur de Cuisine Raffinée", 4900, 700, 650),
        human("Escort_PalTamer01", "Paldresseur Protecteur", 4900, 700, 650),
        human("Believer_Bat", "Ligue pour la Protection des Pals - Adhérent", 2900, 550, 110),
        human("BOSS_Male_NinjaElite", "Tsujigiri Recherché : Urchin", 4900, 700, 650),
    )

    private val itemState = ItemPickerUiState(
        categories = categories,
        results = items,
        target = players[0],
        selectedItem = items[1],
        quantity = 10,
    )

    @Test
    fun itemPickerDark() {
        paparazzi.snapshotScreen(darkTheme = true) { ItemPickerContent(itemState, ItemPickerActions()) }
    }

    @Test
    fun itemPickerLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            ItemPickerContent(itemState.copy(selectedCategory = "Material", results = items.filter { it.category == "Material" }, target = null, selectedItem = items[4], quantity = 100), ItemPickerActions())
        }
    }

    @Test
    fun itemDetailSheetDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame { ItemDetailContent(items[1]) } }
    }

    @Test
    fun palPickerDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            PalPickerContent(
                PalPickerUiState(results = pals, target = players[0], selectedPal = pals[10], level = 50, selectedRarityTier = PalLabels.RARITY_TIERS[4]),
                PalPickerActions(),
            )
        }
    }

    @Test
    fun palPickerLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            PalPickerContent(
                PalPickerUiState(results = pals.filter { it.element1 == "Water" || it.element1 == "Leaf" }, selectedElement = "Water", target = players[1], selectedPal = pals[4], level = 30),
                PalPickerActions(),
            )
        }
    }

    @Test
    fun palDetailSheetDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame { PalDetailContent(pals[10]) } }
    }

    @Test
    fun palFilterSheetDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            SheetFrame {
                PalFilterSheetContent(PalLabels.RARITY_TIERS[4], "Mining", 12, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun npcPickerDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            HumanPickerContent(
                HumanPickerUiState(
                    results = humans,
                    target = players[0],
                    selectedHuman = humans[0],
                    dropImages = mapOf("Pal_Sphere" to "PalSphere.webp", "Gold_Coin" to "Money.webp"),
                ),
                HumanPickerActions(),
            )
        }
    }

    @Test
    fun npcPickerLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            HumanPickerContent(HumanPickerUiState(results = humans, target = players[2], selectedHuman = humans[1], level = 20), HumanPickerActions())
        }
    }

    @Test
    fun playerSheetDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            SheetFrame { PlayerPickerContent(players, selectedUserId = "steam_1", onSelect = {}, onRefresh = {}) }
        }
    }

    @Test
    fun playerSheetEmptyLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            SheetFrame { PlayerPickerContent(emptyList(), selectedUserId = null, onSelect = {}, onRefresh = {}) }
        }
    }

    /** Imite l'habillage de GiveSheet (ModalBottomSheet ne se rend pas dans Paparazzi). */
    @Composable
    private fun SheetFrame(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
        val c = PalTheme.colors
        Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.BottomCenter) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceBar, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
                content = content,
            )
        }
    }
}
