package com.paladmin.ui.settings

import android.net.Uri
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
    val debugLoggingEnabled: Boolean = false,
    val debugLogFolderUri: String? = null,
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val state: StateFlow<AppSettingsUiState> = combine(
        appPreferences.themeMode,
        appPreferences.language,
        appPreferences.debugLoggingEnabled,
        appPreferences.debugLogFolderUri,
    ) { theme, language, debugEnabled, debugFolder ->
        AppSettingsUiState(theme, language, debugEnabled, debugFolder)
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

    fun setDebugLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDebugLoggingEnabled(enabled) }
    }

    /** Enregistre le dossier choisi (permission déjà prise par l'appelant) et active le logging. */
    fun setDebugLogFolder(uri: Uri) {
        viewModelScope.launch {
            appPreferences.setDebugLogFolderUri(uri.toString())
            appPreferences.setDebugLoggingEnabled(true)
        }
    }
}
