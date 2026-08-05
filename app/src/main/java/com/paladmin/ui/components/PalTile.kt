package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Case d'un Pal (équipe joueur, camp de guilde...) : portrait + niveau en badge, comme dans le jeu.
 * Cliquable pour ouvrir le détail complet (le portrait seul ne dit rien de l'état/des compétences). */
@Composable
fun PalTile(pal: PalInfo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        FallbackAsyncImage(model = pal.imagePath, modifier = Modifier.fillMaxSize().padding(4.dp))
        if (pal.shiny) {
            Text(
                "✨",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
            )
        }
        Text(
            "Nv.${pal.level}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
