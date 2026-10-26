package com.paladmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme

// Briques partagées par les outils SFTP (Logs, Fichiers, Créateur de Pal) : mêmes états
// "non configuré", "clé d'hôte modifiée", "erreur" et "chargement" sur les trois écrans.

/** Chargement plein écran. */
@Composable
fun ToolsLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PalTheme.colors.green.solid)
    }
}

/** SFTP absent du profil : explication + lien vers l'édition du profil. */
@Composable
fun ToolsNotConfiguredState(onOpenEditProfile: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            title = stringResource(R.string.tools_not_configured_title),
            message = stringResource(R.string.logs_not_configured),
            icon = Icons.Filled.CloudOff,
            tone = PalTheme.colors.sky,
            action = {
                PalButton(
                    text = stringResource(R.string.logs_open_profile_settings),
                    onClick = onOpenEditProfile,
                    icon = Icons.Filled.Settings,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
        )
    }
}

/** Erreur de connexion / lecture avec bouton Réessayer. */
@Composable
fun ToolsErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            title = stringResource(R.string.tools_error_title),
            message = message,
            icon = Icons.Filled.ErrorOutline,
            tone = PalTheme.colors.danger,
            action = {
                PalButton(
                    text = stringResource(R.string.common_retry),
                    onClick = onRetry,
                    icon = Icons.Filled.Refresh,
                    style = PalButtonStyle.Neutral,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
        )
    }
}

/** Clé d'hôte SSH différente de l'empreinte épinglée : on montre les deux, l'utilisateur décide. */
@Composable
fun ToolsHostKeyMismatchState(expected: String, actual: String, onTrustNewKey: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        IconTile(Icons.Filled.Warning, c.danger, size = 56.dp, iconSize = 28.dp, radius = 18.dp)
        Text(stringResource(R.string.tools_host_key_title), style = PalTheme.type.cardTitle, color = c.text, textAlign = TextAlign.Center)
        Text(stringResource(R.string.logs_host_key_mismatch), style = PalTheme.type.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
        PalCard(border = c.danger.border, modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
            FingerprintLine(stringResource(R.string.tools_host_key_expected), expected, c.textSecondary)
            PalDivider()
            FingerprintLine(stringResource(R.string.tools_host_key_actual), actual, c.danger.content)
        }
        PalButton(
            text = stringResource(R.string.logs_trust_new_key),
            onClick = onTrustNewKey,
            tone = c.danger,
            style = PalButtonStyle.Tonal,
            icon = Icons.Filled.VerifiedUser,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FingerprintLine(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = PalTheme.type.overline.copy(fontSize = 11.sp), color = PalTheme.colors.textMuted)
        Text(value, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = color)
    }
}

/**
 * Sélecteur segmenté sur rail "surface" (sources de log, sexe du Pal) — variante de
 * [SegmentedControl] dont le rail reste visible sur le fond d'écran.
 * [allowDeselect] : un appui sur le segment actif le désélectionne ([onSelect] reçoit null).
 */
@Composable
fun <T> ToolsSegmented(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allowDeselect: Boolean = false,
    groupDescription: String? = null,
) {
    val c = PalTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .then(if (groupDescription != null) Modifier.semantics { contentDescription = groupDescription } else Modifier)
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.divider else Color.Transparent)
                    .clickable(role = if (allowDeselect) Role.Checkbox else Role.Tab) {
                        onSelect(if (on && allowDeselect) null else value)
                    }
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    label,
                    style = PalTheme.type.label.copy(fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (on) c.text else c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
