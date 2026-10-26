package com.paladmin.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.ActiveSkillDisplay
import com.paladmin.ui.components.DetailRow
import com.paladmin.ui.components.InventoryGridItem
import com.paladmin.ui.components.PalInfo
import com.paladmin.ui.components.PalIvInfo
import com.paladmin.ui.components.PalSoulInfo
import com.paladmin.ui.components.PassiveDisplay
import com.paladmin.ui.components.ToneKey
import com.paladmin.ui.players.DetailTabState
import com.paladmin.ui.players.PalsTabState
import com.paladmin.ui.players.PlayerSheetContent
import com.paladmin.ui.players.PlayerSheetUiState
import com.paladmin.ui.players.PlayerSort
import com.paladmin.ui.players.PlayerTab
import com.paladmin.ui.players.PlayersActions
import com.paladmin.ui.players.PlayersContent
import com.paladmin.ui.players.PlayersUiState
import com.paladmin.ui.theme.PalTheme
import org.junit.Rule
import org.junit.Test

/** Rendu du vrai [PlayersContent] et de la fiche joueur ([PlayerSheetContent], un test par onglet),
 * sans ViewModel, avec les données d'exemple de la maquette. */
class PlayersScreenshotTest {

    @get:Rule
    val paparazzi = palPaparazzi()

    private val players = listOf(
        PalworldPlayer(name = "Kaito", userId = "steam_76561198000000001", ip = "192.168.1.24", ping = 18.0, level = 42),
        PalworldPlayer(name = "Aelys", userId = "steam_76561198000000002", ip = "192.168.1.31", ping = 32.0, level = 37),
        PalworldPlayer(name = "Ryuujin", userId = "gdk_2814639284756192", ip = "192.168.1.45", ping = 54.0, level = 25),
        PalworldPlayer(name = "Fennecoeur", userId = "steam_76561198000000004", ip = "192.168.1.52", ping = 21.0, level = 19),
        PalworldPlayer(name = "Vexil", userId = "steam_76561198000000005", ip = "192.168.1.60", ping = 112.0, level = 31),
        PalworldPlayer(name = "Lunaé", userId = "gdk_2814639284756207", ip = "192.168.1.71", ping = 47.0, level = 12),
        PalworldPlayer(name = "Mirou", userId = "steam_76561198000000007", ip = "192.168.1.88", ping = 76.0, level = 8),
    )

    private val listState = PlayersUiState(isLoading = false, players = players, maxPlayers = 32, sort = PlayerSort.LEVEL)

    private fun pal(
        id: String,
        name: String,
        element: String,
        level: Int,
        gender: String,
        iv: PalIvInfo? = null,
        soul: PalSoulInfo? = null,
        skills: List<ActiveSkillDisplay> = emptyList(),
        passives: List<PassiveDisplay> = emptyList(),
        nickname: String = "",
    ) = PalInfo(
        imagePath = "file:///android_asset/images/pals/$id.webp",
        speciesName = name,
        nickname = nickname,
        level = level,
        genderRaw = gender,
        gender = gender,
        shiny = false,
        element = element,
        exp = 1_482_910L,
        activeSkills = skills,
        passives = passives,
        iv = iv,
        soul = soul,
    )

    private val team = listOf(
        pal(
            "Anubis", "Anubis", "Earth", 50, "Male",
            iv = PalIvInfo(87, 92, 75), soul = PalSoulInfo(10, 8, 5, 3),
            skills = listOf(
                ActiveSkillDisplay("Lance-Roche", "Earth", 400, 20),
                ActiveSkillDisplay("Tremblement", "Earth", 300, 16),
                ActiveSkillDisplay("Fracasse-Sol", "Earth", 500, 24),
            ),
            passives = listOf(PassiveDisplay("Légende", 4), PassiveDisplay("Empereur Terrestre", 3), PassiveDisplay("Maîtrise Exceptionnelle", 4)),
        ),
        pal("JetDragon", "Jetragon", "Dragon", 50, "Female", PalIvInfo(64, 100, 58), PalSoulInfo(6, 10, 4, 0), passives = listOf(PassiveDisplay("Sprinteur", 4), PassiveDisplay("Dragon Divin", 3))),
        pal("ElecPanda", "Grizzbolt", "Electricity", 44, "Male", PalIvInfo(55, 70, 81), PalSoulInfo(2, 4, 2, 6), passives = listOf(PassiveDisplay("Appliqué", 3))),
        pal("CaptainPenguin", "Penking", "Water", 41, "Female", PalIvInfo(40, 66, 72), PalSoulInfo(0, 2, 0, 4), passives = listOf(PassiveDisplay("Sérieux", 1))),
        pal("Kitsunebi", "Foxparks", "Fire", 22, "Male", PalIvInfo(31, 48, 20), PalSoulInfo(0, 0, 0, 0)),
    )

