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

/** Table de correspondance id de compétence passive de Pal (PalDefender Passives) -> nom FR/EN,
 * chargée une fois depuis assets/data/passives.json (pas besoin de Room) — même pattern que
 * TechnologyCatalog/ActiveSkillCatalog. */
@Singleton
class PassiveSkillCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: Map<String, PassiveDatasetEntry>? = null

    suspend fun get(id: String): PassiveDatasetEntry? = byId()[id]

    private suspend fun byId(): Map<String, PassiveDatasetEntry> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                val text = context.assets.open("data/passives.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<PassiveDatasetEntry>>(text).associateBy { it.id }.also { cache = it }
            }
        }
    }
}
