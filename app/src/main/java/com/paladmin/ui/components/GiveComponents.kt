package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer
import com.paladmin.ui.theme.PalTheme

// Composants partagés des écrans "Donner" (item, Pal, PNJ) : puce du destinataire et sa feuille
// de sélection, stepper de quantité/niveau, feuille du bas stylée, sections de fiche détail.

/**
 * Destinataire effectif : celui choisi par l'utilisateur s'il est toujours connecté, sinon le seul
 * joueur en ligne quand il n'y en a qu'un (aucune ambiguïté possible), sinon personne.
 */
fun resolveGiveTarget(players: List<PalworldPlayer>, selectedUserId: String?): PalworldPlayer? =
    players.firstOrNull { it.userId == selectedUserId } ?: players.singleOrNull()

/** Feuille du bas aux couleurs de la maquette (même habillage que la feuille Alimentation). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = PalTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceBar,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp).background(c.neutral.solid, RoundedCornerShape(4.dp)))
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            content = content,
        )
    }
}

/**
 * Puce du destinataire dans la barre du haut (avatar + pseudo). [compact] n'affiche que l'avatar
 * (écran Pal, où la barre porte aussi le bouton Template). Sans destinataire : "Choisir".
 */
@Composable
fun PlayerTargetChip(
    player: PalworldPlayer?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = PalTheme.colors
    val cd = if (player != null) stringResource(R.string.give_target_cd_fmt, player.name) else stringResource(R.string.give_target_none_cd)
    if (compact) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = cd },
        ) {
            if (player != null) {
                InitialAvatar(player.name, size = 40.dp, seed = player.userId.ifBlank { player.name })
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).border(1.dp, c.borderStrong, CircleShape),
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
        return
    }
    val selected = player != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (selected) c.green.container else Color.Transparent)
            .border(1.dp, if (selected) c.green.border else c.borderStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd }
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        if (player != null) {
            InitialAvatar(player.name, size = 28.dp, seed = player.userId.ifBlank { player.name })
            Text(
                player.name,
                style = PalTheme.type.label.copy(fontSize = 13.5.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).background(c.surfaceRaised, CircleShape)) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            }
            Text(stringResource(R.string.give_target_choose), style = PalTheme.type.label.copy(fontSize = 13.5.sp), color = c.textSecondary, maxLines = 1)
        }
    }
}

/** Feuille de choix du destinataire : recherche par pseudo (comme [PlayerSearchField]) + liste. */
@Composable
fun PlayerPickerSheet(
    players: List<PalworldPlayer>,
    selectedUserId: String?,
    onSelect: (PalworldPlayer) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    GiveSheet(onDismiss = onDismiss) {
        PlayerPickerContent(
            players = players,
            selectedUserId = selectedUserId,
            onSelect = { onSelect(it); onDismiss() },
            onRefresh = onRefresh,
        )
    }
}

