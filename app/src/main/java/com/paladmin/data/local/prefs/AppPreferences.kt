package com.paladmin.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_prefs")
private val LAST_PROFILE_ID = longPreferencesKey("last_profile_id")
private val DATASET_VERSION = intPreferencesKey("dataset_version")
private val THEME_MODE = stringPreferencesKey("theme_mode")
private val LANGUAGE = stringPreferencesKey("language")
private val DEBUG_LOGGING_ENABLED = booleanPreferencesKey("debug_logging_enabled")
private val DEBUG_LOG_FOLDER_URI = stringPreferencesKey("debug_log_folder_uri")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val code: String) { FRENCH("fr"), ENGLISH("en") }

/** Lecture synchrone utilisée uniquement dans Activity.attachBaseContext(), appelé avant que Hilt
 * n'ait injecté quoi que ce soit — pas d'autre point d'entrée disponible à ce stade du cycle de vie. */
fun readStoredLanguageBlocking(context: Context): AppLanguage = runBlocking {
    AppLanguage.entries.find { it.code == context.dataStore.data.first()[LANGUAGE] } ?: AppLanguage.FRENCH
}

/** Mémorise le dernier serveur ouvert pour y revenir directement au lancement de l'app. */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val lastProfileId: Flow<Long?> = context.dataStore.data.map { it[LAST_PROFILE_ID] }

    suspend fun setLastProfileId(id: Long) {
        context.dataStore.edit { it[LAST_PROFILE_ID] = id }
    }

    suspend fun getDatasetVersion(): Int = context.dataStore.data.first()[DATASET_VERSION] ?: 0

    suspend fun setDatasetVersion(version: Int) {
        context.dataStore.edit { it[DATASET_VERSION] = version }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.entries.find { it.code == prefs[LANGUAGE] } ?: AppLanguage.FRENCH
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[LANGUAGE] = language.code }
    }

    /** Écriture d'un fichier de log dans le dossier [debugLogFolderUri] (SAF), pour le diagnostic. */
    val debugLoggingEnabled: Flow<Boolean> = context.dataStore.data.map { it[DEBUG_LOGGING_ENABLED] ?: false }

    suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DEBUG_LOGGING_ENABLED] = enabled }
    }

    /** URI (arbre SAF, permission persistée par l'appelant) du dossier choisi pour le fichier de log. */
    val debugLogFolderUri: Flow<String?> = context.dataStore.data.map { it[DEBUG_LOG_FOLDER_URI] }

    suspend fun setDebugLogFolderUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(DEBUG_LOG_FOLDER_URI) else prefs[DEBUG_LOG_FOLDER_URI] = uri
        }
    }
}
