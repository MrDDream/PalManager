package com.paladmin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Champ de recherche commun aux écrans de liste — délègue à [PalSearchBar] (barre 48 dp de la
 * maquette, loupe + croix d'effacement). [label] sert de texte indicatif.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    PalSearchBar(value = value, onValueChange = onValueChange, placeholder = label, modifier = modifier, trailing = trailing)
}
