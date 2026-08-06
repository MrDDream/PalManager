package com.paladmin.ui.palcreator

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.R
import com.paladmin.data.local.dataset.ActiveSkillCatalog
import com.paladmin.data.local.dataset.PassiveSkillCatalog
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.model.SftpLogDefaults
import com.paladmin.data.remote.paldefender.PalIVs
import com.paladmin.data.remote.paldefender.PalSoulRanks
import com.paladmin.data.repository.PalRepository
import com.paladmin.data.repository.ServerRepository
import com.paladmin.data.sftp.SftpLogClient
import com.paladmin.data.sftp.TofuResult
import com.paladmin.data.sftp.sftpJoin
import com.paladmin.util.pickLocalizedName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import javax.inject.Inject

data class CatalogOption(val id: String, val name: String)

data class PalCreatorFormState(
    val speciesQuery: String = "",
    val selectedSpecies: PalEntity? = null,
    val fileName: String = "",
    val nickname: String = "",
    val level: String = "50",
    val gender: String = "None",
    val shiny: Boolean = false,
    val isBoss: Boolean = false,
    val ivHealth: String = "100",
    val ivAttack: String = "100",
    val ivDefense: String = "100",
    val soulHealth: String = "20",
    val soulAttack: String = "20",
    val soulDefense: String = "20",
    val soulCraftSpeed: String = "20",
    val activeSkillQuery: String = "",
    val activeSkills: List<CatalogOption> = emptyList(),
    val passiveQuery: String = "",
    val passives: List<CatalogOption> = emptyList(),
)

