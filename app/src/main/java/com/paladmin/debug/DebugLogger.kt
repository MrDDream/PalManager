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

    // findFile() ne retrouve pas toujours de façon fiable, sur certains fournisseurs SAF, un fichier
    // tout juste créé par createFile() dans la même session — chaque ligne de log qui ratait cette
    // recherche déclenchait un nouveau createFile(), donc un nouveau fichier à chaque ligne (une
    // requête HTTP en génère une dizaine). On résout le fichier une seule fois et on garde sa
    // référence exacte pour tous les appends suivants, sans jamais retenter findFile().
    @Volatile private var resolvedLogFile: DocumentFile? = null
    @Volatile private var resolvedForFolderUri: Uri? = null

    init {
        appPreferences.debugLoggingEnabled.onEach { enabled = it }.launchIn(scope)
        appPreferences.debugLogFolderUri.onEach {
            folderUri = it?.let(Uri::parse)
            resolvedLogFile = null
            resolvedForFolderUri = null
        }.launchIn(scope)
    }

    fun log(message: String) {
        if (!enabled) return
        val treeUri = folderUri ?: return
        scope.launch { runCatching { appendLine(treeUri, message) } }
    }

    private fun appendLine(treeUri: Uri, message: String) {
        val file = resolveLogFile(treeUri) ?: return
        val line = "[${timestampFormat.format(Date())}] $message\n"
        context.contentResolver.openOutputStream(file.uri, "wa")?.use { it.write(line.toByteArray()) }
    }

    private fun resolveLogFile(treeUri: Uri): DocumentFile? {
        resolvedLogFile?.takeIf { resolvedForFolderUri == treeUri }?.let { return it }
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val file = tree.findFile(LOG_FILE_NAME) ?: tree.createFile("text/plain", LOG_FILE_NAME) ?: return null
        resolvedLogFile = file
        resolvedForFolderUri = treeUri
        return file
    }
}