    private val palbox = listOf(
        "LilyQueen" to "Lyleen" to 47, "Horus" to "Faleris" to 45, "BlackGriffon" to "Shadowbeak" to 43, "IceHorse" to "Frostallion" to 50,
        "ThunderDragonMan" to "Orserk" to 39, "GrassPanda" to "Mossanda" to 31, "AmaterasuWolf" to "Kitsun" to 29, "Alpaca" to "Melpaca" to 20,
        "SheepBall" to "Lamball" to 12, "ChickenPal" to "Chikipi" to 9, "NegativeKoala" to "Depresso" to 15, "WoolFox" to "Cremis" to 8,
    ).let { base -> (0 until 6).flatMap { base } }.take(68).map { (idName, level) -> pal(idName.first, idName.second, "Normal", level, "Male") }

    private fun item(id: String, name: String, quantity: Int) =
        InventoryGridItem(imagePath = "file:///android_asset/images/items/$id.webp", label = name, quantity = quantity)

    private val inventoryRows = listOf(
        DetailRow.Section("Objets", trailing = "16/48"),
        DetailRow.Grid(
            listOf(
                item("Money", "Pièce d'or", 17_900_000), item("Wood", "Bois", 364), item("Stone", "Pierre", 950),
                item("Pal_crystal_S", "Fragment de paldium", 2000), item("Fiber", "Fibre", 85), item("IronIngot", "Lingot de métal raffiné", 360),
                item("PalSphere_Mega", "Méga Sphère", 25), item("PalSphere_Giga", "Giga Sphère", 11), item("Wool", "Laine", 749),
                item("PalCrystal_Ex", "Fragments d'une civilisation antique", 21),
            ),
        ),
        DetailRow.Section("Armes", trailing = "4/4"),
        DetailRow.Grid(
            listOf(
                item("AssaultRifle_Default1", "Fusil d'assaut", 1), item("Launcher_Default", "Lance-fusées", 1),
                item("GrapplingGun", "Pistolet-grappin", 1), item("BowGun", "Arbalète", 1),
            ),
        ),
        DetailRow.Section("Nourriture", trailing = "3/5"),
        DetailRow.Grid(listOf(item("Cake", "Gâteau", 10), item("Baked_Berries", "Baie grillée", 42), item("Honey", "Miel", 18))),
    )

    private val progressionRows = listOf(
        DetailRow.Kpis(listOf("Niveau" to "42", "Boss vaincus" to "23", "Pals capturés" to "1 284")),
        DetailRow.Section("Joueur", Icons.Filled.TrendingUp, tone = ToneKey.Green),
        DetailRow.Stats(listOf("EXP" to "1 482 910", "Statut non utilisés" to "3"), tone = ToneKey.Green),
        DetailRow.Section("Monnaies", Icons.Filled.Diamond, tone = ToneKey.Amber),
        DetailRow.Stats(listOf("Points de technologie" to "12", "Techno. ancienne" to "4", "Reliques" to "61"), tone = ToneKey.Amber),
        DetailRow.Section("Boss", Icons.Filled.Warning, tone = ToneKey.Danger),
        DetailRow.Stats(listOf("Boss de tour" to "3", "Boss de raid" to "2", "Prédateurs vaincus" to "7"), tone = ToneKey.Danger),
        DetailRow.Section("Captures", Icons.Filled.Pets, tone = ToneKey.Sky),
        DetailRow.Stats(listOf("Tribus capturées" to "4"), tone = ToneKey.Sky),
        DetailRow.Section("Activités", Icons.Filled.Construction, tone = ToneKey.Purple),
        DetailRow.Stats(listOf("Donjons normaux" to "38", "Objets fabriqués" to "9 120"), tone = ToneKey.Purple),
    )

