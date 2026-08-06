package com.paladmin.ui.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.model.SftpLogDefaults
import com.paladmin.ui.components.IconBadge
import com.paladmin.ui.components.ServerIcons

private enum class LogPathField { PALDEFENDER, UE4SS, PAL_TEMPLATES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProfileScreen(
    onDone: () -> Unit,
    viewModel: AddEditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()
    var browsingField by remember { mutableStateOf<LogPathField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isNew) R.string.profile_edit_new_title else R.string.profile_edit_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconBadge(icon = ServerIcons.iconFor(state.iconKey), size = 72.dp)
                    Text(
                        state.name.ifBlank { stringResource(R.string.profile_edit_preview_placeholder) },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            CollapsibleCard(icon = Icons.Filled.Dns, title = stringResource(R.string.profile_edit_general_section)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                    label = { Text(stringResource(R.string.profile_edit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column {
                    Text(stringResource(R.string.profile_edit_icon_label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ServerIcons.options) { (key, icon) ->
                            val selected = key == state.iconKey
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                                    .clickable { viewModel.update { it.copy(iconKey = key) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.host,
                    onValueChange = { value -> viewModel.update { it.copy(host = value) } },
                    label = { Text(stringResource(R.string.profile_edit_host_label)) },
                    supportingText = { Text(stringResource(R.string.profile_edit_host_support)) },
                    leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CollapsibleCard(icon = Icons.Filled.Dns, title = stringResource(R.string.profile_edit_palworld_api)) {
                OutlinedTextField(
                    value = state.palworldPort,
                    onValueChange = { value -> viewModel.update { it.copy(palworldPort = value) } },
                    label = { Text(stringResource(R.string.profile_edit_port_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = state.palworldPassword,
                    onValueChange = { value -> viewModel.update { it.copy(palworldPassword = value) } },
                    label = stringResource(R.string.profile_edit_admin_password_label),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CollapsibleCard(icon = Icons.Filled.Shield, title = stringResource(R.string.profile_edit_paldefender_api)) {
                OutlinedTextField(
                    value = state.palDefenderPort,
                    onValueChange = { value -> viewModel.update { it.copy(palDefenderPort = value) } },
                    label = { Text(stringResource(R.string.profile_edit_port_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = state.palDefenderToken,
                    onValueChange = { value -> viewModel.update { it.copy(palDefenderToken = value) } },
                    label = stringResource(R.string.profile_edit_bearer_token_label),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AdvancedSection(
                state = state,
                onUpdate = viewModel::update,
                onBrowse = { field -> browsingField = field },
            )

            OutlinedButton(
                onClick = viewModel::verify,
                enabled = verifyState?.isChecking != true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (verifyState?.isChecking == true) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(stringResource(R.string.profile_edit_verify_action), modifier = Modifier.padding(start = 8.dp))
            }

            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save_action))
            }
        }
    }

    verifyState?.let { result ->
        VerifyResultDialog(result = result, onDismiss = viewModel::dismissVerify)
    }

    browsingField?.let { field ->
        SftpPathPickerDialog(
            initialPath = when (field) {
                LogPathField.PALDEFENDER -> state.sftpPalDefenderLogPath
                LogPathField.UE4SS -> state.sftpUe4ssLogPath
                LogPathField.PAL_TEMPLATES -> state.sftpPalTemplatesPath
            }.ifBlank { "." },
            browse = viewModel::browseSftp,
            onDismiss = { browsingField = null },
            onPathSelected = { path ->
                viewModel.update {
                    when (field) {
                        LogPathField.PALDEFENDER -> it.copy(sftpPalDefenderLogPath = path)
                        LogPathField.UE4SS -> it.copy(sftpUe4ssLogPath = path)
                        LogPathField.PAL_TEMPLATES -> it.copy(sftpPalTemplatesPath = path)
                    }
                }
                browsingField = null
            },
        )
    }
}

@Composable
private fun AdvancedSection(
    state: ProfileFormState,
    onUpdate: ((ProfileFormState) -> ProfileFormState) -> Unit,
    onBrowse: (LogPathField) -> Unit,
) {
    CollapsibleCard(icon = Icons.Filled.Terminal, title = stringResource(R.string.profile_edit_advanced_section), initiallyExpanded = false) {
        Text(
            stringResource(R.string.profile_edit_sftp_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.sftpPort,
            onValueChange = { value -> onUpdate { it.copy(sftpPort = value) } },
            label = { Text(stringResource(R.string.profile_edit_port_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.sftpUsername,
            onValueChange = { value -> onUpdate { it.copy(sftpUsername = value) } },
            label = { Text(stringResource(R.string.profile_edit_sftp_username_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        PasswordField(
            value = state.sftpPassword,
            onValueChange = { value -> onUpdate { it.copy(sftpPassword = value) } },
            label = stringResource(R.string.profile_edit_sftp_password_label),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.sftpPalDefenderLogPath,
            onValueChange = { value -> onUpdate { it.copy(sftpPalDefenderLogPath = value) } },
            label = { Text(stringResource(R.string.profile_edit_sftp_paldefender_log_label)) },
            placeholder = { Text(SftpLogDefaults.PALDEFENDER_LOG_PATH) },
            trailingIcon = { BrowseButton(onClick = { onBrowse(LogPathField.PALDEFENDER) }) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.sftpUe4ssLogPath,
            onValueChange = { value -> onUpdate { it.copy(sftpUe4ssLogPath = value) } },
            label = { Text(stringResource(R.string.profile_edit_sftp_ue4ss_log_label)) },
            placeholder = { Text(SftpLogDefaults.UE4SS_LOG_PATH) },
            trailingIcon = { BrowseButton(onClick = { onBrowse(LogPathField.UE4SS) }) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.sftpPalTemplatesPath,
            onValueChange = { value -> onUpdate { it.copy(sftpPalTemplatesPath = value) } },
            label = { Text(stringResource(R.string.profile_edit_sftp_pal_templates_label)) },
            placeholder = { Text(SftpLogDefaults.PAL_TEMPLATES_PATH) },
            supportingText = { Text(stringResource(R.string.profile_edit_sftp_pal_templates_support)) },
            trailingIcon = { BrowseButton(onClick = { onBrowse(LogPathField.PAL_TEMPLATES) }) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Bloc rétractable réutilisé pour chaque groupe de champs (Général, API Palworld/PalDefender,
 * Avancées) — permet de replier ce qu'on ne consulte pas pour gagner en visibilité. */
@Composable
private fun CollapsibleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
        ) {
            IconBadge(icon = icon, size = 32.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }

        AnimatedVisibility(visible = expanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    }
}

@Composable
private fun BrowseButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.FolderOpen, contentDescription = stringResource(R.string.profile_edit_sftp_browse_cd))
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (visible) R.string.cd_hide else R.string.cd_show),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun VerifyResultDialog(result: VerifyState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!result.isChecking) onDismiss() },
        title = { Text(stringResource(R.string.profile_verify_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VerifyResultRow(stringResource(R.string.profile_edit_palworld_api), result.palworld)
                VerifyResultRow(stringResource(R.string.profile_edit_paldefender_api), result.palDefender)
                result.sftp?.let { VerifyResultRow(stringResource(R.string.dashboard_action_sftp), it) }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, enabled = !result.isChecking) { Text(stringResource(R.string.common_close)) }
        },
    )
}

@Composable
private fun VerifyResultRow(label: String, result: CheckResult) {
    Row(verticalAlignment = Alignment.Top) {
        when (result.status) {
            CheckStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            CheckStatus.SUCCESS -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            CheckStatus.FAILURE -> Icon(Icons.Filled.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (result.status == CheckStatus.FAILURE && !result.message.isNullOrBlank()) {
                Text(result.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
