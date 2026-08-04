package com.paladmin.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pals", indices = [Index("searchText"), Index("element1"), Index("rarity")])
data class PalEntity(
    @PrimaryKey val id: String,
    val nameFr: String,
    val nameEn: String,
    val image: String,
    val searchText: String,
    val element1: String = "Normal",
    val element2: String? = null,
    val rarity: Int = 1,
    val zukanIndex: Int = -1,
    /** Stats/métiers/compétence/localisation sérialisés en JSON — lus seulement à l'affichage du détail. */
    val statsJson: String = "null",
    val workSuitabilitiesJson: String = "[]",
    val partnerSkillJson: String = "null",
    val locationsJson: String = "[]",
    val mapPositionJson: String = "null",
)
