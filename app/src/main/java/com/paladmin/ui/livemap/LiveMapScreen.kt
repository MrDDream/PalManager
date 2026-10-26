package com.paladmin.ui.livemap

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.InfoNote
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.theme.PalTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 6f
private const val BUTTON_ZOOM_STEP = 1.6f

enum class LiveMapTab(@StringRes val labelRes: Int, val image: String) {
    WORLD(R.string.livemap_tab_world, "file:///android_asset/images/world_map.webp"),
    TREE(R.string.livemap_tab_tree, "file:///android_asset/images/tree_map.webp"),
}

@Composable
fun LiveMapScreen(
    onBack: () -> Unit,
    viewModel: LiveMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(if (state.focusMarkers.any { it.onWorldTree }) LiveMapTab.TREE else LiveMapTab.WORLD) }
    var focusedPlayerName by rememberSaveable { mutableStateOf<String?>(null) }

    LiveMapContent(
        state = state,
        selectedTab = selectedTab,
        focusedPlayerName = focusedPlayerName,
        actions = LiveMapActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onSelectTab = { tab -> selectedTab = tab; focusedPlayerName = null },
            onFocusPlayer = { name -> focusedPlayerName = if (focusedPlayerName == name) null else name },
        ),
    )
}

data class LiveMapActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onSelectTab: (LiveMapTab) -> Unit = {},
    /** Sélectionne un joueur (ou le désélectionne s'il l'était déjà). */
    val onFocusPlayer: (String) -> Unit = {},
)

/**
 * Carte plein écran façon maquette : la carte occupe tout l'écran (recadrée pour le couvrir), les
 * commandes flottent par-dessus et une feuille fixe en bas liste les joueurs de l'onglet courant.
 */
