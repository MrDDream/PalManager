package com.paladmin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.local.prefs.AppLanguage
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.local.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.FRENCH,
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val state: StateFlow<AppSettingsUiState> = combine(appPreferences.themeMode, appPreferences.language) { theme, language ->
        AppSettingsUiState(theme, language)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            appPreferences.setLanguage(language)
            onComplete()
        }
    }
}
