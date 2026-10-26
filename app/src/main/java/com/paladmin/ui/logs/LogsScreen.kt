package com.paladmin.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.components.EmptyState
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.ToolsErrorState
import com.paladmin.ui.components.ToolsHostKeyMismatchState
import com.paladmin.ui.components.ToolsLoadingState
import com.paladmin.ui.components.ToolsNotConfiguredState
import com.paladmin.ui.components.ToolsSegmented
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

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
    var level by remember(selectedTab) { mutableStateOf<LogLevel?>(null) }

    LogsContent(
        profileLoaded = profile != null,
        sftpConfigured = profile?.isSftpConfigured == true,
        source = selectedTab,
        state = state,
        query = query,
        level = level,
        actions = LogsActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onOpenEditProfile = onOpenEditProfile,
            onSelectSource = viewModel::selectTab,
            onTrustNewKey = { viewModel.trustNewHostKey(selectedTab) },
            onQueryChange = { query = it },
            onLevelChange = { level = it },
        ),
    )
}

/** Callbacks de [LogsContent]. */
data class LogsActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenEditProfile: () -> Unit = {},
    val onSelectSource: (LogSource) -> Unit = {},
    val onTrustNewKey: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    /** null = toutes les sévérités. */
    val onLevelChange: (LogLevel?) -> Unit = {},
)

/** Sévérité déduite d'une ligne de log (voir [parseLogLine]). [DEBUG] est compté avec [INFO] dans les filtres. */
enum class LogLevel(val tag: String) { INFO("INFO"), WARN("WARN"), ERROR("ERROR"), DEBUG("DEBUG") }

data class LogEntry(val time: String?, val level: LogLevel, val message: String)

