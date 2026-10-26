package com.paladmin.ui.bans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.remote.paldefender.BanEntry
import com.paladmin.ui.components.CardTitle
import com.paladmin.ui.components.ConfirmDialog
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.theme.PalTheme

@Composable
fun BansScreen(
    onBack: () -> Unit,
    viewModel: BansViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var ipInput by rememberSaveable { mutableStateOf("") }
    var showBanIpConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    BansContent(
        state = state,
        ipInput = ipInput,
        snackbarHostState = snackbarHostState,
        actions = BansActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onIpChange = { ipInput = it },
            onBanIp = { if (ipInput.isNotBlank()) showBanIpConfirm = true },
            onUnbanIp = { if (ipInput.isNotBlank()) viewModel.unbanIp(ipInput) },
            onUnbanPlayer = { ban -> viewModel.unbanPlayer(ban.uid) },
        ),
    )

    if (showBanIpConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.bans_ban_ip_confirm_title_fmt, ipInput),
            message = stringResource(R.string.bans_ban_ip_confirm_message),
            confirmLabel = stringResource(R.string.bans_ban_action),
            onConfirm = { viewModel.banIp(ipInput); showBanIpConfirm = false },
            onDismiss = { showBanIpConfirm = false },
        )
    }
}

data class BansActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onIpChange: (String) -> Unit = {},
    val onBanIp: () -> Unit = {},
    val onUnbanIp: () -> Unit = {},
    val onUnbanPlayer: (BanEntry) -> Unit = {},
)

@Composable
fun BansContent(
    state: BansUiState,
    ipInput: String,
    actions: BansActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(title = stringResource(R.string.bans_title), onBack = actions.onBack) {
                if (state.isLoading) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                } else {
                    PalIconButton(Icons.Filled.Refresh, stringResource(R.string.cd_refresh), actions.onRefresh)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item { IpBanCard(ipInput, actions) }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp),
                ) {
                    SectionLabel(stringResource(R.string.bans_banned_players_header), modifier = Modifier.weight(1f))
                    if (!state.isLoading) {
                        Text(state.bans.size.toString(), style = PalTheme.type.mono.copy(fontSize = 12.5.sp), color = c.textMuted)
                    }
                }
            }

            when {
                state.isLoading && state.bans.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.green.solid)
                    }
                }
                state.bans.isEmpty() -> item {
                    Text(
                        stringResource(R.string.bans_empty),
                        style = PalTheme.type.body.copy(fontSize = 14.sp),
                        color = c.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                }
                else -> items(state.bans, key = { it.uid }) { ban ->
                    BannedPlayerCard(ban, onUnban = { actions.onUnbanPlayer(ban) })
                }
            }
        }
    }
}

@Composable
private fun IpBanCard(ipInput: String, actions: BansActions) {
    val c = PalTheme.colors
    PalCard(spacing = 10.dp, modifier = Modifier.fillMaxWidth()) {
        CardTitle(stringResource(R.string.bans_ip_ban_header))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PalTextField(
                value = ipInput,
                onValueChange = actions.onIpChange,
                placeholder = stringResource(R.string.bans_ip_label),
                mono = true,
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.weight(1f),
            )
            PalButton(
                text = stringResource(R.string.bans_ban_action),
                onClick = actions.onBanIp,
                tone = c.danger,
                enabled = ipInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                textStyle = PalTheme.type.button.copy(fontSize = 14.sp),
            )
        }
        // Pas de GET des IP bannies côté API : on débannit une adresse saisie à la main.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(c.background, RoundedCornerShape(12.dp))
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                stringResource(R.string.manage_bans_ip_unban_hint),
                style = PalTheme.type.caption,
                color = c.textMuted,
                modifier = Modifier.weight(1f),
            )
            TextAction(
                text = stringResource(R.string.bans_unban_action),
                enabled = ipInput.isNotBlank(),
                onClick = actions.onUnbanIp,
            )
        }
    }
}

/** Bouton texte vert 44 dp (action secondaire "Débannir"). */
@Composable
private fun TextAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text,
            style = PalTheme.type.label.copy(fontWeight = FontWeight.Medium),
            color = c.green.onContainer.copy(alpha = if (enabled) 1f else 0.45f),
            maxLines = 1,
        )
    }
}

@Composable
private fun BannedPlayerCard(ban: BanEntry, onUnban: () -> Unit) {
    val c = PalTheme.colors
    val name = ban.name.ifBlank { ban.uid }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(c.danger.container, CircleShape),
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = PalTheme.type.cardTitle.copy(fontSize = 15.sp),
                color = c.danger.onContainer,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                style = PalTheme.type.sectionTitle,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                ban.reason.ifBlank { stringResource(R.string.bans_no_reason) },
                style = PalTheme.type.bodySmall,
                color = if (ban.reason.isBlank()) c.textMuted else c.danger.onContainer.copy(alpha = 0.85f),
            )
            if (ban.uid.isNotBlank()) {
                Text(ban.uid, style = PalTheme.type.mono.copy(fontSize = 11.5.sp), color = c.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val shape = RoundedCornerShape(10.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(shape)
                .border(1.dp, c.borderStrong, shape)
                .clickable(role = Role.Button, onClick = onUnban)
                .padding(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.bans_unban_action), style = PalTheme.type.label.copy(fontWeight = FontWeight.Medium), color = c.text, maxLines = 1)
        }
    }
}
