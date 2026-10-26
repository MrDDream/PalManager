package com.paladmin.ui.palcreator

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.CardTitle
import com.paladmin.ui.components.FallbackAsyncImage
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.PalIconButton
import com.paladmin.ui.components.PalSearchBar
import com.paladmin.ui.components.PalTextField
import com.paladmin.ui.components.PalTopBar
import com.paladmin.ui.components.ToolsHostKeyMismatchState
import com.paladmin.ui.components.ToolsLoadingState
import com.paladmin.ui.components.ToolsNotConfiguredState
import com.paladmin.ui.components.ToolsSegmented
import com.paladmin.ui.palpicker.PalLabels
import com.paladmin.ui.theme.PalTheme
import com.paladmin.ui.theme.Tone
import com.paladmin.util.pickLocalizedName

private const val MAX_ACTIVE = 3
private const val MAX_PASSIVE = 4

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

    PalCreatorContent(
        profileLoaded = profile != null,
        sftpConfigured = profile?.isSftpConfigured == true,
        hostKeyMismatch = hostKeyMismatch,
        form = form,
        isSaving = isSaving,
        speciesResults = speciesResults,
        activeSkillOptions = viewModel.filteredActiveSkills(),
        passiveOptions = viewModel.filteredPassives(),
        language = language,
        snackbarHostState = snackbarHostState,
        actions = PalCreatorActions(
            onBack = onBack,
            onOpenEditProfile = onOpenEditProfile,
            onTrustNewKey = viewModel::trustNewHostKey,
            onUpdate = viewModel::update,
            onSelectSpecies = viewModel::selectSpecies,
            onClearSpecies = viewModel::clearSpecies,
            onAddActiveSkill = viewModel::addActiveSkill,
            onRemoveActiveSkill = viewModel::removeActiveSkill,
            onAddPassive = viewModel::addPassive,
            onRemovePassive = viewModel::removePassive,
            onSave = { viewModel.save() },
        ),
    )
}

