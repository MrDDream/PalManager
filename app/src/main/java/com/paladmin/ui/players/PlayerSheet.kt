package com.paladmin.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.components.DetailError
import com.paladmin.ui.components.DetailLoading
import com.paladmin.ui.components.DetailRowsList
import com.paladmin.ui.components.DetailSheet
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.InitialAvatar
import com.paladmin.ui.components.PalGridContent
import com.paladmin.ui.components.PalGroup
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalInfo
import com.paladmin.ui.components.PalInfoDialog
import com.paladmin.ui.components.PalTeamCard
import com.paladmin.ui.theme.PalTheme

private val TAB_CONTENT_PADDING = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp)

/**
 * Fiche joueur en feuille du bas (maquette "Fiche joueur") : en-tête (avatar, plateforme, niveau,
 * latence, menu de modération) puis 5 onglets — Équipe, Inventaire, Palbox, Progrès, Techs —
 * chargés à la première visite. Remplace les anciennes boîtes de dialogue séparées.
 */
@Composable
fun PlayerSheet(
    state: PlayerSheetUiState,
    onSelectTab: (PlayerTab) -> Unit,
    onRetry: () -> Unit,
    onMessage: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit,
    onDismiss: () -> Unit,
) {
    DetailSheet(onDismiss = onDismiss) {
        PlayerSheetContent(
            state = state,
            onSelectTab = onSelectTab,
            onRetry = onRetry,
            onMessage = onMessage,
            onKick = onKick,
            onBan = onBan,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

/** Contenu stateless de [PlayerSheet] (sans la feuille), testable. */
@Composable
fun PlayerSheetContent(
    state: PlayerSheetUiState,
    onSelectTab: (PlayerTab) -> Unit,
    onRetry: () -> Unit = {},
    onMessage: () -> Unit = {},
    onKick: () -> Unit = {},
    onBan: () -> Unit = {},
    modifier: Modifier = Modifier,
    initialExpandedTeamIndex: Int? = 0,
) {
    val c = PalTheme.colors
    val player = state.player
    var selectedPal by remember(player.userId) { mutableStateOf<PalInfo?>(null) }
    var expandedTeam by remember(player.userId) { mutableStateOf(initialExpandedTeamIndex) }
    val sheetTitle = player.name

    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding().semantics { paneTitle = sheetTitle }) {
        SheetHeader(state, onMessage, onKick, onBan)
        SheetTabs(selected = state.tab, onSelect = onSelectTab)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (state.tab) {
                PlayerTab.TEAM -> {
                    val pals = state.pals
                    when {
                        pals == null || pals.isLoading -> DetailLoading()
                        pals.error != null -> DetailError(pals.error, onRetry = onRetry)
                        pals.team.isEmpty() -> EmptyState(stringResource(R.string.players_v2_team_empty), icon = Icons.Filled.Pets, modifier = Modifier.fillMaxWidth())
                        else -> LazyColumn(
                            contentPadding = TAB_CONTENT_PADDING,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            itemsIndexed(pals.team) { index, pal ->
                                PalTeamCard(
                                    pal = pal,
                                    expanded = expandedTeam == index,
                                    onToggle = { expandedTeam = if (expandedTeam == index) null else index },
                                )
                            }
                        }
                    }
                }
                PlayerTab.PALBOX -> {
                    val pals = state.pals
                    val groups = remember(pals?.palbox) { listOf(PalGroup(label = null, pals = pals?.palbox.orEmpty())) }
                    PalGridContent(
                        groups = groups,
                        searchable = true,
                        isLoading = pals == null || pals.isLoading,
                        error = pals?.error,
                        onPalClick = { selectedPal = it },
                        contentPadding = TAB_CONTENT_PADDING,
                        onRetry = onRetry,
                    )
                }
                PlayerTab.INVENTORY -> DetailTab(state.inventory, onRetry)
                PlayerTab.PROGRESSION -> DetailTab(state.progression, onRetry)
                PlayerTab.TECHS -> DetailTab(state.techs, onRetry)
            }
        }
    }
    selectedPal?.let { PalInfoDialog(it, onDismiss = { selectedPal = null }) }
}

@Composable
private fun DetailTab(tab: DetailTabState?, onRetry: () -> Unit) {
    DetailRowsList(
        isLoading = tab == null || tab.isLoading,
        error = tab?.error,
        rows = tab?.rows.orEmpty(),
        contentPadding = TAB_CONTENT_PADDING,
        onRetry = onRetry,
    )
}

@Composable
private fun SheetHeader(state: PlayerSheetUiState, onMessage: () -> Unit, onKick: () -> Unit, onBan: () -> Unit) {
    val c = PalTheme.colors
    val player = state.player
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
    ) {
        InitialAvatar(player.name, size = 52.dp, seed = player.userId.ifBlank { player.name })
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    player.name,
                    style = PalTheme.type.screenTitle.copy(fontSize = 20.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                platformLabel(player.userId)?.let { PlatformBadge(it) }
            }
            Text(
                stringResource(R.string.players_v2_sheet_subtitle_fmt, player.level, player.ping.toInt()),
                style = PalTheme.type.bodySmall,
                color = c.textMuted,
                maxLines = 1,
            )
        }
        Box {
            PalIconButton(
                icon = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cd_actions),
                onClick = { menuOpen = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceRaised)
                    .border(1.dp, c.borderStrong, RoundedCornerShape(12.dp)),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.surfaceRaised) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.players_action_message), style = PalTheme.type.body, color = c.green.onContainer) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = c.green.content) },
                    onClick = { menuOpen = false; onMessage() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.players_action_kick), style = PalTheme.type.body, color = c.amber.onContainer) },
                    leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = c.amber.content) },
                    onClick = { menuOpen = false; onKick() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.players_action_ban), style = PalTheme.type.body, color = c.danger.onContainer) },
                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = c.danger.content) },
                    onClick = { menuOpen = false; onBan() },
                )
            }
        }
    }
}

/** Rail d'onglets de la fiche (5 onglets courts, 12,5 sp pour tenir sur un écran de 360 dp). */
@Composable
private fun SheetTabs(selected: PlayerTab, onSelect: (PlayerTab) -> Unit) {
    val c = PalTheme.colors
    val railLabel = stringResource(R.string.players_v2_tabs_label)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(c.field, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .semantics { contentDescription = railLabel },
    ) {
        PlayerTab.entries.forEach { tab ->
            val on = tab == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.divider else c.field)
                    .semantics { this.selected = on }
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(horizontal = 2.dp),
            ) {
                Text(
                    stringResource(tab.labelRes),
                    style = PalTheme.type.label.copy(fontSize = 12.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (on) c.text else c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
