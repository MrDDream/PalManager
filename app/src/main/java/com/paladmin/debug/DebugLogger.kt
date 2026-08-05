package com.paladmin.debug

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.paladmin.data.local.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_FILE_NAME = "palmanager-debug.log"

/**
 * Écrit les lignes de log réseau dans un fichier texte du dossier choisi par l'utilisateur
 * (Storage Access Framework), pour permettre le diagnostic sans accès adb/logcat.
 * Toutes les écritures sont sérialisées sur un seul thread pour éviter d'entrelacer des lignes,
 * et toute erreur est avalée : le logging ne doit jamais faire planter l'app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DebugLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    appPreferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)

    @Volatile private var enabled = false

    @Volatile private var folderUri: Uri? = null

    init {
        appPreferences.debugLoggingEnabled.onEach { enabled = it }.launchIn(scope)
        appPreferences.debugLogFolderUri.onEach { folderUri = it?.let(Uri::parse) }.launchIn(scope)
    }

    fun log(message: String) {
        if (!enabled) return
        val treeUri = folderUri ?: return
        scope.launch { runCatching { appendLine(treeUri, message) } }
    }

    private fun appendLine(treeUri: Uri, message: String) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val file = tree.findFile(LOG_FILE_NAME) ?: tree.createFile("text/plain", LOG_FILE_NAME) ?: return
        val line = "[${timestampFormat.format(Date())}] $message\n"
        context.contentResolver.openOutputStream(file.uri, "wa")?.use { it.write(line.toByteArray()) }
    }
}
