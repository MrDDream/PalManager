package com.paladmin.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ne contient aucun secret : mot de passe admin Palworld et token PalDefender sont stockés
 * séparément dans [com.paladmin.data.local.security.CredentialStore] (EncryptedSharedPreferences).
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
)
