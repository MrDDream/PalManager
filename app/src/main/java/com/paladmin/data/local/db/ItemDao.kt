package com.paladmin.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query(
        """
        SELECT * FROM items
        WHERE (:category IS NULL OR category = :category)
          AND (:query = '' OR searchText LIKE '%' || :query || '%')
        ORDER BY nameFr
        LIMIT 200
        """,
    )
    fun search(query: String, category: String?): Flow<List<ItemEntity>>

    @Query("SELECT DISTINCT category FROM items ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?
}
