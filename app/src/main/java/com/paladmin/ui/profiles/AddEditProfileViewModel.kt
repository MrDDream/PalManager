package com.paladmin.ui.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileFormState(
    val id: Long = 0,
    val name: String = "",
    val iconKey: String = "dns",
    val host: String = "",
    val palworldPort: String = "8212",
    val palworldPassword: String = "",
    val palDefenderPort: String = "17993",
    val palDefenderToken: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class AddEditProfileViewModel @Inject constructor(
    private val repository: ServerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = savedStateHandle.get<String>("profileId")?.toLongOrNull()?.takeIf { it >= 0 } ?: -1
    val isNew: Boolean = profileId < 0

    private val _state = MutableStateFlow(ProfileFormState())
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    init {
        if (isNew) {
            _state.value = _state.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                repository.getProfile(profileId)?.let { profile ->
                    _state.value = ProfileFormState(
                        id = profile.id,
                        name = profile.name,
                        iconKey = profile.iconKey,
                        host = profile.host,
                        palworldPort = profile.palworldPort.toString(),
                        palworldPassword = profile.palworldPassword,
                        palDefenderPort = profile.palDefenderPort.toString(),
                        palDefenderToken = profile.palDefenderToken,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun update(transform: (ProfileFormState) -> ProfileFormState) {
        _state.value = transform(_state.value)
    }

    fun save(onSaved: () -> Unit) {
        val form = _state.value
        if (form.name.isBlank() || form.host.isBlank()) return
        viewModelScope.launch {
            repository.saveProfile(
                ServerProfile(
                    id = form.id,
                    name = form.name.trim(),
                    iconKey = form.iconKey,
                    host = form.host.trim(),
                    palworldPort = form.palworldPort.trim().toIntOrNull() ?: 8212,
                    // Le mot de passe n'est PAS trim() : un espace en fin de mot de passe
                    // pourrait être légitime côté serveur, on ne le modifie jamais silencieusement.
                    palworldPassword = form.palworldPassword,
                    palDefenderPort = form.palDefenderPort.trim().toIntOrNull() ?: 17993,
                    palDefenderToken = form.palDefenderToken,
                ),
            )
            onSaved()
        }
    }
}
