package com.paladmin.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.SearchField

/** Ambre fixe (pas de rôle "warning" dans Material3) — lisible sur fond clair et sombre. */
private val WarningColor = Color(0xFFC77F00)

private fun logSourceIcon(source: LogSource): ImageVector = when (source) {
    LogSource.PALDEFENDER -> Icons.Filled.Shield
    LogSource.UE4SS -> Icons.Filled.Extension
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val states by viewModel.states.collectAsState()
    val state = states[selectedTab] ?: LogsUiState()
    var query by remember(selectedTab) { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (profile?.isSftpConfigured == true) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            profile == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            profile?.isSftpConfigured == false -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        stringResource(R.string.logs_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onOpenEditProfile, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.logs_open_profile_settings))
                    }
                }
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    LogSource.entries.forEach { source ->
                        FilterChip(
                            selected = selectedTab == source,
                            onClick = { viewModel.selectTab(source) },
                            label = { Text(stringResource(source.labelRes)) },
                            leadingIcon = { Icon(logSourceIcon(source), contentDescription = null) },
                        )
                    }
                }

                if (!state.isLoading && state.error == null && state.hostKeyMismatch == null && state.lines.isNotEmpty()) {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.logs_search_hint),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                LogTabContent(
                    state = state,
                    query = query,
                    onRetry = viewModel::refresh,
                    onTrustNewKey = { viewModel.trustNewHostKey(selectedTab) },
                )
            }
        }
    }
}

@Composable
private fun LogTabContent(state: LogsUiState, query: String, onRetry: () -> Unit, onTrustNewKey: () -> Unit) {
    when {
        state.hostKeyMismatch != null -> {
            val (expected, actual) = state.hostKeyMismatch
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    stringResource(R.string.logs_host_key_mismatch),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.logs_host_key_fmt, expected, actual),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onTrustNewKey, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.logs_trust_new_key))
                }
            }
        }
        state.error != null -> Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            Text(state.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.common_retry))
            }
        }
        state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> {
            val visibleLines = remember(state.lines, query) {
                if (query.isBlank()) state.lines else state.lines.filter { it.contains(query, ignoreCase = true) }
            }
            val listState = rememberLazyListState()
            // Les entrées les plus récentes sont en fin de fichier — on ouvre directement dessus
            // plutôt qu'en haut (le début de la fenêtre tronquée à 512 Ko, souvent peu utile).
            LaunchedEffect(state.lines) {
                if (state.lines.isNotEmpty()) listState.scrollToItem(state.lines.lastIndex)
            }
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.truncated) {
                    Text(
                        stringResource(R.string.logs_truncated_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                if (query.isNotBlank() && visibleLines.isNotEmpty()) {
                    Text(
                        stringResource(R.string.logs_search_results_fmt, visibleLines.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                if (visibleLines.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(if (query.isBlank()) R.string.logs_empty else R.string.logs_search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    ) {
                        items(visibleLines) { line -> LogLine(line) }
                    }
                }
            }
        }
    }
}

/** Repère l'horodatage en tête de ligne commun aux trois outils (`[2026-08-06 10:02:26.518...]` en
 * UE4SS, `[12:02:14]` en PalDefender) pour l'afficher en plus petit/atténué — le reste de la ligne
 * (message, éventuel second tag `[error]`/`[warning]`...) garde la lisibilité normale à la place. */
private val TIMESTAMP_PREFIX = Regex("""^\[([^]]+)]\s*(.*)$""")

/** UE4SS logge avec date + jusqu'à 7 décimales ("2026-08-06 10:02:26.5180697"), illisible sur un
 * écran étroit — on ne garde que l'heure avec 3 décimales ("10:02:26.518"). Déjà court (PalDefender,
 * "12:02:14") : inchangé. */
private fun shortenTimestamp(raw: String): String {
    val timePart = raw.substringAfterLast(' ')
    val dotIndex = timePart.indexOf('.')
    return if (dotIndex in 0..<timePart.length - 4) timePart.take(dotIndex + 4) else timePart
}

@Composable
private fun LogLine(line: String) {
    val match = remember(line) { TIMESTAMP_PREFIX.find(line) }
    val messageColor = logLineColor(line)
    val weight = if (isSevereLogLine(line)) FontWeight.SemiBold else FontWeight.Normal
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    val text = if (match != null) {
        val (timestamp, message) = match.destructured
        buildAnnotatedString {
            withStyle(SpanStyle(color = dimColor, fontSize = 11.sp)) { append(shortenTimestamp(timestamp)) }
            append("  ")
            withStyle(SpanStyle(color = messageColor, fontWeight = weight)) { append(message) }
        }
    } else {
        buildAnnotatedString { withStyle(SpanStyle(color = messageColor, fontWeight = weight)) { append(line) } }
    }

    Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
}

/** Détection par mots-clés, pas de parseur dédié par outil (Palworld/PalDefender/UE4SS n'ont pas
 * le même format) — suffisant pour repérer erreurs/avertissements d'un coup d'œil dans un flux de
 * log brut, les trois utilisent tous des tags "error"/"warning" en anglais sous une forme ou une
 * autre (`[error]`, `LogTemp: Warning:`, `Exception`...). */
private fun isSevereLogLine(line: String): Boolean {
    val lower = line.lowercase()
    return "error" in lower || "fatal" in lower || "exception" in lower
}

@Composable
private fun logLineColor(line: String): Color {
    val lower = line.lowercase()
    return when {
        "error" in lower || "fatal" in lower || "exception" in lower -> MaterialTheme.colorScheme.error
        "warn" in lower -> WarningColor
        "debug" in lower || "verbose" in lower -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
}
