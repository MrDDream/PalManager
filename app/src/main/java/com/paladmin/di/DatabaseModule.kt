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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "paladmin.db")
            .addMigrations(MIGRATION_1_2)
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
