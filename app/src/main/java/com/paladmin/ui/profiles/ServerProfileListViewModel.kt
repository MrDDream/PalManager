package com.paladmin.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerProfileListViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    val profiles: StateFlow<List<ServerProfile>> = repository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch { repository.deleteProfile(profile) }
    }
}
