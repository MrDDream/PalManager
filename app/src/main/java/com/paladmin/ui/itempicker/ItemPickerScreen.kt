package com.paladmin.ui.itempicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.db.ItemEntity
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.ChoiceGrid
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.GiveDetailHeader
import com.paladmin.ui.components.GiveDetailSection
import com.paladmin.ui.components.GiveSheet
import com.paladmin.ui.components.GiveStepper
import com.paladmin.ui.components.GiveThumb
import com.paladmin.ui.components.KeyValueRow
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalChip
import com.paladmin.ui.components.PalDivider
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.PlayerPickerSheet
import com.paladmin.ui.components.PlayerTargetChip
import com.paladmin.ui.components.resolveGiveTarget
import com.paladmin.ui.theme.PalTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val statsJson = Json { ignoreUnknownKeys = true }

private val QUANTITY_PRESETS = listOf(1, 10, 100)

private val CATEGORY_LABELS = mapOf(
    "Weapon" to R.string.give_cat_weapon,
    "Sphere" to R.string.give_cat_sphere,
    "Sphere_Module" to R.string.give_cat_sphere_module,
    "Armor" to R.string.give_cat_armor,
    "Accessory" to R.string.give_cat_accessory,
    "Material" to R.string.give_cat_material,
    "Consumable" to R.string.give_cat_consumable,
    "Ammo" to R.string.give_cat_ammo,
    "Ingredient" to R.string.give_cat_ingredient,
    "Key_Items" to R.string.give_cat_key_items,
    "Glider" to R.string.give_cat_glider,
    "Schematic" to R.string.give_cat_schematic,
)

/** Ordre d'affichage des chips (les plus demandées d'abord, comme la maquette) ; inconnues à la fin. */
private val CATEGORY_ORDER = listOf(
    "Sphere", "Material", "Weapon", "Ingredient", "Consumable", "Key_Items",
    "Ammo", "Armor", "Accessory", "Sphere_Module", "Glider", "Schematic",
)

/** Libellé traduit d'une catégorie du catalogue (repli sur l'identifiant brut si inconnue). */
@Composable
fun itemCategoryLabel(category: String): String =
    CATEGORY_LABELS[category]?.let { stringResource(it) } ?: category.replace('_', ' ')

private fun itemImage(item: ItemEntity) = "file:///android_asset/images/items/${item.image}"

private fun ItemEntity.displayName() = nameFr.ifBlank { id }

data class ItemPickerUiState(
    val query: String = "",
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val results: List<ItemEntity> = emptyList(),
    val target: PalworldPlayer? = null,
    val selectedItem: ItemEntity? = null,
    val quantity: Int = 1,
)

data class ItemPickerActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onCategorySelected: (String?) -> Unit = {},
    val onSelectItem: (ItemEntity) -> Unit = {},
    val onQuantityChange: (Int) -> Unit = {},
    val onOpenPlayerPicker: () -> Unit = {},
    val onGive: () -> Unit = {},
    val onShowDetails: () -> Unit = {},
)

@Composable
fun ItemPickerScreen(
    onBack: () -> Unit,
    viewModel: ItemPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val results by viewModel.results.collectAsState()
    val players by viewModel.players.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var targetUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val target = resolveGiveTarget(players, targetUserId)

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    ItemPickerContent(
        state = ItemPickerUiState(
            query = query,
            categories = categories,
            selectedCategory = selectedCategory,
            results = results,
            target = target,
            selectedItem = selectedItem,
            quantity = quantity,
        ),
        actions = ItemPickerActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onSelectItem = { selectedItem = it },
            onQuantityChange = { quantity = it.coerceAtLeast(1) },
            onOpenPlayerPicker = { showPlayerPicker = true; viewModel.loadPlayers() },
            onGive = {
                val item = selectedItem
                when {
                    item == null -> Unit
                    target == null -> { showPlayerPicker = true; viewModel.loadPlayers() }
                    else -> viewModel.giveItem(item, target.userId, quantity)
                }
            },
            onShowDetails = { showDetails = true },
        ),
        snackbarHostState = snackbarHostState,
    )

    if (showPlayerPicker) {
        PlayerPickerSheet(
            players = players,
            selectedUserId = target?.userId,
            onSelect = { targetUserId = it.userId },
            onRefresh = viewModel::loadPlayers,
            onDismiss = { showPlayerPicker = false },
        )
    }

    val detailItem = selectedItem
    if (showDetails && detailItem != null) {
        GiveSheet(onDismiss = { showDetails = false }) { ItemDetailContent(detailItem) }
    }
}

