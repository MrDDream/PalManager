package com.paladmin.data.repository

import com.paladmin.data.local.dataset.DatasetLoader
import com.paladmin.data.local.dataset.SearchNormalizer
import com.paladmin.data.local.db.PalDao
import com.paladmin.data.local.db.PalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalRepository @Inject constructor(
    private val palDao: PalDao,
    private val datasetLoader: DatasetLoader,
) {
    fun search(query: String, element: String? = null, minRarity: Int? = null, maxRarity: Int? = null): Flow<List<PalEntity>> = flow {
        datasetLoader.ensureLoaded()
        emitAll(palDao.search(SearchNormalizer.normalize(query), element, minRarity, maxRarity))
    }

    suspend fun getById(id: String): PalEntity? = palDao.getById(id)
}
