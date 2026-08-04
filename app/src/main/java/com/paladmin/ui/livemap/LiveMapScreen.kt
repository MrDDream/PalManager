package com.paladmin.ui.livemap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.annotation.StringRes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.paladmin.R

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 6f

private enum class MapTab(@StringRes val labelRes: Int, val image: String) {
    WORLD(R.string.livemap_tab_world, "file:///android_asset/images/world_map.webp"),
    TREE(R.string.livemap_tab_tree, "file:///android_asset/images/tree_map.webp"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    onBack: () -> Unit,
    viewModel: LiveMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(if (state.focusMarker?.onWorldTree == true) MapTab.TREE else MapTab.WORLD) }
    var focusedPlayerName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.livemap_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.errorMessage ?: "", modifier = Modifier.padding(24.dp))
            }
            else -> {
                val allPlayers = if (selectedTab == MapTab.WORLD) state.worldPlayers else state.treePlayers
                val visiblePlayers = focusedPlayerName?.let { name -> allPlayers.filter { it.name == name } } ?: allPlayers
                val focusMarker = state.focusMarker?.takeIf { it.onWorldTree == (selectedTab == MapTab.TREE) }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        MapTab.entries.forEach { tab ->
                            FilterChip(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab; focusedPlayerName = null },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        PanZoomMap(mapImage = selectedTab.image, players = visiblePlayers, focusMarker = focusMarker, key = selectedTab)
                    }

                    if (allPlayers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.livemap_empty_players), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(allPlayers, key = { it.name }) { player ->
                                val isFocused = focusedPlayerName == player.name
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .clickable { focusedPlayerName = if (isFocused) null else player.name },
                                    colors = if (isFocused) {
                                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        CardDefaults.cardColors()
                                    },
                                ) {
                                    ListItem(
                                        headlineContent = { Text(player.name) },
                                        supportingContent = { Text(stringResource(R.string.livemap_player_level_fmt, player.level)) },
                                        leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                                        colors = if (isFocused) {
                                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        } else {
                                            ListItemDefaults.colors()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanZoomMap(mapImage: String, players: List<LiveMapPlayer>, focusMarker: LiveMapBaseMarker?, key: Any) {
    var scale by remember(key) { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember(key) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(key) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    // Zoom centré sur le point de pincement : on retrouve le point du monde sous le
                    // doigt avant le zoom, puis on replace ce même point sous le doigt après.
                    val worldX = (centroid.x - offset.x) / scale
                    val worldY = (centroid.y - offset.y) / scale
                    val newOffset = Offset(
                        centroid.x - worldX * newScale + pan.x,
                        centroid.y - worldY * newScale + pan.y,
                    )

                    val maxOffsetX = 0f
                    val minOffsetX = size.width - size.width * newScale
                    val maxOffsetY = 0f
                    val minOffsetY = size.height - size.height * newScale
                    scale = newScale
                    offset = Offset(
                        newOffset.x.coerceIn(minOffsetX, maxOffsetX),
                        newOffset.y.coerceIn(minOffsetY, maxOffsetY),
                    )
                }
            },
    ) {
        val density = LocalDensity.current
        // Le BoxWithConstraints est déjà forcé carré par l'appelant (aspectRatio(1f)), donc
        // maxWidth == maxHeight ici — plus besoin de recalculer un côté carré à la main.
        val mapSizeDp = maxWidth
        val mapSizePx = with(density) { mapSizeDp.toPx() }

        Box(
            modifier = Modifier
                .size(mapSizeDp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            AsyncImage(
                model = mapImage,
                contentDescription = stringResource(R.string.livemap_map_cd),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            val markerHalfDp = 14.dp
            players.forEach { player ->
                val xDp = with(density) { (mapSizePx * player.xPercent / 100f).toDp() } - markerHalfDp
                val yDp = with(density) { (mapSizePx * player.yPercent / 100f).toDp() } - markerHalfDp
                Box(
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .graphicsLayer {
                            // Contre-échelle : le marqueur garde une taille constante à l'écran au lieu de
                            // grossir avec le zoom de la carte (sinon il devient illisible ou minuscule).
                            scaleX = 1f / scale
                            scaleY = 1f / scale
                        },
                ) {
                    PlayerMarker(name = player.name, level = player.level)
                }
            }

            focusMarker?.let { marker ->
                val xDp = with(density) { (mapSizePx * marker.xPercent / 100f).toDp() } - markerHalfDp
                val yDp = with(density) { (mapSizePx * marker.yPercent / 100f).toDp() } - markerHalfDp
                Box(
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .graphicsLayer { scaleX = 1f / scale; scaleY = 1f / scale },
                ) {
                    BaseMarker()
                }
            }
        }
    }
}

/** Marqueur de base de guilde — distinct des joueurs (pin coloré au lieu de l'icône personnage). */
@Composable
private fun BaseMarker() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = stringResource(R.string.livemap_base_marker_cd),
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Icône façon PalSite (livemap/icons/player.webp) — le pseudo n'apparaît qu'au tap, comme le hover desktop de PalSite. */
@Composable
private fun PlayerMarker(name: String, level: Int) {
    var showLabel by remember { mutableStateOf(false) }

    Box {
        AsyncImage(
            model = "file:///android_asset/images/player_marker.webp",
            contentDescription = stringResource(R.string.livemap_player_level_fmt, level).let { "$name ($it)" },
            modifier = Modifier
                .size(28.dp)
                .clickable { showLabel = !showLabel },
        )
        if (showLabel) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 32.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(6.dp)),
            ) {
                Text(
                    text = stringResource(R.string.livemap_marker_label_fmt, name, level),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
