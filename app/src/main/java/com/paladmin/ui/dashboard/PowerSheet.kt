package com.paladmin.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paladmin.R
import com.paladmin.ui.components.CardTitle
import com.paladmin.ui.components.ChoiceGrid
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.theme.PalTheme
import kotlinx.coroutines.launch

private val DELAYS = listOf(
    30 to R.string.power_delay_30s,
    60 to R.string.power_delay_1m,
    300 to R.string.power_delay_5m,
    600 to R.string.power_delay_10m,
)

/**
 * Feuille "Alimentation" : arrêt programmé (le "redémarrage" passe par le gestionnaire de process)
 * et arrêt forcé protégé par un appui long, pour qu'un simple tap accidentel ne coupe pas le serveur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerSheet(
    serverName: String,
    onlinePlayers: Int,
    onSchedule: (seconds: Int, message: String) -> Unit,
    onForceStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PalTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceBar,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp).background(c.neutral.solid, RoundedCornerShape(4.dp)))
        },
    ) {
        PowerSheetContent(serverName, onlinePlayers, onSchedule, onForceStop)
    }
}

@Composable
fun PowerSheetContent(
    serverName: String,
    onlinePlayers: Int,
    onSchedule: (seconds: Int, message: String) -> Unit,
    onForceStop: () -> Unit,
) {
    val c = PalTheme.colors
    val defaultMessage = stringResource(R.string.dashboard_default_restart_message)
    var delay by rememberSaveable { mutableIntStateOf(60) }
    var message by rememberSaveable { mutableStateOf(defaultMessage) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.PowerSettingsNew, c.amber, size = 44.dp, iconSize = 22.dp, radius = 14.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.dashboard_power), style = PalTheme.type.screenTitle.copy(fontSize = PalTheme.type.screenTitle.fontSize * 1.05f), color = c.text)
                Text(stringResource(R.string.power_subtitle_fmt, serverName, onlinePlayers), style = PalTheme.type.bodySmall, color = c.textMuted)
            }
        }

        PalCard(background = c.surfaceRaised.copy(alpha = if (c.isDark) 0.75f else 1f), contentPadding = PaddingValues(16.dp), spacing = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardTitle(stringResource(R.string.power_restart_title))
                Text(stringResource(R.string.power_restart_explanation), style = PalTheme.type.bodySmall, color = c.textBody)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.power_delay_label), style = PalTheme.type.bodySmall, color = c.textMuted)
                ChoiceGrid(
                    options = DELAYS.map { (seconds, label) -> seconds to stringResource(label) },
                    selected = delay,
                    onSelect = { delay = it },
                    tone = c.amber,
                )
            }
            PalTextField(value = message, onValueChange = { message = it }, label = stringResource(R.string.power_message_label))
            val delayLabel = stringResource(DELAYS.first { it.first == delay }.second)
            PalButton(
                text = stringResource(R.string.power_schedule_fmt, delayLabel),
                onClick = { onSchedule(delay, message) },
                tone = c.amber,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PalCard(
            background = c.danger.container.copy(alpha = if (c.isDark) 0.45f else 0.6f),
            border = c.danger.border,
            contentPadding = PaddingValues(16.dp),
            spacing = 10.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = c.danger.onContainer, modifier = Modifier.size(18.dp))
                CardTitle(stringResource(R.string.power_force_title), color = c.danger.onContainer)
            }
            Text(stringResource(R.string.power_force_message), style = PalTheme.type.bodySmall, color = c.danger.onContainer.copy(alpha = 0.85f))
            HoldToConfirmButton(text = stringResource(R.string.power_hold_to_stop), holdingText = stringResource(R.string.power_holding), onConfirmed = onForceStop)
        }
    }
}

/** Bouton qu'il faut maintenir ~1,5 s : la jauge se remplit, relâcher avant annule. */
@Composable
private fun HoldToConfirmButton(text: String, holdingText: String, onConfirmed: () -> Unit) {
    val c = PalTheme.colors
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var holding by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(1.dp, c.danger.solid, shape)
            .semantics {
                role = Role.Button
                // Les lecteurs d'écran ne peuvent pas "maintenir" : action dédiée à la place.
                customActions = listOf(CustomAccessibilityAction(text) { onConfirmed(); true })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    holding = true
                    val job = scope.launch {
                        progress.animateTo(1f, tween(durationMillis = 1500, easing = LinearEasing))
                        onConfirmed()
                    }
                    waitForUpOrCancellation()
                    holding = false
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(200)) }
                    }
                }
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(end = 0.dp),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(c.danger.solid.copy(alpha = 0.35f)))
        }
        Text(
            if (holding) holdingText else text,
            style = PalTheme.type.button.copy(fontSize = PalTheme.type.button.fontSize * 0.97f),
            color = c.danger.onContainer,
            textAlign = TextAlign.Center,
        )
    }
}
