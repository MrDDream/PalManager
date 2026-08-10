package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import java.util.Locale

/** Case de grille (inventaire joueur, coffre de guilde...) : icône + badge de quantité, comme dans le jeu. */
data class InventoryGridItem(val imagePath: String, val label: String, val quantity: Int? = null)

/** Case cliquable pour identifier l'objet (nom + quantité exacte), les icônes seules ne suffisent pas à tout reconnaître. */
@Composable
fun InventoryTile(item: InventoryGridItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        FallbackAsyncImage(model = item.imagePath, modifier = Modifier.fillMaxSize().padding(4.dp))
        item.quantity?.let { quantity ->
            Text(
                "×${formatQuantity(quantity)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

/** Image avec repli sur une icône générique si le fichier n'existe pas côté assets (id d'item/Pal
 * non résolu — variantes, ids obsolètes...) : mieux vaut un symbole visible qu'un vide silencieux.
 * tint : pour les icônes monochromes (ex. chevrons de rang de passif) à colorer dynamiquement. */
@Composable
fun FallbackAsyncImage(model: String, modifier: Modifier = Modifier, tint: Color? = null) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Error -> Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> SubcomposeAsyncImageContent()
        }
    }
}

/** Abrège les grosses quantités (ex. pièces d'or) façon jeu : 3 346 473 -> "3.3M". */
fun formatQuantity(value: Int): String {
    val v = value.toDouble()
    return when {
        v >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.1fB", v / 1_000_000_000)
        v >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", v / 1_000_000)
        v > 9_999 -> String.format(Locale.getDefault(), "%.1fK", v / 1_000)
        else -> value.toString()
    }
}
