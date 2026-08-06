package com.paladmin.data.sftp

/** Chemin du dossier parent d'un chemin SFTP absolu — "/" si déjà à la racine (ou chemin non
 * résolu, ex. "." avant la première navigation). */
fun sftpParentPath(path: String): String {
    val trimmed = path.trimEnd('/')
    val idx = trimmed.lastIndexOf('/')
    return if (idx <= 0) "/" else trimmed.substring(0, idx)
}

/** Joint un nom de fichier/dossier à un chemin de dossier, sans double slash ni cas particulier
 * pour "." (dossier de connexion initial, avant résolution en chemin absolu). */
fun sftpJoin(directory: String, name: String): String {
    val trimmed = directory.trimEnd('/')
    return if (trimmed.isEmpty() || trimmed == ".") name else "$trimmed/$name"
}
