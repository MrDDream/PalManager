package com.paladmin.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data class OpenDashboard(val profileId: Long) : SplashDestination
    data object OpenProfileList : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val lastProfileId = appPreferences.lastProfileId.first()
            val profile = lastProfileId?.let { serverRepository.getProfile(it) }
            _destination.value = if (profile != null) {
                SplashDestination.OpenDashboard(profile.id)
            } else {
                SplashDestination.OpenProfileList
            }
        }
    }
}
