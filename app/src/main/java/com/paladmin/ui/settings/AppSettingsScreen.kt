package com.paladmin.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_theme_header), style = MaterialTheme.typography.titleMedium)
                    Row {
                        FilterChip(
                            selected = state.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.settings_theme_system)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(stringResource(R.string.settings_theme_light)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text(stringResource(R.string.settings_theme_dark)) },
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_language_header), style = MaterialTheme.typography.titleMedium)
                    Row {
                        FilterChip(
                            selected = state.language == AppLanguage.FRENCH,
                            onClick = { viewModel.setLanguage(AppLanguage.FRENCH) { activity?.recreate() } },
                            label = { Text("Français") },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = state.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) { activity?.recreate() } },
                            label = { Text("English") },
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_debug_header), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_debug_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.debugLogFolderUri?.let { folderDisplayName(context, it) }
                                ?: stringResource(R.string.settings_debug_no_folder),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.debugLoggingEnabled,
                            onCheckedChange = { checked ->
                                if (checked && state.debugLogFolderUri == null) {
                                    folderPicker.launch(null)
                                } else {
                                    viewModel.setDebugLoggingEnabled(checked)
                                }
                            },
                        )
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }) {
                        Text(stringResource(R.string.settings_debug_choose_folder))
                    }
                }
            }
        }
    }
}

private fun folderDisplayName(context: android.content.Context, uriString: String): String =
    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uriString))?.name }
        .getOrNull() ?: uriString
