package com.paladmin.ui.palcreator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.PalInfoSection
import com.paladmin.util.pickLocalizedName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalCreatorScreen(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    viewModel: PalCreatorViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val form by viewModel.form.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val hostKeyMismatch by viewModel.hostKeyMismatch.collectAsState()
    val speciesResults by viewModel.speciesResults.collectAsState()
    val language by viewModel.language.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.palcreator_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(stringResource(R.string.logs_not_configured), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Button(onClick = onOpenEditProfile, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.logs_open_profile_settings))
                    }
                }
            }
            hostKeyMismatch != null -> {
                val (expected, actual) = hostKeyMismatch!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.logs_host_key_mismatch),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        stringResource(R.string.logs_host_key_fmt, expected, actual),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(onClick = viewModel::trustNewHostKey, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.logs_trust_new_key))
                    }
                }
            }
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpeciesField(
                    query = form.speciesQuery,
                    selected = form.selectedSpecies,
                    results = speciesResults,
                    language = language,
                    onQueryChange = { viewModel.update { state -> state.copy(speciesQuery = it) } },
                    onSelect = viewModel::selectSpecies,
                    onClear = viewModel::clearSpecies,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    ToggleGroup(title = stringResource(R.string.palcreator_gender_group)) {
                        ToggleWithCaption(
                            checked = form.gender == "Male",
                            caption = stringResource(R.string.pal_gender_male),
                            onCheckedChange = { checked -> viewModel.update { it.copy(gender = if (checked) "Male" else "None") } },
                        ) { Text("♂", style = MaterialTheme.typography.titleMedium) }
                        ToggleWithCaption(
                            checked = form.gender == "Female",
                            caption = stringResource(R.string.pal_gender_female),
                            onCheckedChange = { checked -> viewModel.update { it.copy(gender = if (checked) "Female" else "None") } },
                        ) { Text("♀", style = MaterialTheme.typography.titleMedium) }
                    }
                    ToggleGroup(title = stringResource(R.string.palcreator_type_group)) {
                        ToggleWithCaption(
                            checked = form.shiny,
                            caption = stringResource(R.string.palcreator_shiny_label),
                            onCheckedChange = { value -> viewModel.update { it.copy(shiny = value) } },
                        ) { FallbackAsyncImage(model = "file:///android_asset/images/status/lucky.webp", modifier = Modifier.size(24.dp)) }
                        ToggleWithCaption(
                            checked = form.isBoss,
                            caption = stringResource(R.string.palcreator_boss_label),
                            onCheckedChange = { value -> viewModel.update { it.copy(isBoss = value) } },
                        ) { FallbackAsyncImage(model = "file:///android_asset/images/status/boss_alpha.webp", modifier = Modifier.size(24.dp)) }
                    }
                }

                OutlinedTextField(
                    value = form.nickname,
                    onValueChange = { value -> viewModel.update { it.copy(nickname = value) } },
                    label = { Text(stringResource(R.string.palcreator_nickname_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = form.level,
                    onValueChange = { value -> viewModel.update { it.copy(level = value) } },
                    label = { Text(stringResource(R.string.palcreator_level_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                PalInfoSection(stringResource(R.string.pal_info_iv_header)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.ivHealth,
                            onValueChange = { value -> viewModel.update { it.copy(ivHealth = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_hp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = form.ivAttack,
                            onValueChange = { value -> viewModel.update { it.copy(ivAttack = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_atk)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = form.ivDefense,
                            onValueChange = { value -> viewModel.update { it.copy(ivDefense = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_def)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                PalInfoSection(stringResource(R.string.pal_info_soul_header)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.soulHealth,
                            onValueChange = { value -> viewModel.update { it.copy(soulHealth = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_hp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = form.soulAttack,
                            onValueChange = { value -> viewModel.update { it.copy(soulAttack = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_atk)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = form.soulDefense,
                            onValueChange = { value -> viewModel.update { it.copy(soulDefense = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_def)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = form.soulCraftSpeed,
                            onValueChange = { value -> viewModel.update { it.copy(soulCraftSpeed = value) } },
                            label = { Text(stringResource(R.string.palcreator_stat_work)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                PalInfoSection(stringResource(R.string.pal_info_active_skills)) {
                    CatalogMultiPicker(
                        label = stringResource(R.string.palcreator_search_skill_label),
                        query = form.activeSkillQuery,
                        onQueryChange = { value -> viewModel.update { it.copy(activeSkillQuery = value) } },
                        options = viewModel.filteredActiveSkills(),
                        selected = form.activeSkills,
                        maxSelected = 3,
                        onAdd = viewModel::addActiveSkill,
                        onRemove = viewModel::removeActiveSkill,
                    )
                }

                PalInfoSection(stringResource(R.string.pal_info_passives)) {
                    CatalogMultiPicker(
                        label = stringResource(R.string.palcreator_search_passive_label),
                        query = form.passiveQuery,
                        onQueryChange = { value -> viewModel.update { it.copy(passiveQuery = value) } },
                        options = viewModel.filteredPassives(),
                        selected = form.passives,
                        maxSelected = 4,
                        onAdd = viewModel::addPassive,
                        onRemove = viewModel::removePassive,
                    )
                }

                OutlinedTextField(
                    value = form.fileName,
                    onValueChange = { value -> viewModel.update { it.copy(fileName = value) } },
                    label = { Text(stringResource(R.string.palcreator_filename_label)) },
                    supportingText = { Text(stringResource(R.string.palcreator_filename_support)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { viewModel.save() },
                    enabled = !isSaving && form.selectedSpecies != null && form.fileName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.palcreator_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleGroup(title: String, content: @Composable RowScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp), content = content)
    }
}

@Composable
private fun ToggleWithCaption(
    checked: Boolean,
    caption: String,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconToggleButton(checked = checked, onCheckedChange = onCheckedChange) { content() }
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesField(
    query: String,
    selected: PalEntity?,
    results: List<PalEntity>,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onSelect: (PalEntity) -> Unit,
    onClear: () -> Unit,
) {
    if (selected != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClear),
        ) {
            FallbackAsyncImage(model = "file:///android_asset/images/pals/${selected.image}", modifier = Modifier.size(56.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(pickLocalizedName(selected.nameFr, selected.nameEn, language), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.palcreator_change_species),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.palcreator_change_species))
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && results.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it); expanded = true },
            label = { Text(stringResource(R.string.palcreator_species_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded && results.isNotEmpty(), onDismissRequest = { expanded = false }) {
            results.take(50).forEach { pal ->
                DropdownMenuItem(
                    text = { Text(pickLocalizedName(pal.nameFr, pal.nameEn, language)) },
                    leadingIcon = { FallbackAsyncImage(model = "file:///android_asset/images/pals/${pal.image}", modifier = Modifier.size(32.dp)) },
                    onClick = { onSelect(pal); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogMultiPicker(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    options: List<CatalogOption>,
    selected: List<CatalogOption>,
    maxSelected: Int,
    onAdd: (CatalogOption) -> Unit,
    onRemove: (CatalogOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        selected.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "• ${option.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(option) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_delete), modifier = Modifier.size(16.dp))
                }
            }
        }
        if (selected.size < maxSelected) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded && options.isNotEmpty(),
                onExpandedChange = { expanded = it },
                modifier = Modifier.padding(top = if (selected.isNotEmpty()) 4.dp else 0.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { onQueryChange(it); expanded = true },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                )
                ExposedDropdownMenu(expanded = expanded && options.isNotEmpty(), onDismissRequest = { expanded = false }) {
                    options.take(50).forEach { option ->
                        DropdownMenuItem(text = { Text(option.name) }, onClick = { onAdd(option); expanded = false })
                    }
                }
            }
        }
    }
}
