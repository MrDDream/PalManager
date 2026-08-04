package com.paladmin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paladmin.R

/**
 * Bouton "Filtres" repliable : les chips de filtre restent disponibles sans occuper
 * en permanence l'écran (repliés par défaut, avec un badge quand des filtres sont actifs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    activeCount: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        AssistChip(
            onClick = { expanded = !expanded },
            label = {
                Text(
                    if (activeCount > 0) {
                        stringResource(R.string.filter_button_count_fmt, activeCount)
                    } else {
                        stringResource(R.string.filter_button)
                    },
                )
            },
            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            },
            colors = if (activeCount > 0) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    trailingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                AssistChipDefaults.assistChipColors()
            },
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}
