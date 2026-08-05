package com.paladmin.data.repository

import com.paladmin.data.local.dataset.DatasetLoader
import com.paladmin.data.local.dataset.SearchNormalizer
import com.paladmin.data.local.db.HumanDao
import com.paladmin.data.local.db.HumanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HumanRepository @Inject constructor(
    private val humanDao: HumanDao,
    private val datasetLoader: DatasetLoader,
) {
    fun search(query: String): Flow<List<HumanEntity>> = flow {
        datasetLoader.ensureLoaded()
        emitAll(humanDao.search(SearchNormalizer.normalize(query)))
    }

    suspend fun getById(id: String): HumanEntity? {
        datasetLoader.ensureLoaded()
        return humanDao.getById(id)
    }
}
