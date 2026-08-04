package com.paladmin.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [searchText] est une version normalisée (minuscules, accents supprimés) de id+nameFr+nameEn,
 * calculée une fois à l'import pour permettre une recherche insensible aux accents avec un simple
 * LIKE — largement suffisant à l'échelle de quelques milliers d'items, pas besoin de FTS.
 */
@Entity(tableName = "items", indices = [Index("searchText"), Index("category")])
data class ItemEntity(
    @PrimaryKey val id: String,
    val nameFr: String,
    val nameEn: String,
    val category: String,
    val image: String,
    val searchText: String,
    val description: String = "",
    /** Stats clé/valeur (ex: "Attaque" -> "320") sérialisées en JSON — pas de table dédiée, lues à l'affichage seulement. */
    val statsJson: String = "{}",
)
