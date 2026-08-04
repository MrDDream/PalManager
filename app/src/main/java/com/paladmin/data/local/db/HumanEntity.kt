package com.paladmin.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** PNJ humains (paldb.cc/fr/Humans) — donnés au joueur via le même give/pals PalDefender que les Pals. */
@Entity(tableName = "humans", indices = [Index("searchText")])
data class HumanEntity(
    @PrimaryKey val id: String,
    val nameFr: String,
    val nameEn: String,
    val image: String,
    val searchText: String,
    val statsJson: String = "null",
    val workSuitabilitiesJson: String = "[]",
    val dropsJson: String = "[]",
)