@Composable
fun LogsContent(
    profileLoaded: Boolean,
    sftpConfigured: Boolean,
    source: LogSource,
    state: LogsUiState,
    query: String,
    level: LogLevel?,
    actions: LogsActions,
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(R.string.logs_title),
                onBack = actions.onBack,
                actions = {
                    if (sftpConfigured) {
                        PalIconButton(
                            Icons.Filled.Refresh,
                            stringResource(R.string.cd_refresh),
                            onClick = actions.onRefresh,
                            enabled = !state.isLoading,
                            tint = c.text,
                        )
                    }
                },
            )
        },
    ) { padding ->
        val inner = Modifier.fillMaxSize().padding(padding)
        when {
            !profileLoaded -> ToolsLoadingState(inner)
            !sftpConfigured -> ToolsNotConfiguredState(actions.onOpenEditProfile, inner)
            else -> Column(inner) {
                val entries = remember(state.lines) { state.lines.filter { it.isNotBlank() }.map(::parseLogLine) }
                LogsToolbar(source, entries, query, level, showSearch = state.error == null && state.hostKeyMismatch == null && entries.isNotEmpty(), actions)
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        state.hostKeyMismatch != null -> {
                            val (expected, actual) = state.hostKeyMismatch
                            ToolsHostKeyMismatchState(expected, actual, actions.onTrustNewKey)
                        }
                        state.error != null -> ToolsErrorState(state.error, actions.onRefresh)
                        state.isLoading -> ToolsLoadingState()
                        else -> LogLines(entries, state.truncated, query, level)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsToolbar(
    source: LogSource,
    entries: List<LogEntry>,
    query: String,
    level: LogLevel?,
    showSearch: Boolean,
    actions: LogsActions,
) {
    val c = PalTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
    ) {
        ToolsSegmented(
            options = LogSource.entries.map { it to stringResource(it.labelRes) },
            selected = source,
            onSelect = { it?.let(actions.onSelectSource) },
            groupDescription = stringResource(R.string.tools_logs_source_cd),
        )
        if (showSearch) {
            PalSearchBar(value = query, onValueChange = actions.onQueryChange, placeholder = stringResource(R.string.logs_search_hint))
            val counts = remember(entries) {
                entries.groupingBy { if (it.level == LogLevel.DEBUG) LogLevel.INFO else it.level }.eachCount()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                LevelChip(stringResource(R.string.tools_logs_level_all), entries.size, level == null, c.text, c.surfaceRaised) { actions.onLevelChange(null) }
                listOf(
                    LogLevel.INFO to R.string.tools_logs_level_info,
                    LogLevel.WARN to R.string.tools_logs_level_warn,
                    LogLevel.ERROR to R.string.tools_logs_level_error,
                ).forEach { (lvl, label) ->
                    val tone = levelTone(lvl)
                    LevelChip(stringResource(label), counts[lvl] ?: 0, level == lvl, tone.onContainer, c.surfaceRaised) {
                        actions.onLevelChange(if (level == lvl) null else lvl)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(label: String, count: Int, selected: Boolean, color: Color, selectedBackground: Color, onClick: () -> Unit) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp)
            .clip(CircleShape)
            .background(if (selected) selectedBackground else Color.Transparent)
            .border(1.dp, if (selected) color else c.borderStrong, CircleShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(label, style = PalTheme.type.label.copy(fontSize = 12.5.sp), color = color, maxLines = 1)
        Text(count.toString(), style = PalTheme.type.monoSmall, color = color.copy(alpha = 0.8f), maxLines = 1)
    }
}

@Composable
private fun levelTone(level: LogLevel): Tone = when (level) {
    LogLevel.INFO, LogLevel.DEBUG -> PalTheme.colors.sky
    LogLevel.WARN -> PalTheme.colors.amber
    LogLevel.ERROR -> PalTheme.colors.danger
}

@Composable
private fun LogLines(entries: List<LogEntry>, truncated: Boolean, query: String, level: LogLevel?) {
    val c = PalTheme.colors
    val visible = remember(entries, query, level) {
        entries.filter { entry ->
            val levelOk = when (level) {
                null -> true
                LogLevel.INFO -> entry.level == LogLevel.INFO || entry.level == LogLevel.DEBUG
                else -> entry.level == level
            }
            levelOk && (query.isBlank() || entry.message.contains(query, ignoreCase = true) || entry.time?.contains(query) == true)
        }
    }
    val listState = rememberLazyListState()
    // Les entrées les plus récentes sont en fin de fichier — on ouvre directement dessus
    // plutôt qu'en haut (le début de la fenêtre tronquée à 512 Ko, souvent peu utile).
    val lastIndex = (if (truncated) 1 else 0) + (if (query.isNotBlank()) 1 else 0) + visible.size - 1
    LaunchedEffect(entries) {
        if (lastIndex >= 0) listState.scrollToItem(lastIndex)
    }
    when {
        entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(stringResource(R.string.logs_empty), icon = Icons.Filled.Description)
        }
        visible.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                stringResource(if (query.isNotBlank()) R.string.logs_search_no_results else R.string.tools_logs_level_empty),
                icon = Icons.Filled.SearchOff,
            )
        }
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            if (truncated) {
                item(key = "truncated") {
                    Text(
                        stringResource(R.string.logs_truncated_notice),
                        style = PalTheme.type.caption,
                        color = c.textBody,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                            .fillMaxWidth()
                            .background(c.surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (query.isNotBlank()) {
                item(key = "results") {
                    Text(
                        stringResource(R.string.logs_search_results_fmt, visible.size),
                        style = PalTheme.type.caption,
                        color = c.textMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            items(visible) { entry -> LogRow(entry) }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val c = PalTheme.colors
    val tone = levelTone(entry.level)
    val severe = entry.level == LogLevel.ERROR
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            entry.time.orEmpty(),
            style = PalTheme.type.mono.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = c.textFaint,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 62.dp).padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                entry.level.tag,
                style = PalTheme.type.mono.copy(fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
                color = tone.onContainer,
                modifier = Modifier
                    .background(tone.container, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
            Text(
                entry.message,
                style = PalTheme.type.mono.copy(fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = if (severe) FontWeight.Medium else FontWeight.Normal),
                color = if (entry.level == LogLevel.DEBUG) c.textMuted else c.textSecondary,
            )
        }
    }
}

/** Repère l'horodatage en tête de ligne commun aux outils (`[2026-08-06 10:02:26.518...]` en
 * UE4SS, `[12:02:14]` en PalDefender), affiché dans sa propre colonne. */
private val TIMESTAMP_PREFIX = Regex("""^\[([^]]+)]\s*(.*)$""")

/** Second tag de sévérité éventuel (`[info]`, `[warning]`, `[error]`...) — remplacé par le badge. */
private val LEVEL_TAG_PREFIX = Regex("""^\[(info|warning|warn|error|err|debug|verbose|trace|fatal|critical)]\s*""", RegexOption.IGNORE_CASE)

/** UE4SS logge avec date + jusqu'à 7 décimales ("2026-08-06 10:02:26.5180697"), illisible sur un
 * écran étroit — on ne garde que l'heure avec 3 décimales ("10:02:26.518"). Déjà court (PalDefender,
 * "12:02:14") : inchangé. */
private fun shortenTimestamp(raw: String): String {
    val timePart = raw.substringAfterLast(' ')
    val dotIndex = timePart.indexOf('.')
    return if (dotIndex in 0..<timePart.length - 4) timePart.take(dotIndex + 4) else timePart
}

/** Un horodatage contient au moins un chiffre et un ':' — évite de prendre `[error]` ou un tag de
 * module pour une heure. */
private fun looksLikeTime(raw: String): Boolean = raw.any { it.isDigit() } && ':' in raw

/** Détection par mots-clés, pas de parseur dédié par outil (PalDefender/UE4SS n'ont pas le même
 * format) — suffisant pour repérer erreurs/avertissements d'un coup d'œil dans un flux de log brut,
 * les deux utilisent des tags "error"/"warning" en anglais sous une forme ou une autre. */
fun parseLogLine(line: String): LogEntry {
    val match = TIMESTAMP_PREFIX.find(line)
    val (time, rest) = if (match != null && looksLikeTime(match.groupValues[1])) {
        shortenTimestamp(match.groupValues[1]) to match.groupValues[2]
    } else {
        null to line
    }
    val lower = line.lowercase()
    val level = when {
        "error" in lower || "fatal" in lower || "exception" in lower -> LogLevel.ERROR
        "warn" in lower -> LogLevel.WARN
        "debug" in lower || "verbose" in lower -> LogLevel.DEBUG
        else -> LogLevel.INFO
    }
    return LogEntry(time, level, rest.replaceFirst(LEVEL_TAG_PREFIX, ""))
}
