package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme

/** Case d'un Pal (Palbox, camp de guilde...) : portrait, nom (surnom sinon espèce) et niveau, comme
 * la grille de la maquette "Fiche joueur". Cliquable pour ouvrir le détail complet (le portrait
 * seul ne dit rien de l'état/des compétences). Sans [modifier], la case fait 80 dp de large ; dans
 * une grille, passer `Modifier.weight(1f)`. */
@Composable
fun PalTile(pal: PalInfo, onClick: () -> Unit, modifier: Modifier = Modifier.width(80.dp)) {
    val c = PalTheme.colors
    val name = pal.nickname.ifBlank { pal.speciesName }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceRaised)
            .clickable(role = Role.Button, onClickLabel = name, onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
        ) {
            FallbackAsyncImage(model = pal.imagePath, modifier = Modifier.size(54.dp))
            Text(
                name,
                style = PalTheme.type.caption.copy(fontSize = 11.5.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.pal_level_fmt, pal.level),
                style = PalTheme.type.monoSmall.copy(fontSize = 10.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = c.textMuted,
                maxLines = 1,
            )
        }
        if (pal.shiny || pal.isBoss) {
            Row(modifier = Modifier.align(Alignment.TopStart).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (pal.shiny) FallbackAsyncImage("file:///android_asset/images/status/lucky.webp", Modifier.size(14.dp))
                if (pal.isBoss) FallbackAsyncImage("file:///android_asset/images/status/boss_alpha.webp", Modifier.size(14.dp))
            }
        }
    }
}
