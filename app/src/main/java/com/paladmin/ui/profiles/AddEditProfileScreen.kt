package com.paladmin.ui.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.model.SftpLogDefaults
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.MonoBadge
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.components.ServerIcons
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone

/** Les trois chemins SFTP qui ont un sélecteur "Parcourir". */
enum class SftpPathField { PALDEFENDER, UE4SS, PAL_TEMPLATES }

@Composable
fun AddEditProfileScreen(
    onDone: () -> Unit,
    viewModel: AddEditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()
    var browsingField by remember { mutableStateOf<SftpPathField?>(null) }

    AddEditProfileContent(
        isNew = viewModel.isNew,
        state = state,
        verifyState = verifyState,
        actions = AddEditProfileActions(
            onBack = onDone,
            onUpdate = viewModel::update,
            onVerify = viewModel::verify,
            onDismissVerify = viewModel::dismissVerify,
            onSave = { viewModel.save(onDone) },
            onBrowse = { browsingField = it },
        ),
    )

    browsingField?.let { field ->
        SftpPathPickerDialog(
            initialPath = when (field) {
                SftpPathField.PALDEFENDER -> state.sftpPalDefenderLogPath
                SftpPathField.UE4SS -> state.sftpUe4ssLogPath
                SftpPathField.PAL_TEMPLATES -> state.sftpPalTemplatesPath
            }.ifBlank { "." },
            browse = viewModel::browseSftp,
            onDismiss = { browsingField = null },
            onPathSelected = { path ->
                viewModel.update {
                    when (field) {
                        SftpPathField.PALDEFENDER -> it.copy(sftpPalDefenderLogPath = path)
                        SftpPathField.UE4SS -> it.copy(sftpUe4ssLogPath = path)
                        SftpPathField.PAL_TEMPLATES -> it.copy(sftpPalTemplatesPath = path)
                    }
                }
                browsingField = null
            },
        )
    }
}

/** Callbacks de [AddEditProfileContent]. */
data class AddEditProfileActions(
    val onBack: () -> Unit = {},
    val onUpdate: ((ProfileFormState) -> ProfileFormState) -> Unit = {},
    val onVerify: () -> Unit = {},
    val onDismissVerify: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onBrowse: (SftpPathField) -> Unit = {},
)

