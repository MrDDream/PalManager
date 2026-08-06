package com.paladmin.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.paladmin.data.local.db.AppDatabase
import com.paladmin.data.local.db.HumanDao
import com.paladmin.data.local.db.ItemDao
import com.paladmin.data.local.db.PalDao
import com.paladmin.data.local.db.ServerProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ajout du toggle HTTP/HTTPS Palworld (certains hébergeurs exposent l'API en clair).
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN palworldUseHttps INTEGER NOT NULL DEFAULT 1")
    }
}

/** Explicite (pas de fallback destructif) : v5 est la version réellement en production (v0.1.1),
 * une migration destructive effacerait les profils serveur des utilisateurs existants en même
 * temps que le dataset items/pals/humains (contrairement à ce dernier, pas reconstructible). */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Config SFTP du menu Logs (Palworld/PalDefender/UE4SS) : port/utilisateur (même hôte que
        // le reste du profil) et chemins par outil, tous optionnels. Le mot de passe SFTP va dans
        // CredentialStore comme les autres secrets ; l'empreinte de clé hôte SSH n'est pas un
        // secret (épinglage confiance-au-1er-usage).
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpPort INTEGER NOT NULL DEFAULT 22")
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpUsername TEXT")
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpPalworldLogPath TEXT")
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpPalDefenderLogPath TEXT")
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpUe4ssLogPath TEXT")
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpHostKeyFingerprint TEXT")
    }
}

/** Un champ hôte SFTP séparé a existé brièvement en v6 avant d'être retiré (l'API et le SFTP sont
 * toujours sur la même machine — pas de champ dédié utile) : les tout premiers testeurs peuvent
 * avoir une table v6 qui l'a encore. Recrée la table plutôt que ALTER TABLE DROP COLUMN, pas
 * garanti disponible sur toutes les versions de SQLite embarquées (Android 8+ = SQLite ≥3.35 non
 * garanti, DROP COLUMN datant de 3.35). SELECT explicite des colonnes voulues : fonctionne que la
 * table source ait ou non la colonne sftpHost. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE server_profiles_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                iconKey TEXT NOT NULL,
                host TEXT NOT NULL,
                palworldPort INTEGER NOT NULL,
                palworldUseHttps INTEGER NOT NULL,
                palworldUsername TEXT NOT NULL,
                palworldTrustAllCerts INTEGER NOT NULL,
                palDefenderPort INTEGER NOT NULL,
                palDefenderUseHttps INTEGER NOT NULL,
                sftpPort INTEGER NOT NULL,
                sftpUsername TEXT,
                sftpPalworldLogPath TEXT,
                sftpPalDefenderLogPath TEXT,
                sftpUe4ssLogPath TEXT,
                sftpHostKeyFingerprint TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO server_profiles_new (id, name, iconKey, host, palworldPort, palworldUseHttps,
                palworldUsername, palworldTrustAllCerts, palDefenderPort, palDefenderUseHttps,
                sftpPort, sftpUsername, sftpPalworldLogPath, sftpPalDefenderLogPath, sftpUe4ssLogPath,
                sftpHostKeyFingerprint)
            SELECT id, name, iconKey, host, palworldPort, palworldUseHttps,
                palworldUsername, palworldTrustAllCerts, palDefenderPort, palDefenderUseHttps,
                sftpPort, sftpUsername, sftpPalworldLogPath, sftpPalDefenderLogPath, sftpUe4ssLogPath,
                sftpHostKeyFingerprint
            FROM server_profiles
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE server_profiles")
        db.execSQL("ALTER TABLE server_profiles_new RENAME TO server_profiles")
    }
}

/** L'onglet Logs "Palworld" a été retiré (le serveur dédié n'écrit aucun log par défaut — le champ
 * n'était donc quasi jamais utile) : ne garder que PalDefender/UE4SS. Même technique de recréation
 * de table qu'en v6→v7 (portable, pas de dépendance à ALTER TABLE DROP COLUMN). */
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE server_profiles_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                iconKey TEXT NOT NULL,
                host TEXT NOT NULL,
                palworldPort INTEGER NOT NULL,
                palworldUseHttps INTEGER NOT NULL,
                palworldUsername TEXT NOT NULL,
                palworldTrustAllCerts INTEGER NOT NULL,
                palDefenderPort INTEGER NOT NULL,
                palDefenderUseHttps INTEGER NOT NULL,
                sftpPort INTEGER NOT NULL,
                sftpUsername TEXT,
                sftpPalDefenderLogPath TEXT,
                sftpUe4ssLogPath TEXT,
                sftpHostKeyFingerprint TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO server_profiles_new (id, name, iconKey, host, palworldPort, palworldUseHttps,
                palworldUsername, palworldTrustAllCerts, palDefenderPort, palDefenderUseHttps,
                sftpPort, sftpUsername, sftpPalDefenderLogPath, sftpUe4ssLogPath, sftpHostKeyFingerprint)
            SELECT id, name, iconKey, host, palworldPort, palworldUseHttps,
                palworldUsername, palworldTrustAllCerts, palDefenderPort, palDefenderUseHttps,
                sftpPort, sftpUsername, sftpPalDefenderLogPath, sftpUe4ssLogPath, sftpHostKeyFingerprint
            FROM server_profiles
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE server_profiles")
        db.execSQL("ALTER TABLE server_profiles_new RENAME TO server_profiles")
    }
}

/** Chemin des templates Pal PalDefender configurable (la convention d'install présumée ne
 * correspondait pas à tous les serveurs) — même pattern que les chemins de log. */
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE server_profiles ADD COLUMN sftpPalTemplatesPath TEXT")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "paladmin.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            // Room interdit fallbackToDestructiveMigrationFrom(2) tant qu'une Migration se
            // termine déjà à la version 2 (MIGRATION_1_2) — conflit détecté à la construction,
            // qui plantait donc à CHAQUE lancement, migration réellement nécessaire ou non.
            // La variante sans argument couvre tout saut de version non couvert par une
            // Migration explicite (ex: v2->v3), sans ce conflit ; items/pals sont de toute façon
            // 100% reconstructibles depuis les assets embarqués (DatasetLoader) après un wipe.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideServerProfileDao(database: AppDatabase): ServerProfileDao = database.serverProfileDao()

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    fun providePalDao(database: AppDatabase): PalDao = database.palDao()

    @Provides
    fun provideHumanDao(database: AppDatabase): HumanDao = database.humanDao()
}
