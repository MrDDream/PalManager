package com.paladmin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

// Briques visuelles de la refonte UI/UX (maquette Claude Design "PalManager — refonte UI/UX").
// Les écrans s'assemblent à partir de ces composants plutôt que des Card/TopAppBar Material bruts,
// pour garder partout les mêmes rayons, contrastes et hauteurs tactiles (≥ 44 dp).

enum class ToneKey { Green, Sky, Amber, Danger, Purple, Neutral }

@Composable
fun tone(key: ToneKey): Tone = with(PalTheme.colors) {
    when (key) {
        ToneKey.Green -> green
        ToneKey.Sky -> sky
        ToneKey.Amber -> amber
        ToneKey.Danger -> danger
        ToneKey.Purple -> purple
        ToneKey.Neutral -> neutral
    }
}

/** Barre du haut : retour (ou fermer), titre Sora, actions à droite. Gère l'encoche (status bar). */
@Composable
fun PalTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    closeIcon: Boolean = false,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(c.background)
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = if (onBack != null) 8.dp else 16.dp, end = 8.dp),
    ) {
        if (onBack != null) {
            PalIconButton(
                icon = if (closeIcon) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
                tint = c.text,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = PalTheme.type.screenTitle,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = PalTheme.type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

/** Bouton icône carré 44 dp, sans fond, coins 12 dp. */
@Composable
fun PalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PalTheme.colors.textSecondary,
    enabled: Boolean = true,
    iconSize: Dp = 22.dp,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(44.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) tint else tint.copy(alpha = 0.4f), modifier = Modifier.size(iconSize))
    }
}

/** Carte de section (fond surface, coins 20 dp). [border] facultatif pour mettre une carte en avant. */
@Composable
fun PalCard(
    modifier: Modifier = Modifier,
    border: Color? = null,
    background: Color = PalTheme.colors.surface,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    spacing: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Libellé de section en capitales ("DONNER", "GÉRER"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = PalTheme.colors.textMuted) {
    Text(
        text.uppercase(),
        style = PalTheme.type.overline,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

/** Titre de section à l'intérieur d'une carte. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier, color: Color = PalTheme.colors.text) {
    Text(text, style = PalTheme.type.sectionTitle, color = color, modifier = modifier.semantics { heading() })
}

/** Petite étiquette monospace teintée ("SFTP", "Steam", "7/32"). */
@Composable
fun MonoBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = PalTheme.colors.neutral,
    pill: Boolean = true,
    style: TextStyle = PalTheme.type.monoSmall,
) {
    Text(
        text,
        style = style,
        color = tone.onContainer,
        maxLines = 1,
        modifier = modifier
            .background(tone.container, if (pill) CircleShape else RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Pastille d'état "En ligne" / "Hors ligne" avec point coloré. */
@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier, dot: Color = tone.content) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(tone.container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        Text(text, style = PalTheme.type.label.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = tone.onContainer, maxLines = 1)
    }
}

enum class PalButtonStyle {
    /** Fond plein de la couleur d'accent. */
    Filled,
    /** Fond teinté discret, texte accentué. */
    Tonal,
    /** Contour de la couleur d'accent, fond transparent. */
    Outlined,
    /** Bouton secondaire neutre (fond "raised" + bordure). */
    Neutral,
}

/** Bouton de la maquette : hauteur ≥ 44 dp (48-52 pour les actions principales), coins 14 dp. */
@Composable
fun PalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = PalTheme.colors.green,
    style: PalButtonStyle = PalButtonStyle.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    textStyle: TextStyle = PalTheme.type.button,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = PalTheme.colors
    val (bg, fg, border) = when (style) {
        PalButtonStyle.Filled -> Triple(tone.solid, tone.onSolid, null)
        PalButtonStyle.Tonal -> Triple(tone.container, tone.onContainer, null)
        PalButtonStyle.Outlined -> Triple(Color.Transparent, tone.onContainer, tone.border)
        PalButtonStyle.Neutral -> Triple(c.surfaceRaised, c.text, c.borderStrong)
    }
    val alpha = if (enabled) 1f else 0.45f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = height)
            .clip(shape)
            .background(bg.copy(alpha = bg.alpha * alpha))
            .then(if (border != null) Modifier.border(1.dp, border.copy(alpha = alpha), shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg.copy(alpha = alpha)) {
            if (leading != null) leading()
            else if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = textStyle, color = fg.copy(alpha = alpha), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Chip de filtre / tri en pilule (36 dp). */
@Composable
fun PalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = PalTheme.colors.green,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(if (selected) tone.container else Color.Transparent)
            .border(1.dp, if (selected) tone.solid else c.borderStrong, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        if (leading != null) leading()
        Text(label, style = PalTheme.type.label, color = if (selected) tone.onContainer else c.textSecondary, maxLines = 1)
    }
}

/** Choix unique en grille (délais, thèmes, quantités) : chaque option est une case de 44 dp. */
@Composable
fun <T> ChoiceGrid(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = PalTheme.colors.green,
    textStyle: TextStyle = PalTheme.type.label.copy(fontSize = 14.sp),
) {
    val c = PalTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) tone.container else c.field)
                    .border(1.dp, if (on) tone.solid else c.borderStrong, RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    style = textStyle.copy(fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.SemiBold else textStyle.fontWeight),
                    color = if (on) tone.onContainer else c.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Sélecteur segmenté (Langue, onglets courts) : rail sombre, segment actif "raised". */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PalTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.field, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.divider else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    label,
                    style = PalTheme.type.label.copy(fontSize = 14.sp, fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium),
                    color = if (on) c.text else c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Barre de recherche 48 dp, coins 14 dp, loupe à gauche, croix pour effacer. */
@Composable
fun PalSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = PalTheme.type.body, color = c.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = PalTheme.type.body.copy(color = c.text),
                cursorBrush = SolidColor(c.green.solid),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            PalIconButton(Icons.Filled.Close, stringResource(R.string.cd_clear), onClick = { onValueChange("") }, iconSize = 18.dp)
        } else if (trailing == null) {
            Spacer(Modifier.width(10.dp))
        }
        trailing?.invoke()
    }
}

