package com.paladmin.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Mots de passe/tokens par profil, chiffrés au repos — jamais stockés dans Room en clair. */
@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "paladmin_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getPalworldPassword(profileId: Long): String = prefs.getString(palworldKey(profileId), "") ?: ""

    fun setPalworldPassword(profileId: Long, password: String) {
        prefs.edit().putString(palworldKey(profileId), password).apply()
    }

    fun getPalDefenderToken(profileId: Long): String = prefs.getString(palDefenderKey(profileId), "") ?: ""

    fun setPalDefenderToken(profileId: Long, token: String) {
        prefs.edit().putString(palDefenderKey(profileId), token).apply()
    }

    fun clear(profileId: Long) {
        prefs.edit().remove(palworldKey(profileId)).remove(palDefenderKey(profileId)).apply()
    }

    private fun palworldKey(profileId: Long) = "palworld_password_$profileId"
    private fun palDefenderKey(profileId: Long) = "paldefender_token_$profileId"
}
