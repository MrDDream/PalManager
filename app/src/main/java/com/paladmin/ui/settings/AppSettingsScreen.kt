package com.paladmin.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.ThemeMode
import com.paladmin.ui.components.CardTitle
import com.paladmin.ui.components.DashedButton
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalDivider
import com.paladmin.ui.components.PalListRow
import com.paladmin.ui.components.PalSwitch
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.SegmentedControl
import com.paladmin.ui.theme.DarkPalColors
import com.paladmin.ui.theme.LightPalColors
import com.paladmin.ui.theme.PalTheme

@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onManageServers: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setDebugLogFolder(uri)
        }
    }
    val folderName = state.debugLogFolderUri?.let { folderDisplayName(context, it) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    AppSettingsContent(
        state = state,
        folderName = folderName,
        version = version,
        actions = AppSettingsActions(
            onBack = onBack,
            onThemeMode = viewModel::setThemeMode,
            onLanguage = { language -> viewModel.setLanguage(language) { activity?.recreate() } },
            onDebugToggle = { checked ->
                if (checked && state.debugLogFolderUri == null) {
                    folderPicker.launch(null)
                } else {
                    viewModel.setDebugLoggingEnabled(checked)
                }
            },
            onChooseFolder = { folderPicker.launch(null) },
            onManageServers = onManageServers,
        ),
    )
}

/** Callbacks de [AppSettingsContent]. */
data class AppSettingsActions(
    val onBack: () -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onDebugToggle: (Boolean) -> Unit = {},
    val onChooseFolder: () -> Unit = {},
    val onManageServers: () -> Unit = {},
)

@Composable
fun AppSettingsContent(
    state: AppSettingsUiState,
    folderName: String?,
    version: String?,
    actions: AppSettingsActions,
) {
    val c = PalTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = { PalTopBar(title = stringResource(R.string.settings_title), onBack = actions.onBack) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            PalCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                CardTitle(stringResource(R.string.settings_theme_header))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    ThemeOption.entries.forEach { option ->
                        ThemeTile(
                            option = option,
                            selected = state.themeMode == option.mode,
                            onClick = { actions.onThemeMode(option.mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            PalCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                CardTitle(stringResource(R.string.settings_language_header))
                Text(stringResource(R.string.servers_settings_language_hint), style = PalTheme.type.bodySmall, color = c.textMuted)
                SegmentedControl(
                    // Noms de langue volontairement non traduits (chacun dans sa propre langue).
                    options = listOf(AppLanguage.FRENCH to "Français", AppLanguage.ENGLISH to "English"),
                    selected = state.language,
                    onSelect = actions.onLanguage,
                )
            }

            PalCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
                val debugTitle = stringResource(R.string.servers_settings_debug_title)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        CardTitle(debugTitle)
                        Text(stringResource(R.string.settings_debug_description), style = PalTheme.type.bodySmall, color = c.textMuted)
                    }
                    PalSwitch(
                        checked = state.debugLoggingEnabled,
                        onCheckedChange = actions.onDebugToggle,
                        modifier = Modifier.semantics { contentDescription = debugTitle },
                    )
                }
                if (state.debugLoggingEnabled) {
                    DashedButton(
                        text = stringResource(R.string.settings_debug_choose_folder),
                        onClick = actions.onChooseFolder,
                        height = 52.dp,
                        radius = 14.dp,
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = c.sky.content, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_debug_choose_folder), style = PalTheme.type.body.copy(fontSize = 14.sp), color = c.text)
                            Text(
                                folderName ?: stringResource(R.string.settings_debug_no_folder),
                                style = PalTheme.type.mono.copy(fontSize = 11.5.sp),
                                color = c.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            PalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), spacing = 0.dp) {
                PalListRow(
                    title = stringResource(R.string.servers_settings_manage),
                    onClick = actions.onManageServers,
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textMuted) },
                )
                PalDivider()
                PalListRow(
                    title = stringResource(R.string.servers_settings_version),
                    trailing = {
                        Text(version ?: "—", style = PalTheme.type.mono.copy(fontSize = 13.sp), color = c.textMuted)
                    },
                )
            }

            Text(
                stringResource(R.string.servers_settings_disclaimer),
                style = PalTheme.type.caption,
                color = c.textFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Vignettes de thème : aperçu dessiné avec les vraies palettes de l'app. */
private enum class ThemeOption(val mode: ThemeMode) { System(ThemeMode.SYSTEM), Light(ThemeMode.LIGHT), Dark(ThemeMode.DARK) }

@Composable
private fun ThemeTile(option: ThemeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PalTheme.colors
    val (previewBg: Color, previewFg: Color, accent: Color) = when (option) {
        ThemeOption.System -> Triple(DarkPalColors.surfaceRaised, DarkPalColors.textMuted, DarkPalColors.green.solid)
        ThemeOption.Light -> Triple(LightPalColors.background, LightPalColors.text, LightPalColors.green.solid)
        ThemeOption.Dark -> Triple(DarkPalColors.background, DarkPalColors.text, DarkPalColors.green.solid)
    }
    val label = stringResource(
        when (option) {
            ThemeOption.System -> R.string.settings_theme_system
            ThemeOption.Light -> R.string.settings_theme_light
            ThemeOption.Dark -> R.string.settings_theme_dark
        },
    )
    val shape = RoundedCornerShape(14.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(c.field)
            .border(2.dp, if (selected) c.green.solid else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewBg)
                .border(1.dp, c.borderStrong, RoundedCornerShape(10.dp))
                .padding(8.dp),
        ) {
            Box(Modifier.fillMaxWidth(0.6f).height(6.dp).background(previewFg, RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxWidth(0.4f).height(6.dp).background(accent, RoundedCornerShape(3.dp)))
        }
        Text(
            label,
            style = PalTheme.type.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) c.text else c.textBody,
            maxLines = 1,
        )
    }
}

private fun folderDisplayName(context: android.content.Context, uriString: String): String =
    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uriString))?.name }
        .getOrNull() ?: uriString
