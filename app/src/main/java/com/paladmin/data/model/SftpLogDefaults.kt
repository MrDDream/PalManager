package com.paladmin.data.model

/**
 * Chemins de log par défaut (relatifs à la racine d'install du serveur), utilisés en repli quand
 * le champ dédié du profil est vide. Vérifiés via la documentation UE4SS/PalDefender — pas de
 * défaut pour Palworld lui-même : contrairement à une idée reçue ("Pal/Saved/Logs/PalServer.log"),
 * le serveur dédié n'écrit aucun fichier de log par défaut, sur aucune plateforme.
 */
object SftpLogDefaults {
    const val PALDEFENDER_LOG_PATH = "Pal/Binaries/Win64/PalDefender/Logs"
    const val UE4SS_LOG_PATH = "Pal/Binaries/Win64/ue4ss/UE4SS.log"

    /** Modèles de Pal PalDefender (un fichier JSON par modèle, nom de fichier = identifiant utilisé
     * par l'API give/paltemplate) — chemin fixe de la convention d'install PalDefender, pas
     * configurable dans le profil (contrairement aux logs, ce n'est pas un fichier à choisir). */
    const val PAL_TEMPLATES_PATH = "Pal/Binaries/Win64/PalDefender/Templates"
}