/**
 * Champ de formulaire de la maquette : libellé au-dessus, boîte 48 dp à fond sombre, coins 12 dp.
 * [mono] pour les hôtes, ports, chemins. [password] ajoute le bouton afficher/masquer.
 */
@Composable
fun PalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    mono: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = PalTheme.colors
    var revealed by remember { mutableStateOf(false) }
    val textStyle = (if (mono) PalTheme.type.mono.copy(fontSize = 14.sp) else PalTheme.type.body).copy(color = if (enabled) c.text else c.textMuted)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        if (label != null) Text(label, style = PalTheme.type.bodySmall, color = c.textSecondary)
        Row(
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.field)
                .border(1.dp, if (isError) c.danger.content else c.borderStrong, RoundedCornerShape(12.dp)),
        ) {
            Box(
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 13.dp),
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = textStyle, color = c.textFaint, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(c.green.solid),
                    visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (password) {
                PalIconButton(
                    icon = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (revealed) R.string.cd_hide_password else R.string.cd_show_password),
                    onClick = { revealed = !revealed },
                    tint = c.textMuted,
                    iconSize = 18.dp,
                )
            }
            trailing?.invoke()
        }
        if (supportingText != null) {
            Text(supportingText, style = PalTheme.type.caption, color = if (isError) c.danger.content else c.textMuted)
        }
    }
}

/** Interrupteur aux couleurs de la maquette. */
@Composable
fun PalSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = PalTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.green.onSolid,
            checkedTrackColor = c.green.solid,
            checkedBorderColor = c.green.solid,
            uncheckedThumbColor = c.textSecondary,
            uncheckedTrackColor = c.neutral.solid,
            uncheckedBorderColor = c.neutral.solid,
        ),
    )
}