@Composable
fun AddEditProfileContent(
    isNew: Boolean,
    state: ProfileFormState,
    verifyState: VerifyState?,
    actions: AddEditProfileActions,
    sftpInitiallyExpanded: Boolean? = null,
) {
    val c = PalTheme.colors
    val scroll = rememberScrollState()
    // Les champs obligatoires ne passent en erreur qu'après une tentative d'enregistrement.
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val nameMissing = state.name.isBlank()
    val hostMissing = state.host.isBlank()

    // Le résultat de vérification s'affiche en bas du formulaire : on y amène l'utilisateur.
    val hasResult = verifyState != null
    LaunchedEffect(hasResult) {
        if (hasResult) {
            withFrameNanos { }
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = {
            PalTopBar(
                title = stringResource(if (isNew) R.string.profile_edit_new_title else R.string.profile_edit_edit_title),
                onBack = actions.onBack,
            )
        },
        bottomBar = {
            BottomActionBar {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val checking = verifyState?.isChecking == true
                    PalButton(
                        text = stringResource(R.string.profile_edit_verify_action),
                        onClick = actions.onVerify,
                        style = PalButtonStyle.Neutral,
                        enabled = !checking && !state.isLoading,
                        height = 52.dp,
                        shape = RoundedCornerShape(16.dp),
                        leading = if (checking) {
                            { CircularProgressIndicator(color = c.text, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PalButton(
                        text = stringResource(R.string.common_save_action),
                        onClick = {
                            if (nameMissing || hostMissing) showErrors = true else actions.onSave()
                        },
                        enabled = !state.isLoading,
                        height = 52.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.green.solid)
            }
            return@Scaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            FormSection(stringResource(R.string.profile_edit_general_section)) {
                PalTextField(
                    value = state.name,
                    onValueChange = { value -> actions.onUpdate { it.copy(name = value) } },
                    label = stringResource(R.string.profile_edit_name_label),
                    placeholder = stringResource(R.string.profile_edit_preview_placeholder),
                    isError = showErrors && nameMissing,
                    supportingText = if (showErrors && nameMissing) stringResource(R.string.servers_form_required) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                IconPicker(selectedKey = state.iconKey, onSelect = { key -> actions.onUpdate { it.copy(iconKey = key) } })
                PalTextField(
                    value = state.host,
                    onValueChange = { value -> actions.onUpdate { it.copy(host = value) } },
                    label = stringResource(R.string.profile_edit_host_label),
                    placeholder = "192.168.1.20",
                    mono = true,
                    keyboardType = KeyboardType.Uri,
                    isError = showErrors && hostMissing,
                    supportingText = if (showErrors && hostMissing) {
                        stringResource(R.string.servers_form_required)
                    } else {
                        stringResource(R.string.profile_edit_host_support)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormSection(stringResource(R.string.profile_edit_palworld_api)) {
                PortAndSecretRow(
                    port = state.palworldPort,
                    onPortChange = { value -> actions.onUpdate { it.copy(palworldPort = value) } },
                    portPlaceholder = "8212",
                    secret = state.palworldPassword,
                    onSecretChange = { value -> actions.onUpdate { it.copy(palworldPassword = value) } },
                    secretLabel = stringResource(R.string.profile_edit_admin_password_label),
                )
            }

            FormSection(stringResource(R.string.profile_edit_paldefender_api)) {
                PortAndSecretRow(
                    port = state.palDefenderPort,
                    onPortChange = { value -> actions.onUpdate { it.copy(palDefenderPort = value) } },
                    portPlaceholder = "17993",
                    secret = state.palDefenderToken,
                    onSecretChange = { value -> actions.onUpdate { it.copy(palDefenderToken = value) } },
                    secretLabel = stringResource(R.string.profile_edit_bearer_token_label),
                )
            }

            SftpSection(state, actions, sftpInitiallyExpanded ?: state.sftpUsername.isNotBlank())

            verifyState?.let { VerifyResultCard(it, actions.onDismissVerify) }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    PalCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), spacing = 12.dp) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun PortAndSecretRow(
    port: String,
    onPortChange: (String) -> Unit,
    portPlaceholder: String,
    secret: String,
    onSecretChange: (String) -> Unit,
    secretLabel: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PalTextField(
            value = port,
            onValueChange = onPortChange,
            label = stringResource(R.string.profile_edit_port_label),
            placeholder = portPlaceholder,
            mono = true,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.width(96.dp),
        )
        PalTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = secretLabel,
            password = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Grille de choix d'icône (5 colonnes), mêmes clés que [ServerIcons]. */
@Composable
private fun IconPicker(selectedKey: String, onSelect: (String) -> Unit) {
    val c = PalTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.profile_edit_icon_label), style = PalTheme.type.bodySmall, color = c.textSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.selectableGroup()) {
            ServerIcons.options.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (key, icon) ->
                        val on = key == selectedKey
                        val shape = RoundedCornerShape(12.dp)
                        val label = stringResource(R.string.servers_form_icon_cd, iconLabel(key))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(shape)
                                .background(if (on) c.green.container else c.field)
                                .border(1.dp, if (on) c.green.solid else c.borderStrong, shape)
                                .selectable(selected = on, role = Role.RadioButton) { onSelect(key) }
                                .semantics { contentDescription = label },
                        ) {
                            Icon(icon, contentDescription = null, tint = if (on) c.green.content else c.textMuted, modifier = Modifier.size(22.dp))
                        }
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun iconLabel(key: String): String = stringResource(
    when (key) {
        "storage" -> R.string.servers_icon_storage
        "cloud" -> R.string.servers_icon_cloud
        "public" -> R.string.servers_icon_public
        "terminal" -> R.string.servers_icon_terminal
        "games" -> R.string.servers_icon_games
        "shield" -> R.string.servers_icon_shield
        "star" -> R.string.servers_icon_star
        "forest" -> R.string.servers_icon_forest
        else -> R.string.servers_icon_dns
    },
)

@Composable
private fun SftpSection(state: ProfileFormState, actions: AddEditProfileActions, initiallyExpanded: Boolean) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(20.dp)
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val toggleLabel = stringResource(if (expanded) R.string.servers_form_sftp_collapse else R.string.servers_form_sftp_expand)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.sky.border, shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = toggleLabel) { expanded = !expanded }
                .padding(horizontal = 14.dp),
        ) {
            MonoBadge(stringResource(R.string.servers_badge_sftp), tone = c.sky)
            Text(stringResource(R.string.servers_form_sftp_title), style = PalTheme.type.sectionTitle, color = c.text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.servers_form_optional), style = PalTheme.type.caption.copy(fontSize = 12.5.sp), color = c.textMuted)
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = c.textMuted)
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            ) {
                Text(stringResource(R.string.servers_form_sftp_intro), style = PalTheme.type.bodySmall, color = c.textBody)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PalTextField(
                        value = state.sftpPort,
                        onValueChange = { value -> actions.onUpdate { it.copy(sftpPort = value) } },
                        label = stringResource(R.string.profile_edit_port_label),
                        placeholder = "22",
                        mono = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.width(96.dp),
                    )
                    PalTextField(
                        value = state.sftpUsername,
                        onValueChange = { value -> actions.onUpdate { it.copy(sftpUsername = value) } },
                        label = stringResource(R.string.profile_edit_sftp_username_label),
                        modifier = Modifier.weight(1f),
                    )
                }
                PalTextField(
                    value = state.sftpPassword,
                    onValueChange = { value -> actions.onUpdate { it.copy(sftpPassword = value) } },
                    label = stringResource(R.string.profile_edit_sftp_password_label),
                    password = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PathField(
                    value = state.sftpPalDefenderLogPath,
                    onValueChange = { value -> actions.onUpdate { it.copy(sftpPalDefenderLogPath = value) } },
                    label = stringResource(R.string.profile_edit_sftp_paldefender_log_label),
                    placeholder = SftpLogDefaults.PALDEFENDER_LOG_PATH,
                    onBrowse = { actions.onBrowse(SftpPathField.PALDEFENDER) },
                )
                PathField(
                    value = state.sftpUe4ssLogPath,
                    onValueChange = { value -> actions.onUpdate { it.copy(sftpUe4ssLogPath = value) } },
                    label = stringResource(R.string.profile_edit_sftp_ue4ss_log_label),
                    placeholder = SftpLogDefaults.UE4SS_LOG_PATH,
                    onBrowse = { actions.onBrowse(SftpPathField.UE4SS) },
                )
                PathField(
                    value = state.sftpPalTemplatesPath,
                    onValueChange = { value -> actions.onUpdate { it.copy(sftpPalTemplatesPath = value) } },
                    label = stringResource(R.string.profile_edit_sftp_pal_templates_label),
                    placeholder = SftpLogDefaults.PAL_TEMPLATES_PATH,
                    supportingText = stringResource(R.string.profile_edit_sftp_pal_templates_support),
                    onBrowse = { actions.onBrowse(SftpPathField.PAL_TEMPLATES) },
                )
                state.sftpHostKeyFingerprint?.takeIf { it.isNotBlank() }?.let { fingerprint ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.servers_form_host_key), style = PalTheme.type.bodySmall, color = c.textSecondary)
                        Text(fingerprint, style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    onBrowse: () -> Unit,
    supportingText: String? = null,
) {
    val c = PalTheme.colors
    PalTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        mono = true,
        modifier = Modifier.fillMaxWidth(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(1.dp).height(48.dp).background(c.borderStrong))
                PalIconButton(
                    icon = Icons.Filled.FolderOpen,
                    contentDescription = stringResource(R.string.profile_edit_sftp_browse_cd),
                    onClick = onBrowse,
                    tint = c.sky.content,
                    iconSize = 20.dp,
                )
            }
        },
    )
}

@Composable
private fun VerifyResultCard(result: VerifyState, onDismiss: () -> Unit) {
    val c = PalTheme.colors
    PalCard(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 14.dp),
        spacing = 8.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.servers_verify_title), style = PalTheme.type.sectionTitle, color = c.text, modifier = Modifier.weight(1f))
            PalIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.servers_verify_dismiss),
                onClick = onDismiss,
                enabled = !result.isChecking,
                tint = c.textMuted,
                iconSize = 18.dp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 8.dp)) {
            VerifyRow(stringResource(R.string.profile_edit_palworld_api), result.palworld)
            VerifyRow(stringResource(R.string.profile_edit_paldefender_api), result.palDefender)
            result.sftp?.let { VerifyRow(stringResource(R.string.dashboard_action_sftp), it) }
        }
    }
}