/** Callbacks de [PalCreatorContent]. */
data class PalCreatorActions(
    val onBack: () -> Unit = {},
    val onOpenEditProfile: () -> Unit = {},
    val onTrustNewKey: () -> Unit = {},
    val onUpdate: ((PalCreatorFormState) -> PalCreatorFormState) -> Unit = {},
    val onSelectSpecies: (PalEntity) -> Unit = {},
    val onClearSpecies: () -> Unit = {},
    val onAddActiveSkill: (CatalogOption) -> Unit = {},
    val onRemoveActiveSkill: (CatalogOption) -> Unit = {},
    val onAddPassive: (CatalogOption) -> Unit = {},
    val onRemovePassive: (CatalogOption) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun PalCreatorContent(
    profileLoaded: Boolean,
    sftpConfigured: Boolean,
    hostKeyMismatch: Pair<String, String>?,
    form: PalCreatorFormState,
    isSaving: Boolean,
    speciesResults: List<PalEntity>,
    activeSkillOptions: List<CatalogOption>,
    passiveOptions: List<CatalogOption>,
    language: AppLanguage,
    actions: PalCreatorActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = PalTheme.colors
    val showForm = profileLoaded && sftpConfigured && hostKeyMismatch == null
    val update = actions.onUpdate
    Scaffold(
        containerColor = c.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { PalTopBar(title = stringResource(R.string.palcreator_title), onBack = actions.onBack) },
        bottomBar = {
            if (showForm) {
                BottomActionBar {
                    PalButton(
                        text = stringResource(R.string.palcreator_save),
                        onClick = actions.onSave,
                        enabled = !isSaving && form.selectedSpecies != null && form.fileName.isNotBlank(),
                        height = 52.dp,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = PalTheme.type.button.copy(fontSize = 15.5.sp),
                        modifier = Modifier.fillMaxWidth(),
                        leading = if (isSaving) {
                            { CircularProgressIndicator(color = c.green.onSolid, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
        },
    ) { padding ->
        val inner = Modifier.fillMaxSize().padding(padding)
        when {
            !profileLoaded -> ToolsLoadingState(inner)
            !sftpConfigured -> ToolsNotConfiguredState(actions.onOpenEditProfile, inner)
            hostKeyMismatch != null -> ToolsHostKeyMismatchState(hostKeyMismatch.first, hostKeyMismatch.second, actions.onTrustNewKey, inner)
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = inner
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
            ) {
                val selected = form.selectedSpecies
                if (selected != null) {
                    SpeciesCard(selected, language, actions.onClearSpecies)
                } else {
                    SpeciesSearchCard(
                        query = form.speciesQuery,
                        results = speciesResults,
                        language = language,
                        onQueryChange = { value -> update { it.copy(speciesQuery = value) } },
                        onSelect = actions.onSelectSpecies,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.tools_creator_gender))
                        ToolsSegmented(
                            options = listOf(
                                "Male" to "♂ " + stringResource(R.string.pal_gender_male),
                                "Female" to "♀ " + stringResource(R.string.pal_gender_female),
                            ),
                            selected = form.gender.takeIf { it != "None" },
                            onSelect = { value -> update { it.copy(gender = value ?: "None") } },
                            allowDeselect = true,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.tools_creator_type))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TypeToggle(
                                label = stringResource(R.string.palcreator_shiny_label),
                                image = "file:///android_asset/images/status/lucky.webp",
                                checked = form.shiny,
                                accent = c.amber.solid,
                                onCheckedChange = { value -> update { it.copy(shiny = value) } },
                                modifier = Modifier.weight(1f),
                            )
                            TypeToggle(
                                label = stringResource(R.string.palcreator_boss_label),
                                image = "file:///android_asset/images/status/boss_alpha.webp",
                                checked = form.isBoss,
                                accent = c.danger.content,
                                onCheckedChange = { value -> update { it.copy(isBoss = value) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    PalTextField(
                        value = form.nickname,
                        onValueChange = { value -> update { it.copy(nickname = value) } },
                        label = stringResource(R.string.palcreator_nickname_label),
                        modifier = Modifier.weight(1f),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(stringResource(R.string.palcreator_level_label))
                        Stepper(
                            value = form.level,
                            onValueChange = { value -> update { it.copy(level = value) } },
                            step = 1,
                            range = 1..99,
                            fallback = 50,
                            decDescription = stringResource(R.string.tools_creator_level_dec),
                            incDescription = stringResource(R.string.tools_creator_level_inc),
                            framed = true,
                            valueStyleSora = true,
                        )
                    }
                }

                val hp = stringResource(R.string.palcreator_stat_hp)
                val atk = stringResource(R.string.palcreator_stat_atk)
                val def = stringResource(R.string.palcreator_stat_def)
                val work = stringResource(R.string.palcreator_stat_work)

                StatCard(stringResource(R.string.pal_info_iv_header), stringResource(R.string.tools_creator_iv_range)) {
                    StatRow(hp, form.ivHealth, { v -> update { it.copy(ivHealth = v) } }, 100, 10, c.green)
                    StatRow(atk, form.ivAttack, { v -> update { it.copy(ivAttack = v) } }, 100, 10, c.green)
                    StatRow(def, form.ivDefense, { v -> update { it.copy(ivDefense = v) } }, 100, 10, c.green)
                }

                StatCard(stringResource(R.string.pal_info_soul_header), stringResource(R.string.tools_creator_soul_range)) {
                    StatRow(hp, form.soulHealth, { v -> update { it.copy(soulHealth = v) } }, 20, 1, c.amber)
                    StatRow(atk, form.soulAttack, { v -> update { it.copy(soulAttack = v) } }, 20, 1, c.amber)
                    StatRow(def, form.soulDefense, { v -> update { it.copy(soulDefense = v) } }, 20, 1, c.amber)
                    StatRow(work, form.soulCraftSpeed, { v -> update { it.copy(soulCraftSpeed = v) } }, 20, 1, c.amber)
                }

                StatCard(stringResource(R.string.pal_info_active_skills), "${form.activeSkills.size}/$MAX_ACTIVE", monoHint = true) {
                    form.activeSkills.forEach { skill -> ActiveSkillRow(skill, onRemove = { actions.onRemoveActiveSkill(skill) }) }
                    if (form.activeSkills.size < MAX_ACTIVE) {
                        CatalogSearch(
                            placeholder = stringResource(R.string.palcreator_search_skill_label),
                            query = form.activeSkillQuery,
                            onQueryChange = { value -> update { it.copy(activeSkillQuery = value) } },
                            options = activeSkillOptions,
                            onPick = actions.onAddActiveSkill,
                        )
                    } else {
                        HintText(stringResource(R.string.tools_creator_skills_max))
                    }
                }

                StatCard(stringResource(R.string.pal_info_passives), "${form.passives.size}/$MAX_PASSIVE", monoHint = true) {
                    if (form.passives.isNotEmpty()) PassiveChips(form.passives, actions.onRemovePassive)
                    if (form.passives.size < MAX_PASSIVE) {
                        CatalogSearch(
                            placeholder = stringResource(R.string.palcreator_search_passive_label),
                            query = form.passiveQuery,
                            onQueryChange = { value -> update { it.copy(passiveQuery = value) } },
                            options = passiveOptions,
                            onPick = actions.onAddPassive,
                        )
                    } else {
                        HintText(stringResource(R.string.tools_creator_passives_max))
                    }
                }

                PalTextField(
                    value = form.fileName,
                    onValueChange = { value -> update { it.copy(fileName = value) } },
                    label = stringResource(R.string.tools_creator_filename_label),
                    placeholder = "my_pal_work",
                    supportingText = stringResource(R.string.palcreator_filename_support),
                    mono = true,
                    trailing = {
                        Text(".json", style = PalTheme.type.mono, color = c.textFaint, modifier = Modifier.padding(end = 12.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = PalTheme.type.bodySmall, color = PalTheme.colors.textMuted)
}

@Composable
private fun HintText(text: String) {
    Text(text, style = PalTheme.type.bodySmall.copy(fontSize = 12.5.sp), color = PalTheme.colors.textMuted)
}

@Composable
private fun SpeciesCard(pal: PalEntity, language: AppLanguage, onChange: () -> Unit) {
    val c = PalTheme.colors
    val amber = c.amber
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(amber.container.copy(alpha = 0.55f))
            .border(1.dp, amber.border.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(18.dp)).background(amber.container),
        ) {
            FallbackAsyncImage(model = "file:///android_asset/images/pals/${pal.image}", modifier = Modifier.size(84.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(
                if (pal.zukanIndex >= 0) stringResource(R.string.tools_creator_species_index_fmt, pal.zukanIndex) else stringResource(R.string.palcreator_species_label),
                style = PalTheme.type.caption,
                color = amber.onContainer.copy(alpha = 0.85f),
            )
            Text(
                pickLocalizedName(pal.nameFr, pal.nameEn, language),
                style = PalTheme.type.cardTitle.copy(fontSize = 20.sp, lineHeight = 25.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOfNotNull(pal.element1, pal.element2).forEach { element ->
                    FallbackAsyncImage(model = "file:///android_asset/images/elements/$element.webp", modifier = Modifier.size(18.dp))
                    Text(PalLabels.elementLabel(element), style = PalTheme.type.bodySmall, color = c.textSecondary)
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, amber.border, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onChange)
                .padding(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.tools_creator_change), style = PalTheme.type.label, color = amber.onContainer)
        }
    }
}

@Composable
private fun SpeciesSearchCard(
    query: String,
    results: List<PalEntity>,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onSelect: (PalEntity) -> Unit,
) {
    val c = PalTheme.colors
    PalCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
        CardTitle(stringResource(R.string.palcreator_species_label))
        PalSearchBar(value = query, onValueChange = onQueryChange, placeholder = stringResource(R.string.tools_creator_species_search_hint))
        if (results.isEmpty()) {
            if (query.isNotBlank()) HintText(stringResource(R.string.tools_creator_no_match))
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                results.take(50).forEach { pal ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onSelect(pal) }
                            .padding(horizontal = 6.dp),
                    ) {
                        Box(Modifier.size(40.dp).background(c.surfaceRaised, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            FallbackAsyncImage(model = "file:///android_asset/images/pals/${pal.image}", modifier = Modifier.size(36.dp))
                        }
                        Text(
                            pickLocalizedName(pal.nameFr, pal.nameEn, language),
                            style = PalTheme.type.body,
                            color = c.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (pal.zukanIndex >= 0) {
                            Text("#${pal.zukanIndex}", style = PalTheme.type.mono.copy(fontSize = 12.sp), color = c.textFaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeToggle(
    label: String,
    image: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PalTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (checked) c.surfaceRaised else Color.Transparent)
            .border(1.dp, if (checked) accent else c.borderStrong, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 2.dp),
    ) {
        FallbackAsyncImage(model = image, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = PalTheme.type.label.copy(fontSize = 12.sp, letterSpacing = (-0.1).sp),
            color = if (checked) c.text else c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * − valeur + : la valeur reste saisissable au clavier (chaîne libre, comme l'ancien champ texte) ;
 * les boutons bornent à [range] et partent de [fallback] si la saisie n'est pas un nombre.
 */
@Composable
private fun Stepper(
    value: String,
    onValueChange: (String) -> Unit,
    step: Int,
    range: IntRange,
    fallback: Int,
    decDescription: String,
    incDescription: String,
    framed: Boolean,
    valueStyleSora: Boolean = false,
) {
    val c = PalTheme.colors
    val current = value.trim().toIntOrNull()
    val frame = if (framed) {
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.borderStrong, RoundedCornerShape(12.dp))
    } else {
        Modifier.clip(RoundedCornerShape(10.dp)).background(c.field)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = frame) {
        StepButton("−", decDescription, enabled = (current ?: fallback) > range.first) {
            onValueChange(((current ?: fallback) - step).coerceIn(range).toString())
        }
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true,
            textStyle = (if (valueStyleSora) PalTheme.type.cardTitle.copy(fontSize = 16.sp) else PalTheme.type.mono.copy(fontWeight = FontWeight.Medium))
                .copy(color = c.text, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(c.green.solid),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(if (valueStyleSora) 34.dp else 32.dp),
        )
        StepButton("+", incDescription, enabled = (current ?: fallback) < range.last) {
            onValueChange(((current ?: fallback) + step).coerceIn(range).toString())
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = PalTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 44.dp, height = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(symbol, style = PalTheme.type.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = if (enabled) c.text else c.textFaint)
    }
}

@Composable
private fun StatCard(title: String, hint: String, monoHint: Boolean = false, content: @Composable () -> Unit) {
    val c = PalTheme.colors
    PalCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CardTitle(title, modifier = Modifier.weight(1f))
            Text(hint, style = if (monoHint) PalTheme.type.mono.copy(fontSize = 12.sp) else PalTheme.type.caption, color = c.textMuted)
        }
        content()
    }
}

@Composable
private fun StatRow(label: String, value: String, onValueChange: (String) -> Unit, max: Int, step: Int, tone: Tone) {
    val c = PalTheme.colors
    val fraction = ((value.trim().toIntOrNull() ?: 0).toFloat() / max).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = PalTheme.type.bodySmall.copy(fontSize = 13.5.sp), color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(64.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(c.field)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(8.dp)).background(tone.solid))
        }
        Stepper(
            value = value,
            onValueChange = onValueChange,
            step = step,
            range = 0..max,
            fallback = max,
            decDescription = stringResource(R.string.tools_creator_dec_fmt, label),
            incDescription = stringResource(R.string.tools_creator_inc_fmt, label),
            framed = false,
        )
    }
}

@Composable
private fun ActiveSkillRow(skill: CatalogOption, onRemove: () -> Unit) {
    val c = PalTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.field)
            .padding(start = 10.dp),
    ) {
        skill.element?.let { FallbackAsyncImage(model = "file:///android_asset/images/elements/$it.webp", modifier = Modifier.size(20.dp)) }
        Text(skill.name, style = PalTheme.type.body.copy(fontSize = 14.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        skillMeta(skill)?.let { Text(it, style = PalTheme.type.mono.copy(fontSize = 11.5.sp), color = c.textMuted, maxLines = 1) }
        PalIconButton(Icons.Filled.Close, stringResource(R.string.tools_creator_remove_fmt, skill.name), onClick = onRemove, tint = c.textMuted, iconSize = 18.dp)
    }
}

@Composable
private fun skillMeta(skill: CatalogOption): String? =
    if (skill.power != null && skill.cooldown != null) stringResource(R.string.tools_creator_skill_meta_fmt, skill.power, skill.cooldown) else null

/** Rang de passif → famille de couleur (même lecture que la fiche Pal : rouge négatif, gris
 * classique, ambre épique, bleu légendaire). */
@Composable
private fun passiveTone(rank: Int?): Tone = with(PalTheme.colors) {
    when {
        rank == null -> neutral
        rank < 0 -> danger
        rank <= 1 -> neutral
        rank <= 3 -> amber
        else -> sky
    }
}

@Composable
private fun PassiveRankIcon(rank: Int?, tint: Color) {
    when {
        rank == null -> Unit
        rank < 0 -> Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        else -> FallbackAsyncImage(model = "file:///android_asset/images/status/passive_rank_${rank.coerceIn(1, 5)}.webp", tint = tint, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassiveChips(passives: List<CatalogOption>, onRemove: (CatalogOption) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        passives.forEach { passive ->
            val tone = passiveTone(passive.rank)
            val description = stringResource(R.string.tools_creator_remove_fmt, passive.name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(tone.container)
                    .clickable(role = Role.Button) { onRemove(passive) }
                    .semantics { contentDescription = description }
                    .padding(start = 12.dp, end = 8.dp),
            ) {
                PassiveRankIcon(passive.rank, tone.content)
                Text(passive.name, style = PalTheme.type.label, color = tone.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp).background(PalTheme.colors.background.copy(alpha = 0.35f), CircleShape),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = tone.onContainer, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** Champ de recherche dans un catalogue ; suggestions en ligne tant que le champ a le focus ou une saisie. */
@Composable
private fun CatalogSearch(
    placeholder: String,
    query: String,
    onQueryChange: (String) -> Unit,
    options: List<CatalogOption>,
    onPick: (CatalogOption) -> Unit,
) {
    val c = PalTheme.colors
    var focused by remember { mutableStateOf(false) }
    PalSearchBar(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        modifier = Modifier.onFocusChanged { focused = it.hasFocus },
    )
    if (focused || query.isNotBlank()) {
        if (options.isEmpty()) {
            HintText(stringResource(R.string.tools_creator_no_match))
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.field)
                    .border(1.dp, c.borderStrong, RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState()),
            ) {
                options.take(50).forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onPick(option) }
                            .padding(horizontal = 12.dp),
                    ) {
                        option.element?.let { FallbackAsyncImage(model = "file:///android_asset/images/elements/$it.webp", modifier = Modifier.size(18.dp)) }
                        if (option.rank != null) PassiveRankIcon(option.rank, passiveTone(option.rank).content)
                        Text(option.name, style = PalTheme.type.body.copy(fontSize = 14.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        skillMeta(option)?.let { Text(it, style = PalTheme.type.mono.copy(fontSize = 11.5.sp), color = c.textMuted, maxLines = 1) }
                    }
                }
            }
        }
    }
}