@Composable
fun ColumnScope.PlayerPickerContent(
    players: List<PalworldPlayer>,
    selectedUserId: String?,
    onSelect: (PalworldPlayer) -> Unit,
    onRefresh: () -> Unit,
    initialQuery: String = "",
) {
    val c = PalTheme.colors
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val results = remember(players, query) {
        if (query.isBlank()) players else players.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(Icons.Filled.Person, c.green, size = 44.dp, iconSize = 22.dp, radius = 14.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.give_player_sheet_title),
                style = PalTheme.type.screenTitle,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.give_player_sheet_subtitle_fmt, players.size), style = PalTheme.type.bodySmall, color = c.textMuted)
        }
        PalIconButton(Icons.Filled.Refresh, stringResource(R.string.give_refresh), onClick = onRefresh)
    }

    if (players.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.give_player_empty_title),
            message = stringResource(R.string.give_player_empty_message),
            icon = Icons.Filled.PersonOff,
            modifier = Modifier.fillMaxWidth(),
            action = {
                PalButton(
                    stringResource(R.string.give_refresh),
                    onClick = onRefresh,
                    style = PalButtonStyle.Tonal,
                    icon = Icons.Filled.Refresh,
                    height = 44.dp,
                )
            },
        )
        return
    }

    PalSearchBar(value = query, onValueChange = { query = it }, placeholder = stringResource(R.string.common_player_hint))

    if (results.isEmpty()) {
        Text(
            stringResource(R.string.give_player_no_match),
            style = PalTheme.type.bodySmall,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
    ) {
        items(results, key = { it.userId.ifBlank { it.name } }) { player ->
            val on = player.userId == selectedUserId
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) c.green.container else c.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) c.green.solid else c.border, RoundedCornerShape(16.dp))
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(player) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                InitialAvatar(player.name, size = 38.dp, seed = player.userId.ifBlank { player.name })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(player.name, style = PalTheme.type.body.copy(fontWeight = FontWeight.Medium), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.give_player_level_fmt, player.level), style = PalTheme.type.caption, color = c.textMuted)
                }
                if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = c.green.content, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Stepper − valeur + (quantité, niveau). La valeur centrale reste éditable au clavier pour saisir
 * directement un grand nombre. [min] borne le bas ; pas de borne haute (comme l'ancien champ libre).
 */
@Composable
fun GiveStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    decreaseCd: String,
    increaseCd: String,
    modifier: Modifier = Modifier,
    groupCd: String? = null,
    min: Int = 1,
    valueStyle: TextStyle = PalTheme.type.mono.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    valueWidth: Dp = 52.dp,
) {
    val c = PalTheme.colors
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(c.surfaceRaised, RoundedCornerShape(14.dp))
            .then(if (groupCd != null) Modifier.semantics { contentDescription = groupCd } else Modifier),
    ) {
        StepperButton("−", decreaseCd, enabled = value > min) { onValueChange((value - 1).coerceAtLeast(min)) }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.widthIn(min = valueWidth).heightIn(min = 44.dp)) {
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter(Char::isDigit).take(6)
                    text = digits
                    digits.toIntOrNull()?.takeIf { it >= min }?.let(onValueChange)
                },
                singleLine = true,
                textStyle = valueStyle.copy(color = c.text, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.green.solid),
                modifier = Modifier.widthIn(min = valueWidth, max = 80.dp),
            )
        }
        StepperButton("+", increaseCd, enabled = true) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepperButton(glyph: String, cd: String, enabled: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = cd
                role = Role.Button
            },
    ) {
        Text(
            glyph,
            style = PalTheme.type.body.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            color = if (enabled) c.text else c.textFaint,
        )
    }
}

/** Vignette carrée d'une image du jeu sur fond teinté (barre du bas, fiches détail). */
@Composable
fun GiveThumb(model: String, background: Color, modifier: Modifier = Modifier, size: Dp = 52.dp, imageSize: Dp = 40.dp, radius: Dp = 14.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(RoundedCornerShape(radius)).background(background),
    ) {
        FallbackAsyncImage(model = model, modifier = Modifier.size(imageSize))
    }
}

/** Section d'une fiche détail : libellé en capitales puis carte. */
@Composable
fun GiveDetailSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        PalCard(spacing = 6.dp, content = content)
    }
}

/** En-tête d'une fiche détail : grande vignette + titre + sous-titre. */
@Composable
fun GiveDetailHeader(model: String, background: Color, title: String, subtitle: String?, badges: (@Composable () -> Unit)? = null) {
    val c = PalTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GiveThumb(model, background, size = 76.dp, imageSize = 68.dp, radius = 18.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = PalTheme.type.screenTitle, color = c.text, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            badges?.invoke()
        }
    }
}

/** Couleurs Material des champs "outlined" restants (menus déroulants), alignées sur les tokens. */
@Composable
fun giveFieldColors(): TextFieldColors {
    val c = PalTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
        focusedContainerColor = c.field,
        unfocusedContainerColor = c.field,
        focusedBorderColor = c.green.solid,
        unfocusedBorderColor = c.borderStrong,
        focusedLabelColor = c.green.content,
        unfocusedLabelColor = c.textMuted,
        cursorColor = c.green.solid,
        focusedPlaceholderColor = c.textFaint,
        unfocusedPlaceholderColor = c.textFaint,
        focusedTrailingIconColor = c.textMuted,
        unfocusedTrailingIconColor = c.textMuted,
        focusedLeadingIconColor = c.textMuted,
        unfocusedLeadingIconColor = c.textMuted,
    )
}