@Composable
private fun VerifyRow(label: String, result: CheckResult) {
    val c = PalTheme.colors
    val tone: Tone = when (result.status) {
        CheckStatus.CHECKING -> c.neutral
        CheckStatus.SUCCESS -> c.green
        CheckStatus.FAILURE -> c.danger
    }
    val value = when (result.status) {
        CheckStatus.CHECKING -> stringResource(R.string.servers_verify_checking)
        CheckStatus.SUCCESS -> result.latencyMs?.let { stringResource(R.string.servers_verify_latency_fmt, it.toInt()) }
            ?: stringResource(R.string.servers_verify_ok)
        CheckStatus.FAILURE -> stringResource(R.string.servers_verify_failed)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(c.field, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { stateDescription = value },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).background(tone.container, CircleShape)) {
                when (result.status) {
                    CheckStatus.CHECKING -> CircularProgressIndicator(color = c.textMuted, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    CheckStatus.SUCCESS -> Icon(Icons.Filled.Check, contentDescription = null, tint = tone.onContainer, modifier = Modifier.size(16.dp))
                    CheckStatus.FAILURE -> Icon(Icons.Filled.Close, contentDescription = null, tint = tone.onContainer, modifier = Modifier.size(16.dp))
                }
            }
            Text(label, style = PalTheme.type.body.copy(fontSize = 14.sp), color = c.text, modifier = Modifier.weight(1f))
            Text(
                value,
                style = PalTheme.type.mono.copy(fontSize = 12.sp),
                color = if (result.status == CheckStatus.CHECKING) c.textMuted else tone.onContainer,
                maxLines = 1,
            )
        }
        if (result.status == CheckStatus.FAILURE && !result.message.isNullOrBlank()) {
            Text(
                result.message,
                style = PalTheme.type.caption,
                color = c.textMuted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 36.dp),
            )
        }
    }
}