private const val MAX_ACTIVE_SKILLS = 3
private const val MAX_PASSIVES = 4
private val templateJson = Json { encodeDefaults = false; explicitNulls = false }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PalCreatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val palRepository: PalRepository,
    private val activeSkillCatalog: ActiveSkillCatalog,
    private val passiveSkillCatalog: PassiveSkillCatalog,
    private val sftpLogClient: SftpLogClient,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = checkNotNull(savedStateHandle.get<String>("profileId")).toLong()

    val language: StateFlow<AppLanguage> = appPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.FRENCH)

    private val _profile = MutableStateFlow<ServerProfile?>(null)
    val profile: StateFlow<ServerProfile?> = _profile.asStateFlow()

    private val _form = MutableStateFlow(PalCreatorFormState())
    val form: StateFlow<PalCreatorFormState> = _form.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _hostKeyMismatch = MutableStateFlow<Pair<String, String>?>(null)
    val hostKeyMismatch: StateFlow<Pair<String, String>?> = _hostKeyMismatch.asStateFlow()

    private var allActiveSkills: List<CatalogOption> = emptyList()
    private var allPassives: List<CatalogOption> = emptyList()

    val speciesResults: StateFlow<List<PalEntity>> = _form
        .flatMapLatest { form -> if (form.selectedSpecies != null) emptyFlow() else palRepository.search(form.speciesQuery) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _profile.value = serverRepository.getProfile(profileId)
            val language = appPreferences.language.first()
            allActiveSkills = activeSkillCatalog.all().map { CatalogOption(it.id, pickLocalizedName(it.nameFr, it.nameEn, language)) }.sortedBy { it.name }
            allPassives = passiveSkillCatalog.all().map { CatalogOption(it.id, pickLocalizedName(it.nameFr, it.nameEn, language)) }.sortedBy { it.name }
        }
    }

    fun update(transform: (PalCreatorFormState) -> PalCreatorFormState) {
        _form.value = transform(_form.value)
    }

    fun selectSpecies(pal: PalEntity) {
        update { it.copy(selectedSpecies = pal, speciesQuery = pickLocalizedName(pal.nameFr, pal.nameEn, language.value)) }
    }

    fun clearSpecies() {
        update { it.copy(selectedSpecies = null, speciesQuery = "") }
    }

    fun filteredActiveSkills(): List<CatalogOption> {
        val query = _form.value.activeSkillQuery
        val chosen = _form.value.activeSkills.map { it.id }.toSet()
        val pool = allActiveSkills.filter { it.id !in chosen }
        return if (query.isBlank()) pool else pool.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun filteredPassives(): List<CatalogOption> {
        val query = _form.value.passiveQuery
        val chosen = _form.value.passives.map { it.id }.toSet()
        val pool = allPassives.filter { it.id !in chosen }
        return if (query.isBlank()) pool else pool.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun addActiveSkill(option: CatalogOption) {
        update {
            if (it.activeSkills.size >= MAX_ACTIVE_SKILLS) return@update it
            it.copy(activeSkills = it.activeSkills + option, activeSkillQuery = "")
        }
    }

    fun removeActiveSkill(option: CatalogOption) {
        update { it.copy(activeSkills = it.activeSkills - option) }
    }

    fun addPassive(option: CatalogOption) {
        update {
            if (it.passives.size >= MAX_PASSIVES) return@update it
            it.copy(passives = it.passives + option, passiveQuery = "")
        }
    }

    fun removePassive(option: CatalogOption) {
        update { it.copy(passives = it.passives - option) }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun trustNewHostKey() {
        if (_hostKeyMismatch.value == null) return
        _hostKeyMismatch.value = null
        save(trustNewKey = true)
    }

    fun save(trustNewKey: Boolean = false) {
        val profile = _profile.value ?: return
        val form = _form.value
        val species = form.selectedSpecies ?: return
        val fileName = form.fileName.trim().removeSuffix(".json")
        if (fileName.isBlank()) return

        val templateFile = PalTemplateFile(
            palId = if (form.isBoss) "BOSS_${species.id}" else species.id,
            nickname = form.nickname.trim().ifBlank { null },
            gender = form.gender,
            level = form.level.trim().toIntOrNull(),
            shiny = form.shiny.takeIf { it },
            ivs = PalIVs(
                health = form.ivHealth.trim().toIntOrNull() ?: 0,
                attackMelee = form.ivAttack.trim().toIntOrNull() ?: 0,
                attackShot = form.ivAttack.trim().toIntOrNull() ?: 0,
                defense = form.ivDefense.trim().toIntOrNull() ?: 0,
            ),
            palSouls = PalSoulRanks(
                health = form.soulHealth.trim().toIntOrNull() ?: 0,
                attack = form.soulAttack.trim().toIntOrNull() ?: 0,
                defense = form.soulDefense.trim().toIntOrNull() ?: 0,
                craftSpeed = form.soulCraftSpeed.trim().toIntOrNull() ?: 0,
            ),
            activeSkills = form.activeSkills.map { it.id }.ifEmpty { null },
            passives = form.passives.map { it.id }.ifEmpty { null },
        )
        val jsonText = templateJson.encodeToString(PalTemplateFile.serializer(), templateFile)
        val remotePath = sftpJoin(
            profile.sftpPalTemplatesPath.ifBlank { SftpLogDefaults.PAL_TEMPLATES_PATH },
            "$fileName.json",
        )

        _isSaving.value = true
        viewModelScope.launch {
            val knownFingerprint = if (trustNewKey) null else profile.sftpHostKeyFingerprint
            val result = sftpLogClient.uploadFile(
                host = profile.host,
                port = profile.sftpPort,
                username = profile.sftpUsername,
                password = profile.sftpPassword,
                remotePath = remotePath,
                knownHostKeyFingerprint = knownFingerprint,
                source = ByteArrayInputStream(jsonText.toByteArray(Charsets.UTF_8)),
            )
            when (result) {
                is TofuResult.Success -> {
                    if (profile.sftpHostKeyFingerprint != result.hostKeyFingerprint) {
                        serverRepository.updateSftpHostKeyFingerprint(profileId, result.hostKeyFingerprint)
                        _profile.value = profile.copy(sftpHostKeyFingerprint = result.hostKeyFingerprint)
                    }
                    _isSaving.value = false
                    _statusMessage.value = context.getString(R.string.palcreator_saved_fmt, fileName)
                    _form.value = PalCreatorFormState()
                }
                is TofuResult.HostKeyMismatch -> {
                    _isSaving.value = false
                    _hostKeyMismatch.value = result.expectedFingerprint to result.actualFingerprint
                }
                is TofuResult.Failure -> {
                    _isSaving.value = false
                    _statusMessage.value = result.message
                }
            }
        }
    }
}
