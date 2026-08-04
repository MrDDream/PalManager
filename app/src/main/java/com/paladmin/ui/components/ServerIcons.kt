package com.paladmin.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

/** Icônes sélectionnables pour distinguer visuellement plusieurs profils serveur. */
object ServerIcons {
    val options: List<Pair<String, ImageVector>> = listOf(
        "dns" to Icons.Filled.Dns,
        "storage" to Icons.Filled.Storage,
        "cloud" to Icons.Filled.Cloud,
        "public" to Icons.Filled.Public,
        "terminal" to Icons.Filled.Terminal,
        "games" to Icons.Filled.Games,
        "shield" to Icons.Filled.Shield,
        "star" to Icons.Filled.Star,
        "forest" to Icons.Filled.Forest,
    )

    private val byKey = options.toMap()

    fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Filled.Dns
}
