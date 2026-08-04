package com.paladmin.data.repository

import com.paladmin.data.local.dataset.DatasetLoader
import com.paladmin.data.local.dataset.SearchNormalizer
import com.paladmin.data.local.db.ItemDao
import com.paladmin.data.local.db.ItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val datasetLoader: DatasetLoader,
) {
    fun search(query: String, category: String?): Flow<List<ItemEntity>> = flow {
        datasetLoader.ensureLoaded()
        emitAll(itemDao.search(SearchNormalizer.normalize(query), category))
    }

    fun observeCategories(): Flow<List<String>> = itemDao.observeCategories()

    suspend fun getById(id: String): ItemEntity? = itemDao.getById(id)
}
