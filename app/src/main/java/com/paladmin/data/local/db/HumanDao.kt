package com.paladmin.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HumanDao {

    @Query("SELECT COUNT(*) FROM humans")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(humans: List<HumanEntity>)

    @Query("DELETE FROM humans")
    suspend fun deleteAll()

    // GROUP BY nameFr : de nombreux PNJ (ex. "Habitant de l'Île" ×92) partagent un nom générique
    // pour des dizaines de variantes cosmétiques distinctes en jeu (même page wiki paldb.cc) — on
    // n'en garde qu'un représentant par nom pour éviter une liste saturée de doublons visuels.
    @Query(
        """
        SELECT * FROM humans
        WHERE (:query = '' OR searchText LIKE '%' || :query || '%')
        GROUP BY nameFr
        ORDER BY nameFr
        LIMIT 500
        """,
    )
    fun search(query: String): Flow<List<HumanEntity>>
}
