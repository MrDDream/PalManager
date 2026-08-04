package com.paladmin.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ServerProfileEntity::class, ItemEntity::class, PalEntity::class, HumanEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverProfileDao(): ServerProfileDao
    abstract fun itemDao(): ItemDao
    abstract fun palDao(): PalDao
    abstract fun humanDao(): HumanDao
}