@Composable
fun LiveMapContent(
    state: LiveMapUiState,
    selectedTab: LiveMapTab,
    focusedPlayerName: String?,
    actions: LiveMapActions,
) {
    val c = PalTheme.colors
    val players = if (selectedTab == LiveMapTab.WORLD) state.worldPlayers else state.treePlayers
    val bases = state.focusMarkers.filter { it.onWorldTree == (selectedTab == LiveMapTab.TREE) }
    val otherTabBases = state.focusMarkers.size - bases.size
    val focused = players.firstOrNull { it.name == focusedPlayerName }
    val viewport = remember(selectedTab) { MapViewport(initialFocus = bases.firstOrNull()?.let { it.xPercent to it.yPercent }) }

    Box(modifier = Modifier.fillMaxSize().background(c.background)) {
        PanZoomMap(
            image = selectedTab.image,
            viewport = viewport,
            players = players,
            bases = bases,
            focusedName = focused?.name,
            onPlayerClick = { actions.onFocusPlayer(it.name) },
        )

        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            MapHeader(selectedTab, actions)
            ZoomControl(
                onZoomIn = { viewport.zoomAroundCenter(BUTTON_ZOOM_STEP) },
                onZoomOut = { viewport.zoomAroundCenter(1f / BUTTON_ZOOM_STEP) },
                modifier = Modifier.align(Alignment.End).padding(top = 12.dp, end = 12.dp),
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(color = c.green.solid, modifier = Modifier.align(Alignment.Center))
        }

        PlayersPanel(
            state = state,
            players = players,
            focused = focused,
            basesShown = bases.size,
            otherTabBases = otherTabBases,
            onChipClick = { player ->
                if (player.name != focused?.name) viewport.centerOn(player.xPercent, player.yPercent)
                actions.onFocusPlayer(player.name)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Zoom / déplacement de la carte, partagé entre les gestes et les boutons +/−. */
@Stable
private class MapViewport(private val initialFocus: Pair<Float, Float>?) {
    var scale by mutableFloatStateOf(MIN_SCALE)
    var offset by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
        private set

    /** Côté de la carte (carrée) à l'échelle 1 : elle couvre tout l'écran, comme dans la maquette. */
    val side: Float get() = max(size.width, size.height).toFloat()

    fun onSize(newSize: IntSize) {
        val first = size == IntSize.Zero
        size = newSize
        if (first) {
            val focus = initialFocus
            if (focus != null) centerOn(focus.first, focus.second) else centerOn(50f, 50f)
        } else {
            offset = clamp(offset, scale)
        }
    }

    private fun clamp(o: Offset, s: Float): Offset {
        val scaled = side * s
        val minX = min(size.width - scaled, 0f)
        val minY = min(size.height - scaled, 0f)
        return Offset(o.x.coerceIn(minX, 0f), o.y.coerceIn(minY, 0f))
    }

    /** Zoom centré sur [centroid] : le point de la carte sous le doigt y reste après le zoom. */
    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val mapX = (centroid.x - offset.x) / scale
        val mapY = (centroid.y - offset.y) / scale
        offset = clamp(Offset(centroid.x - mapX * newScale + pan.x, centroid.y - mapY * newScale + pan.y), newScale)
        scale = newScale
    }

    fun zoomAroundCenter(factor: Float) = transform(Offset(size.width / 2f, size.height / 2f), Offset.Zero, factor)

    fun centerOn(xPercent: Float, yPercent: Float) {
        val scaled = side * scale
        offset = clamp(Offset(size.width / 2f - scaled * xPercent / 100f, size.height / 2f - scaled * yPercent / 100f), scale)
    }

    fun screenPosition(xPercent: Float, yPercent: Float): Offset {
        val scaled = side * scale
        return Offset(offset.x + scaled * xPercent / 100f, offset.y + scaled * yPercent / 100f)
    }
}

/** Place l'élément de sorte que son point (fx, fy) — fractions de sa taille — tombe sur [position]. */
private fun Modifier.anchoredAt(position: () -> Offset, fx: Float, fy: Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val p = position()
        placeable.place((p.x - placeable.width * fx).roundToInt(), (p.y - placeable.height * fy).roundToInt())
    }
}

@Composable
private fun PanZoomMap(
    image: String,
    viewport: MapViewport,
    players: List<LiveMapPlayer>,
    bases: List<LiveMapBaseMarker>,
    focusedName: String?,
    onPlayerClick: (LiveMapPlayer) -> Unit,
) {
    val c = PalTheme.colors
    val density = LocalDensity.current
    val mapCd = stringResource(R.string.livemap_map_cd)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(c.background)
            .onSizeChanged(viewport::onSize)
            .pointerInput(viewport) {
                detectTransformGestures { centroid, pan, zoom, _ -> viewport.transform(centroid, pan, zoom) }
            },
    ) {
        if (viewport.size != IntSize.Zero) {
            val sideDp = with(density) { viewport.side.toDp() }
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(sideDp)
                    .graphicsLayer {
                        scaleX = viewport.scale
                        scaleY = viewport.scale
                        translationX = viewport.offset.x
                        translationY = viewport.offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .semantics { contentDescription = mapCd },
            ) {
                FallbackAsyncImage(model = image, modifier = Modifier.fillMaxSize())
            }

            // Marqueurs hors du calque zoomé : ils gardent une taille constante à l'écran.
            bases.forEach { base ->
                BaseMarker(Modifier.anchoredAt({ viewport.screenPosition(base.xPercent, base.yPercent) }, 0.5f, 0.5f))
            }
            players.forEach { player ->
                PlayerMarker(
                    player = player,
                    focused = player.name == focusedName,
                    onClick = { onPlayerClick(player) },
                    modifier = Modifier.anchoredAt({ viewport.screenPosition(player.xPercent, player.yPercent) }, 0.5f, 1f),
                )
            }
        }
    }
}

/** Couleur stable d'un joueur (même cycle que les avatars) : fond + encre lisible dessus. */
@Composable
private fun playerColors(player: LiveMapPlayer): Pair<Color, Color> {
    val palette = PalTheme.colors.avatars
    return palette[Math.floorMod(player.userId.ifBlank { player.name }.hashCode(), palette.size)]
}

@Composable
private fun PlayerMarker(player: LiveMapPlayer, focused: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val (color, ink) = playerColors(player)
    val cd = "${player.name} (${stringResource(R.string.livemap_player_level_fmt, player.level)})"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cd; selected = focused },
    ) {
        Text(
            player.name,
            style = PalTheme.type.label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
            color = ink,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 4.dp, start = 2.dp, end = 2.dp)
                .shadow(4.dp, CircleShape)
                .background(color, CircleShape)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
        val dot = if (focused) 20.dp else 14.dp
        Box(
            Modifier
                .size(dot)
                .background(c.background, CircleShape)
                .padding(3.dp)
                .background(color, CircleShape),
        )
    }
}

