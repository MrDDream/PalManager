package com.paladmin.ui.livemap

/**
 * Bornes réelles du monde jouable (unités .sav Unreal Engine), reprises telles quelles du projet
 * PalSite (src/lib/palworld-map-coords.ts), lui-même calibré sur world-map-v2.webp
 * (RNZ01/palworld-server-dashboard). Le monde n'est pas centré sur (0,0) — une hypothèse
 * symétrique ±N décale la plupart des joueurs vers un bord au lieu de les répartir correctement.
 */
private const val WORLD_MIN_X = -1099400.0
private const val WORLD_MAX_X = 349400.0
private const val WORLD_MIN_Y = -724400.0
private const val WORLD_MAX_Y = 724400.0

/**
 * L'Arbre-Monde est une instance séparée avec son propre repère — un joueur là-bas a des
 * coordonnées hors des bornes WORLD_* ci-dessus (X juste au-delà de WORLD_MAX_X), d'où l'intérêt
 * de isOnWorldTree() : sans ça, ces joueurs se retrouvaient épinglés au bord de la carte du monde.
 */
private const val TREE_MIN_X = 347351.5
private const val TREE_MAX_X = 689148.5
private const val TREE_MIN_Y = -818197.0
private const val TREE_MAX_Y = -476400.0

fun isOnWorldTree(worldX: Double, worldY: Double): Boolean =
    worldX in TREE_MIN_X..TREE_MAX_X && worldY in TREE_MIN_Y..TREE_MAX_Y

private fun normalize(value: Double, min: Double, max: Double): Double {
    val percent = ((value - min) / (max - min)) * 100
    return percent.coerceIn(0.0, 100.0)
}

/**
 * L'UI carte de Palworld échange les axes entre le repère .sav et l'image : l'horizontal écran
 * suit Y-monde, le vertical écran suit X-monde (inversé). Vérifié par PalSite contre ~15 points
 * de téléportation réels (boss, îles, arbre-monde) — non inversé, ils tombent hors de la zone
 * plausible.
 */
fun worldToScreenPercent(worldX: Double, worldY: Double): Pair<Float, Float> {
    val left = normalize(worldY, WORLD_MIN_Y, WORLD_MAX_Y)
    val top = 100 - normalize(worldX, WORLD_MIN_X, WORLD_MAX_X)
    return left.toFloat() to top.toFloat()
}

/** Même transformation que worldToScreenPercent, sur les bornes TREE_MIN/MAX — uniquement pertinent si isOnWorldTree() est vrai. */
fun worldTreeToScreenPercent(worldX: Double, worldY: Double): Pair<Float, Float> {
    val left = normalize(worldY, TREE_MIN_Y, TREE_MAX_Y)
    val top = 100 - normalize(worldX, TREE_MIN_X, TREE_MAX_X)
    return left.toFloat() to top.toFloat()
}
