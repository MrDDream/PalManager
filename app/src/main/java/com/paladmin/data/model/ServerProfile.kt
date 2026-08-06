package com.paladmin.data.model

data class ServerProfile(
    val id: Long = 0,
    val name: String,
    val iconKey: String = "dns",
    /** Hôte partagé par les deux API : sur un même serveur, IP Palworld et PalDefender sont toujours identiques. */
    val host: String,
    val palworldPort: Int = 8212,
    val palworldPassword: String = "",
    val palDefenderPort: Int = 17993,
    val palDefenderToken: String = "",
    /** Menu Logs (Palworld/PalDefender/UE4SS) — tout optionnel, "" = pas configuré. Toujours sur
     * [host] : SFTP et API sont sur la même machine dans l'immense majorité des cas, pas de champ
     * séparé à saisir. */
    val sftpPort: Int = 22,
    val sftpUsername: String = "",
    val sftpPassword: String = "",
    val sftpPalDefenderLogPath: String = "",
    val sftpUe4ssLogPath: String = "",
    val sftpPalTemplatesPath: String = "",
    /** Empreinte de clé hôte SSH épinglée à la première connexion (confiance au premier usage) —
     * pas un secret, sert juste à détecter un changement suspect côté serveur. */
    val sftpHostKeyFingerprint: String? = null,
) {
    val isSftpConfigured: Boolean get() = sftpUsername.isNotBlank() && sftpPassword.isNotBlank()
}
