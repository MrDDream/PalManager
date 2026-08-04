package com.paladmin.data.local.dataset

import android.content.Context
import com.paladmin.data.local.db.HumanDao
import com.paladmin.data.local.db.HumanEntity
import com.paladmin.data.local.db.ItemDao
import com.paladmin.data.local.db.ItemEntity
import com.paladmin.data.local.db.PalDao
import com.paladmin.data.local.db.PalEntity
import com.paladmin.data.local.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Importe le dataset embarqué (assets/data, items/pals/humans.json) dans Room. Le contenu de ces
 * fichiers évolue (filtrage, nouveaux champs...) sans forcément toucher au schéma Room —
 * [CURRENT_DATASET_VERSION] force donc un ré-import complet indépendamment de tout changement de
 * schéma, dès que ces fichiers changent.
 */
@Singleton
class DatasetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemDao: ItemDao,
    private val palDao: PalDao,
    private val humanDao: HumanDao,
    private val appPreferences: AppPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureLoaded() {
        val needsReload = appPreferences.getDatasetVersion() < CURRENT_DATASET_VERSION
        if (needsReload) {
            itemDao.deleteAll()
            palDao.deleteAll()
            humanDao.deleteAll()
        }

        if (needsReload || itemDao.count() == 0) {
            val entries = readAsset<List<ItemDatasetEntry>>("data/items.json")
            itemDao.insertAll(
                entries.map { entry ->
                    // paldb.cc n'a pas de nom FR pour certains items récents : on retombe sur
                    // l'anglais plutôt que d'afficher l'ID technique brut, et ça évite aussi que
                    // ces entrées se retrouvent toutes groupées en tête de liste (tri par nameFr,
                    // une chaîne vide passe avant tout nom réel).
                    val displayNameFr = entry.nameFr.ifBlank { entry.nameEn }
                    ItemEntity(
                        id = entry.id,
                        nameFr = displayNameFr,
                        nameEn = entry.nameEn,
                        category = entry.category,
                        image = entry.image,
                        searchText = SearchNormalizer.normalize("${entry.id} ${entry.nameFr} ${entry.nameEn}"),
                        description = entry.description,
                        statsJson = json.encodeToString(entry.stats),
                    )
                },
            )
        }
        if (needsReload || palDao.count() == 0) {
            val entries = readAsset<List<PalDatasetEntry>>("data/pals.json")
            palDao.insertAll(
                entries.map { entry ->
                    PalEntity(
                        id = entry.id,
                        nameFr = entry.nameFr.ifBlank { entry.nameEn },
                        nameEn = entry.nameEn,
                        image = entry.image,
                        searchText = SearchNormalizer.normalize("${entry.id} ${entry.nameFr} ${entry.nameEn}"),
                        element1 = entry.element1,
                        element2 = entry.element2,
                        rarity = entry.rarity,
                        zukanIndex = entry.zukanIndex,
                        statsJson = json.encodeToString(entry.stats),
                        workSuitabilitiesJson = json.encodeToString(entry.workSuitabilities),
                        partnerSkillJson = json.encodeToString(entry.partnerSkill),
                        locationsJson = json.encodeToString(entry.locations),
                        mapPositionJson = json.encodeToString(entry.mapPosition),
                    )
                },
            )
        }
        if (needsReload || humanDao.count() == 0) {
            val entries = readAsset<List<HumanDatasetEntry>>("data/humans.json")
            humanDao.insertAll(
                entries.map { entry ->
                    HumanEntity(
                        id = entry.id,
                        nameFr = entry.nameFr.ifBlank { entry.nameEn },
                        nameEn = entry.nameEn,
                        image = entry.image,
                        searchText = SearchNormalizer.normalize("${entry.id} ${entry.nameFr} ${entry.nameEn}"),
                        statsJson = json.encodeToString(entry.stats),
                        workSuitabilitiesJson = json.encodeToString(entry.workSuitabilities),
                        dropsJson = json.encodeToString(entry.drops),
                    )
                },
            )
        }

        if (needsReload) {
            appPreferences.setDatasetVersion(CURRENT_DATASET_VERSION)
        }
    }

    private inline fun <reified T> readAsset(path: String): T {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }

    private companion object {
        // À incrémenter à chaque changement de contenu de items/pals/humans.json (filtrage,
        // nouveaux champs...), même sans changement du schéma Room — sinon les installations
        // existantes gardent l'ancien contenu indéfiniment (ensureLoaded ne réimporte que si vide).
        const val CURRENT_DATASET_VERSION = 6
    }
}
