package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paladmin.ui.theme.PalTheme

/** Icône dans un carré arrondi teinté (style [IconTile] de la refonte) — utilisé comme leading
 * content sur les listes (profils, bans, guildes...). [tint] : couleur de l'icône, le fond en est
 * une version très atténuée ; par défaut l'accent vert du thème. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = PalTheme.colors.green.content, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}
