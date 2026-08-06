package com.paladmin.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {

    @Query("SELECT * FROM server_profiles ORDER BY name")
    fun observeAll(): Flow<List<ServerProfileEntity>>

    @Query("SELECT * FROM server_profiles WHERE id = :id")
    suspend fun getById(id: Long): ServerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ServerProfileEntity): Long

    @Update
    suspend fun update(entity: ServerProfileEntity)

    /** Épinglage "confiance au premier usage" de la clé hôte SSH — mis à jour indépendamment du
     * reste du profil (pas de repassage par CredentialStore, ce n'est pas un secret). */
    @Query("UPDATE server_profiles SET sftpHostKeyFingerprint = :fingerprint WHERE id = :id")
    suspend fun updateSftpHostKeyFingerprint(id: Long, fingerprint: String?)

    @Delete
    suspend fun delete(entity: ServerProfileEntity)
}
