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

/** Table de correspondance id de compétence active de Pal (EPalWazaID, PalDefender ActiveSkills/
 * LearntSkills) -> nom FR/EN, chargée une fois depuis assets/data/active_skills.json (pas besoin de
 * Room, lu une seule fois par session) — même pattern que TechnologyCatalog. */
@Singleton
class ActiveSkillCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: Map<String, ActiveSkillDatasetEntry>? = null

    suspend fun get(id: String): ActiveSkillDatasetEntry? = byId()[id]

    private suspend fun byId(): Map<String, ActiveSkillDatasetEntry> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                val text = context.assets.open("data/active_skills.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<ActiveSkillDatasetEntry>>(text).associateBy { it.id }.also { cache = it }
            }
        }
    }
}
