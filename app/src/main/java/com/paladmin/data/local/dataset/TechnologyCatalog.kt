package com.paladmin.data.local.dataset

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Table de correspondance id technologie (PalDefender Techs.Unlocked) -> niveau/nom/icône, chargée
 * une fois depuis assets/data/technologies.json (pas besoin de Room, lu une seule fois par session). */
@Singleton
class TechnologyCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: Map<String, TechnologyDatasetEntry>? = null

    suspend fun get(id: String): TechnologyDatasetEntry? = byId()[id]

    private suspend fun byId(): Map<String, TechnologyDatasetEntry> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                val text = context.assets.open("data/technologies.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<TechnologyDatasetEntry>>(text).associateBy { it.id }.also { cache = it }
            }
        }
    }
}
