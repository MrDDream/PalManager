package com.paladmin.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.paladmin.R
import com.paladmin.data.remote.palworld.PalworldPlayer

/**
 * Filtre rapide par pseudo (nom du personnage en jeu) au lieu d'une liste déroulante statique de
 * tous les joueurs connectés — on tape le début d'un nom, les résultats correspondants
 * apparaissent, on tape pour sélectionner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSearchField(
    players: List<PalworldPlayer>,
    selectedPlayer: PalworldPlayer?,
    onPlayerSelected: (PalworldPlayer) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.common_player_label),
) {
    var query by remember(selectedPlayer) { mutableStateOf(selectedPlayer?.name ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val results = remember(players, query) {
        if (query.isBlank()) players else players.filter { it.name.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && results.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.common_player_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && results.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            results.forEach { player ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_player_level_fmt, player.name, player.level)) },
                    onClick = {
                        query = player.name
                        expanded = false
                        onPlayerSelected(player)
                    },
                )
            }
        }
    }
}
