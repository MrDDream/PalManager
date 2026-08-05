package com.paladmin.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PalDao {

    @Query("SELECT COUNT(*) FROM pals")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pals: List<PalEntity>)

    @Query("DELETE FROM pals")
    suspend fun deleteAll()

    @Query(
        """
        SELECT * FROM pals
        WHERE (:query = '' OR searchText LIKE '%' || :query || '%')
          AND (:element IS NULL OR element1 = :element OR element2 = :element)
          AND (:minRarity IS NULL OR rarity >= :minRarity)
          AND (:maxRarity IS NULL OR rarity <= :maxRarity)
        ORDER BY nameFr
        LIMIT 200
        """,
    )
    fun search(query: String, element: String?, minRarity: Int?, maxRarity: Int?): Flow<List<PalEntity>>

    @Query("SELECT * FROM pals WHERE id = :id COLLATE NOCASE")
    suspend fun getById(id: String): PalEntity?
}
