package com.paladmin.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.ui.theme.PalTheme
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import java.util.Locale

/** Case de grille (inventaire joueur, coffre de guilde...) : icône + badge de quantité, comme dans
 * le jeu. dimmed : pour lister aussi les entrées non débloquées (ex. technologies) à côté de
 * celles obtenues, sans les confondre visuellement. borderColor : état débloqué/non débloqué (vert/
 * rouge). tintBackground : catégorie spéciale (ex. technologie ancienne/boss, violet). */
data class InventoryGridItem(
    val imagePath: String,
    val label: String,
    val quantity: Int? = null,
    val dimmed: Boolean = false,
    val borderColor: Color? = null,
    val tintBackground: Color? = null,
)

/** Case cliquable pour identifier l'objet (nom + quantité exacte), les icônes seules ne suffisent
 * pas à tout reconnaître. Carré arrondi de la maquette (fond "raised", icône à 70 %, quantité en
 * monospace dans le coin). Sans [modifier], la case fait 56 dp ; dans une grille, passer une
 * largeur (ex. `Modifier.weight(1f)`) : la case reste carrée. */
@Composable
fun InventoryTile(item: InventoryGridItem, onClick: () -> Unit, modifier: Modifier = Modifier.size(56.dp)) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val description = item.quantity?.let { "${item.label} × $it" } ?: item.label
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .alpha(if (item.dimmed) 0.4f else 1f)
            .clip(shape)
            .background(item.tintBackground ?: c.surfaceRaised)
            .let { m -> item.borderColor?.let { m.border(1.5.dp, it, shape) } ?: m }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        FallbackAsyncImage(model = item.imagePath, modifier = Modifier.fillMaxSize(0.7f))
        item.quantity?.let { quantity ->
            Text(
                formatQuantity(quantity),
                style = PalTheme.type.monoSmall.copy(fontSize = 10.5.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 3.dp)
                    .background(c.background.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/** Image avec repli sur une icône générique si le fichier n'existe pas côté assets (id d'item/Pal
 * non résolu — variantes, ids obsolètes...) : mieux vaut un symbole visible qu'un vide silencieux.
 * tint : pour les icônes monochromes (ex. chevrons de rang de passif) à colorer dynamiquement. */
@Composable
fun FallbackAsyncImage(model: String, modifier: Modifier = Modifier, tint: Color? = null) {
    // Aperçus / captures Paparazzi : une seule frame est rendue, avant qu'un chargement Coil
    // asynchrone ait pu aboutir — les assets locaux sont alors décodés de façon synchrone.
    if (LocalInspectionMode.current && model.startsWith(ASSET_PREFIX)) {
        val context = LocalContext.current
        val bitmap = remember(model) {
            runCatching { context.assets.open(model.removePrefix(ASSET_PREFIX)).use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = null, colorFilter = tint?.let { ColorFilter.tint(it) }, modifier = modifier)
        } else {
            Box(modifier)
        }
        return
    }
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

private const val ASSET_PREFIX = "file:///android_asset/"

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