    private fun tech(image: String, name: String, unlocked: Boolean, ancient: Boolean = false) = DetailRow.WithImage(
        imagePath = "file:///android_asset/images/technologies/$image.webp",
        text = name,
        status = if (unlocked) "Débloquée" else "Verrouillée",
        positive = unlocked,
        tint = if (ancient) ToneKey.Purple else null,
    )

    private val techRows = listOf(
        DetailRow.Progress("Débloquées", 142, 587),
        DetailRow.Section("Niveau 1"),
        tech("Battle_MeleeWeapon_Bat", "Batte en bois", true),
        tech("Workbench", "Établi de fortune", true),
        DetailRow.Section("Niveau 2"),
        tech("PALBOX", "Boîte à Pal", true),
        tech("Infra_ItemChest_Grade_01", "Coffre en bois", true),
        DetailRow.Section("Niveau 3"),
        tech("Arrow", "Flèche", true),
        tech("GlobalPalStorage", "Boîte à Pals globale", false),
        DetailRow.Section("Niveau 4"),
        tech("Battle_Armor_Grade_01_Cloth", "Vêtement en tissu", true),
        tech("PalFoodBox", "Boîte à nourriture", false),
        DetailRow.Section("Niveau 6"),
        tech("Product_WorkBench_SkillUnlock", "Établi pour équipement de Pal", true),
        tech("Arrow_Fire", "Flèche de feu", false, ancient = true),
    )

    private val sheet = PlayerSheetUiState(
        player = players.first(),
        tab = PlayerTab.TEAM,
        pals = PalsTabState(isLoading = false, team = team, palbox = palbox),
        inventory = DetailTabState(isLoading = false, rows = inventoryRows),
        progression = DetailTabState(isLoading = false, rows = progressionRows),
        techs = DetailTabState(isLoading = false, rows = techRows),
    )

    /** Habillage "feuille du bas" (écran assombri au-dessus, coins 28 dp, poignée) comme la maquette. */
    @Composable
    private fun SheetFrame(state: PlayerSheetUiState) {
        val c = PalTheme.colors
        Box(Modifier.fillMaxSize().background(c.background)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 56.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(c.surfaceBar),
            ) {
                Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp).background(c.neutral.solid, RoundedCornerShape(4.dp)))
                PlayerSheetContent(state = state, onSelectTab = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Test
    fun playersListDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            PlayersContent(listState, expandedUserId = "steam_76561198000000001", actions = PlayersActions())
        }
    }

    @Test
    fun playersListLight() {
        paparazzi.snapshotScreen(darkTheme = false) {
            PlayersContent(listState.copy(sort = PlayerSort.PING), expandedUserId = "gdk_2814639284756192", actions = PlayersActions())
        }
    }

    @Test
    fun playersEmptyDark() {
        paparazzi.snapshotScreen(darkTheme = true) {
            PlayersContent(PlayersUiState(isLoading = false), expandedUserId = null, actions = PlayersActions())
        }
    }

    @Test
    fun sheetTeamDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame(sheet) }
    }

    @Test
    fun sheetTeamLight() {
        paparazzi.snapshotScreen(darkTheme = false) { SheetFrame(sheet) }
    }

    @Test
    fun sheetInventoryDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame(sheet.copy(tab = PlayerTab.INVENTORY)) }
    }

    @Test
    fun sheetPalboxDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame(sheet.copy(tab = PlayerTab.PALBOX)) }
    }

    @Test
    fun sheetProgressionDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame(sheet.copy(tab = PlayerTab.PROGRESSION)) }
    }

    @Test
    fun sheetTechsDark() {
        paparazzi.snapshotScreen(darkTheme = true) { SheetFrame(sheet.copy(tab = PlayerTab.TECHS)) }
    }

    @Test
    fun sheetTechsLight() {
        paparazzi.snapshotScreen(darkTheme = false) { SheetFrame(sheet.copy(tab = PlayerTab.TECHS)) }
    }
}