/** Pastille ronde avec initiale (joueurs, membres de guilde). Couleur stable dérivée de [seed]. */
@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 42.dp, seed: String = name, ring: Color? = null) {
    val palette = PalTheme.colors.avatars
    val (bg, ink) = palette[Math.floorMod(seed.hashCode(), palette.size)]
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .background(bg, CircleShape),
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = PalTheme.type.cardTitle.copy(fontSize = (size.value * 0.38f).sp),
            color = ink,
        )
    }
}

/** Carré arrondi teinté contenant une icône (raccourcis du dashboard, en-têtes de feuille). */
@Composable
fun IconTile(
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 21.dp,
    radius: Dp = 12.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(tone.container, RoundedCornerShape(radius)),
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(iconSize))
    }
}

/** Tuile de statistique teintée : libellé, grand chiffre (avec unité plus petite), contenu optionnel. */
@Composable
fun StatTile(
    label: String,
    value: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    unit: String? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(tone.container, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(label, style = PalTheme.type.caption, color = tone.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = PalTheme.type.stat, color = PalTheme.colors.text, maxLines = 1)
            if (unit != null) {
                Text(unit, style = PalTheme.type.stat.copy(fontSize = 15.sp), color = tone.onContainer, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        footer?.invoke()
    }
}

/** Barre de progression fine (4 dp) aux couleurs d'un [Tone]. */
@Composable
fun ThinProgress(fraction: Float, tone: Tone, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(tone.border, RoundedCornerShape(height)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(tone.solid, RoundedCornerShape(height)),
        )
    }
}

/** Barre fixe en bas d'écran (actions principales), au-dessus de la barre de navigation système. */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PalTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(c.surfaceBar)
            .drawBehind { drawLine(c.divider, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        content = content,
    )
}

/** Ligne cliquable d'une liste dans une carte (54 dp, chevron/valeur à droite). */
@Composable
fun PalListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = PalTheme.colors.text,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PalTheme.type.body, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = PalTheme.type.bodySmall, color = PalTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

/** Fine séparation horizontale entre lignes d'une carte. */
@Composable
fun PalDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(PalTheme.colors.divider))
}

/** Bouton/zone en pointillés ("Ajouter un serveur", "Choisir un dossier"). */
@Composable
fun DashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 64.dp,
    radius: Dp = 22.dp,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, if (content == null) Alignment.CenterHorizontally else Alignment.Start),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(RoundedCornerShape(radius))
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = c.neutral.solid,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (content != null) {
            content()
        } else {
            if (icon != null) Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            Text(text, style = PalTheme.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.textSecondary)
        }
    }
}

/** Encart d'information discret (icône + texte), fond "bar". */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tone: Tone? = null) {
    val c = PalTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(tone?.container ?: c.surfaceBar, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tone?.content ?: c.sky.content, modifier = Modifier.size(20.dp))
        Text(text, style = PalTheme.type.bodySmall, color = tone?.onContainer ?: c.textSecondary)
    }
}

/** Écran vide / erreur centré : icône dans un cercle teinté, titre, message, action optionnelle. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    tone: Tone = PalTheme.colors.neutral,
    action: (@Composable () -> Unit)? = null,
) {
    val c = PalTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(32.dp),
    ) {
        if (icon != null) IconTile(icon, tone, size = 56.dp, iconSize = 28.dp, radius = 18.dp)
        Text(title, style = PalTheme.type.cardTitle, color = c.text, textAlign = TextAlign.Center)
        if (message != null) Text(message, style = PalTheme.type.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** Ligne clé → valeur (fiches de détail). */
@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false, valueColor: Color = PalTheme.colors.text) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp),
    ) {
        Text(key, style = PalTheme.type.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
        Text(value, style = if (mono) PalTheme.type.mono.copy(fontSize = 13.sp) else PalTheme.type.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = valueColor, textAlign = TextAlign.End)
    }
}