/** Camp de base de guilde : carré à contour ambre, distinct des joueurs. */
@Composable
private fun BaseMarker(modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(30.dp)
            .background(c.background.copy(alpha = 0.85f), shape)
            .border(1.5.dp, c.amber.solid, shape),
    ) {
        Icon(Icons.Filled.Cabin, contentDescription = stringResource(R.string.livemap_base_marker_cd), tint = c.amber.content, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun floatingBackground(): Color = PalTheme.colors.surfaceBar.copy(alpha = 0.92f)

@Composable
private fun MapHeader(selectedTab: LiveMapTab, actions: LiveMapActions) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
    ) {
        Box(Modifier.background(floatingBackground(), shape)) {
            PalIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), actions.onBack, tint = c.text)
        }
        val tabsCd = stringResource(R.string.manage_map_tabs_cd)
        Row(
            modifier = Modifier
                .weight(1f)
                .background(floatingBackground(), shape)
                .padding(4.dp)
                .semantics { contentDescription = tabsCd },
        ) {
            LiveMapTab.entries.forEach { tab ->
                val on = tab == selectedTab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) c.borderStrong else Color.Transparent)
                        .clickable(role = Role.Tab) { actions.onSelectTab(tab) }
                        .semantics { selected = on },
                ) {
                    Text(
                        stringResource(tab.labelRes),
                        style = PalTheme.type.label.copy(fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (on) c.text else c.textBody,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(Modifier.background(floatingBackground(), shape)) {
            PalIconButton(Icons.Filled.Refresh, stringResource(R.string.cd_refresh), actions.onRefresh, tint = c.text)
        }
    }
}

@Composable
private fun ZoomControl(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(modifier = modifier.background(floatingBackground(), RoundedCornerShape(14.dp))) {
        PalIconButton(Icons.Filled.Add, stringResource(R.string.manage_map_zoom_in), onZoomIn, tint = c.text, iconSize = 20.dp)
        Box(Modifier.padding(horizontal = 8.dp).width(28.dp).height(1.dp).background(c.borderStrong))
        PalIconButton(Icons.Filled.Remove, stringResource(R.string.manage_map_zoom_out), onZoomOut, tint = c.text, iconSize = 20.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersPanel(
    state: LiveMapUiState,
    players: List<LiveMapPlayer>,
    focused: LiveMapPlayer?,
    basesShown: Int,
    otherTabBases: Int,
    onChipClick: (LiveMapPlayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    // Liseré du haut : la couleur de bordure dépasse d'1 dp au-dessus de la feuille.
    Box(modifier = modifier.fillMaxWidth().background(c.border, shape).padding(top = 1.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surfaceBar, shape)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (focused != null) stringResource(R.string.manage_map_focus_title_fmt, focused.name, focused.level)
                    else stringResource(R.string.manage_map_players_title),
                    style = PalTheme.type.cardTitle.copy(fontSize = 16.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val info = when {
                    focused != null && focused.ping > 0 -> stringResource(R.string.manage_map_ping_fmt, focused.ping)
                    focused == null && state.errorMessage == null -> stringResource(R.string.manage_map_players_count_fmt, players.size)
                    else -> null
                }
                if (info != null) Text(info, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textMuted, maxLines = 1)
            }

            when {
                state.errorMessage != null -> InfoNote(state.errorMessage, icon = Icons.Filled.WifiOff, tone = c.danger)
                players.isEmpty() && !state.isLoading -> Text(stringResource(R.string.livemap_empty_players), style = PalTheme.type.bodySmall, color = c.textMuted)
                else -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 132.dp).verticalScroll(rememberScrollState()),
                ) {
                    players.forEach { player -> PlayerChip(player, player.name == focused?.name) { onChipClick(player) } }
                }
            }

            if (basesShown > 0 || otherTabBases > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(16.dp).border(1.5.dp, c.amber.solid, RoundedCornerShape(5.dp)))
                    Text(
                        if (basesShown > 0) stringResource(R.string.manage_map_bases_fmt, basesShown)
                        else stringResource(R.string.manage_map_bases_other_tab_fmt, otherTabBases),
                        style = PalTheme.type.caption.copy(fontSize = 12.5.sp),
                        color = c.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerChip(player: LiveMapPlayer, on: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    val (color, _) = playerColors(player)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(if (on) c.surfaceRaised else Color.Transparent)
            .border(1.dp, if (on) color else c.borderStrong, CircleShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Checkbox; selected = on }
            .padding(horizontal = 12.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(player.name, style = PalTheme.type.label, color = c.text, maxLines = 1)
    }
}
