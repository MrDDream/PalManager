package com.paladmin.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ne contient aucun secret : mot de passe admin Palworld, token PalDefender et mot de passe SFTP
 * sont stockés séparément dans [com.paladmin.data.local.security.CredentialStore]
 * (EncryptedSharedPreferences). L'empreinte de clé hôte SSH n'est pas un secret (c'est une donnée
 * publique par nature) — elle est conservée ici pour l'épinglage "confiance au premier usage".
 */
@Entity(tableName = "server_profiles")
data class ServerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val host: String,
    val palworldPort: Int,
    val palworldUseHttps: Boolean,
    val palworldUsername: String,
    val palworldTrustAllCerts: Boolean,
    val palDefenderPort: Int,
    val palDefenderUseHttps: Boolean,
    val sftpPort: Int = 22,
    val sftpUsername: String? = null,
    val sftpPalDefenderLogPath: String? = null,
    val sftpUe4ssLogPath: String? = null,
    val sftpPalTemplatesPath: String? = null,
    val sftpHostKeyFingerprint: String? = null,
)
