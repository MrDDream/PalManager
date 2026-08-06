package com.paladmin.data.repository

import com.paladmin.data.local.db.ServerProfileDao
import com.paladmin.data.local.db.ServerProfileEntity
import com.paladmin.data.local.security.CredentialStore
import com.paladmin.data.model.ServerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val dao: ServerProfileDao,
    private val credentialStore: CredentialStore,
) {
    fun observeProfiles(): Flow<List<ServerProfile>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain(credentialStore) }
    }

    suspend fun getProfile(id: Long): ServerProfile? = dao.getById(id)?.toDomain(credentialStore)

    /** Insère ou met à jour le profil ; les secrets sont écrits après résolution de l'id généré. */
    suspend fun saveProfile(profile: ServerProfile): Long {
        val entity = profile.toEntity()
        val id = dao.upsert(entity)
        credentialStore.setPalworldPassword(id, profile.palworldPassword)
        credentialStore.setPalDefenderToken(id, profile.palDefenderToken)
        credentialStore.setSftpPassword(id, profile.sftpPassword)
        return id
    }

    suspend fun deleteProfile(profile: ServerProfile) {
        dao.delete(profile.toEntity())
        credentialStore.clear(profile.id)
    }

    /** Épinglage "confiance au premier usage" de la clé hôte SSH, indépendant du reste du profil
     * (appelé depuis l'écran Logs à la connexion, pas depuis le formulaire d'édition). */
    suspend fun updateSftpHostKeyFingerprint(profileId: Long, fingerprint: String?) {
        dao.updateSftpHostKeyFingerprint(profileId, fingerprint)
    }

    private fun ServerProfileEntity.toDomain(store: CredentialStore) = ServerProfile(
        id = id,
        name = name,
        iconKey = iconKey,
        host = host,
        palworldPort = palworldPort,
        palworldPassword = store.getPalworldPassword(id),
        palDefenderPort = palDefenderPort,
        palDefenderToken = store.getPalDefenderToken(id),
        sftpPort = sftpPort,
        sftpUsername = sftpUsername.orEmpty(),
        sftpPassword = store.getSftpPassword(id),
        sftpPalDefenderLogPath = sftpPalDefenderLogPath.orEmpty(),
        sftpUe4ssLogPath = sftpUe4ssLogPath.orEmpty(),
        sftpPalTemplatesPath = sftpPalTemplatesPath.orEmpty(),
        sftpHostKeyFingerprint = sftpHostKeyFingerprint,
    )

    private fun ServerProfile.toEntity() = ServerProfileEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        host = host,
        palworldPort = palworldPort,
        // Colonnes conservées en base pour éviter une migration de schéma, mais plus utilisées :
        // le nom d'utilisateur est toujours "admin" et les deux API sont toujours en HTTP simple.
        palworldUsername = "admin",
        palworldUseHttps = false,
        palworldTrustAllCerts = false,
        palDefenderPort = palDefenderPort,
        palDefenderUseHttps = false,
        sftpPort = sftpPort,
        sftpUsername = sftpUsername.ifBlank { null },
        sftpPalDefenderLogPath = sftpPalDefenderLogPath.ifBlank { null },
        sftpUe4ssLogPath = sftpUe4ssLogPath.ifBlank { null },
        sftpPalTemplatesPath = sftpPalTemplatesPath.ifBlank { null },
        sftpHostKeyFingerprint = sftpHostKeyFingerprint,
    )
}
