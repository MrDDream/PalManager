package com.paladmin.screenshots

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Charge une image des assets de façon synchrone (BitmapFactory) au lieu de Coil : dans un test
 * Paparazzi la capture se fait après une seule frame, avant qu'un chargement async ait pu terminer.
 */
@Composable
internal fun AssetImage(context: Context, path: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val bitmap = decodeAsset(context, path)
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier = modifier)
    }
}

private fun decodeAsset(context: Context, path: String) = runCatching {
    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
}.getOrNull()