@Composable
fun ItemPickerContent(
    state: ItemPickerUiState,
    actions: ItemPickerActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(R.string.itempicker_title),
                onBack = actions.onBack,
                actions = { PlayerTargetChip(state.target, actions.onOpenPlayerPicker, Modifier.padding(end = 8.dp)) },
            )
        },
        bottomBar = { ItemGiveBar(state, actions) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PalSearchBar(
                value = state.query,
                onValueChange = actions.onQueryChange,
                placeholder = stringResource(R.string.common_search_hint),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            val groupCd = stringResource(R.string.give_category_group_cd)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = groupCd },
            ) {
                item(key = "__all") {
                    PalChip(stringResource(R.string.filter_all_fem), state.selectedCategory == null, { actions.onCategorySelected(null) }, tone = c.amber)
                }
                val ordered = state.categories.sortedBy { CATEGORY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
                items(ordered, key = { it }) { category ->
                    PalChip(itemCategoryLabel(category), state.selectedCategory == category, { actions.onCategorySelected(category) }, tone = c.amber)
                }
            }
            Spacer(Modifier.height(10.dp))

            if (state.results.isEmpty() && (state.query.isNotBlank() || state.selectedCategory != null)) {
                EmptyState(
                    title = stringResource(R.string.give_empty_results_title),
                    message = stringResource(R.string.give_empty_results_message),
                    icon = Icons.Filled.SearchOff,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { item ->
                        ItemTile(item, selected = item.id == state.selectedItem?.id, onClick = { actions.onSelectItem(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemTile(item: ItemEntity, selected: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(if (selected) c.amber.container else c.surface)
            .border(2.dp, if (selected) c.amber.solid else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        FallbackAsyncImage(model = itemImage(item), modifier = Modifier.size(52.dp))
        Text(
            item.displayName(),
            style = PalTheme.type.caption.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
            color = c.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ItemGiveBar(state: ItemPickerUiState, actions: ItemPickerActions) {
    val c = PalTheme.colors
    val item = state.selectedItem
    BottomActionBar {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (item != null) {
                GiveThumb(itemImage(item), c.surfaceRaised)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.displayName(), style = PalTheme.type.cardTitle.copy(fontSize = 16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val firstLine = item.description.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
                    Text(
                        listOfNotNull(itemCategoryLabel(item.category), firstLine).joinToString(" · "),
                        style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
                        color = c.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PalIconButton(Icons.Outlined.Info, stringResource(R.string.give_details_cd), onClick = actions.onShowDetails, tint = c.textMuted)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp).background(c.surfaceRaised, RoundedCornerShape(14.dp))) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.give_item_none_selected), style = PalTheme.type.cardTitle.copy(fontSize = 16.sp), color = c.textSecondary)
                    Text(stringResource(R.string.give_item_none_hint), style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp), color = c.textMuted)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GiveStepper(
                value = state.quantity,
                onValueChange = actions.onQuantityChange,
                decreaseCd = stringResource(R.string.give_decrease_cd),
                increaseCd = stringResource(R.string.give_increase_cd),
                groupCd = stringResource(R.string.give_quantity_cd),
            )
            ChoiceGrid(
                options = QUANTITY_PRESETS.map { it to stringResource(R.string.give_preset_fmt, it) },
                selected = state.quantity.takeIf { it in QUANTITY_PRESETS },
                onSelect = actions.onQuantityChange,
                tone = c.amber,
                textStyle = PalTheme.type.mono.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
        }
        PalButton(
            text = when {
                item == null -> stringResource(R.string.common_give)
                state.target == null -> stringResource(R.string.give_choose_player_cta)
                else -> stringResource(R.string.give_item_cta_fmt, item.displayName(), state.quantity)
            },
            onClick = actions.onGive,
            enabled = item != null,
            tone = c.amber,
            height = 52.dp,
            shape = RoundedCornerShape(16.dp),
            textStyle = PalTheme.type.button.copy(fontSize = 15.5.sp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Fiche détail d'un item (feuille du bas) : description complète, caractéristiques, ID. */
@Composable
fun ItemDetailContent(item: ItemEntity) {
    val c = PalTheme.colors
    val stats = remember(item.statsJson) {
        runCatching { statsJson.decodeFromString<Map<String, String>>(item.statsJson) }.getOrDefault(emptyMap())
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
    ) {
        GiveDetailHeader(itemImage(item), c.amber.container, item.displayName(), item.id) {
            Text(itemCategoryLabel(item.category), style = PalTheme.type.label, color = c.amber.content)
        }
        if (item.description.isNotBlank()) {
            GiveDetailSection(stringResource(R.string.give_detail_description)) {
                Text(item.description.trim(), style = PalTheme.type.bodySmall, color = c.textBody)
            }
        }
        if (stats.isNotEmpty()) {
            GiveDetailSection(stringResource(R.string.give_detail_stats)) {
                stats.entries.forEachIndexed { index, (label, value) ->
                    if (index > 0) PalDivider()
                    KeyValueRow(label, value)
                }
            }
        }
    }
}
