package com.paladmin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme

/**
 * Bouton "Filtres" repliable : les chips de filtre restent disponibles sans occuper
 * en permanence l'écran (repliés par défaut, chip accentuée avec compteur quand des filtres sont actifs).
 */
@Composable
fun FilterPanel(
    activeCount: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val c = PalTheme.colors

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        PalChip(
            label = if (activeCount > 0) stringResource(R.string.filter_button_count_fmt, activeCount) else stringResource(R.string.filter_button),
            selected = activeCount > 0,
            onClick = { expanded = !expanded },
            leading = {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = if (activeCount > 0) c.green.onContainer else c.textSecondary, modifier = Modifier.size(18.dp))
            },
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}
