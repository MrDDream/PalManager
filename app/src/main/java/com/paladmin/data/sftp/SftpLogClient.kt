package com.paladmin.data.sftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.InputStream
import java.security.PublicKey
import java.security.Security
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_LOG_BYTES = 512 * 1024L

/** Android enregistre déjà un provider limité nommé "BC" (sans X25519) — sshj ne remplace pas un
 * provider existant du même nom, donc `X25519` résout vers le stub Android au lieu de la vraie
 * BouncyCastle embarquée par sshj ("no such algorithm: X25519 for provider BC"). On le remplace
 * explicitement par la bonne implémentation avant toute connexion. Lazy + une seule fois par
 * process (mutation globale de la JCE, inutile de la refaire à chaque appel). */
private val ensureBouncyCastleRegistered by lazy {
    val existing = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
    if (existing == null || existing.javaClass != BouncyCastleProvider::class.java) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
    SecurityUtils.setRegisterBouncyCastle(true)
}

sealed interface SftpLogResult {
    data class Success(val content: String, val truncated: Boolean, val hostKeyFingerprint: String) : SftpLogResult
    data class HostKeyMismatch(val expectedFingerprint: String, val actualFingerprint: String) : SftpLogResult
    data class Failure(val message: String) : SftpLogResult
}

data class SftpEntry(val name: String, val path: String, val isDirectory: Boolean)

/** Élément d'une arborescence locale à envoyer via [SftpLogClient.uploadTree] — [open] est différé
 * (pas ouvert tant que l'upload de ce fichier précis n'est pas atteint), un dossier avec beaucoup
 * de fichiers ne doit pas garder des dizaines de flux ouverts simultanément. */
sealed interface LocalTreeEntry {
    val relativePath: String

    data class Directory(override val relativePath: String) : LocalTreeEntry
    data class File(override val relativePath: String, val open: () -> InputStream) : LocalTreeEntry
}

sealed interface SftpBrowseResult {
    data class Success(val entries: List<SftpEntry>) : SftpBrowseResult
    data class Failure(val message: String) : SftpBrowseResult
}

/** Résultat générique des opérations épinglées (écran SFTP dédié — parcourir/télécharger/envoyer/
 * renommer) : mêmes trois cas que [SftpLogResult], factorisés puisqu'il n'y a plus qu'un seul
 * champ variable ([value]) entre les usages. */
sealed interface TofuResult<out T> {
    data class Success<T>(val value: T, val hostKeyFingerprint: String) : TofuResult<T>
    data class HostKeyMismatch(val expectedFingerprint: String, val actualFingerprint: String) : TofuResult<Nothing>
    data class Failure(val message: String) : TofuResult<Nothing>
}

/** Confiance au premier usage : accepte n'importe quelle clé hôte tant qu'aucune empreinte n'est
 * connue, sinon exige une correspondance exacte — la connexion échoue sinon (verify() = false),
 * ce que sshj traduit en exception à la connexion, détectée via [mismatchDetected]. */
private class TofuVerifier(private val knownFingerprint: String?) : HostKeyVerifier {
    var observedFingerprint: String? = null
        private set
    var mismatchDetected: Boolean = false
        private set

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val actual = SecurityUtils.getFingerprint(key)
        observedFingerprint = actual
        if (knownFingerprint == null || knownFingerprint == actual) return true
        mismatchDetected = true
        return false
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
}

/** Récupère un fichier de log par SFTP (mot de passe uniquement, pas de clé privée) — utilisé par
 * l'écran Logs pour Palworld/PalDefender/UE4SS. Aucun état conservé entre deux appels : la
 * persistance de l'empreinte de clé hôte est de la responsabilité de l'appelant. */
@Singleton
class SftpLogClient @Inject constructor() {

    suspend fun fetchLog(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        knownHostKeyFingerprint: String?,
    ): SftpLogResult = withContext(Dispatchers.IO) {
        val verifier = TofuVerifier(knownHostKeyFingerprint?.takeIf { it.isNotBlank() })
        runCatching { readViaSftp(host, port, username, password, remotePath, verifier) }
            .fold(
                onSuccess = { (content, truncated) ->
                    SftpLogResult.Success(content, truncated, verifier.observedFingerprint.orEmpty())
                },
                onFailure = { error ->
                    if (verifier.mismatchDetected) {
                        SftpLogResult.HostKeyMismatch(
                            expectedFingerprint = knownHostKeyFingerprint.orEmpty(),
                            actualFingerprint = verifier.observedFingerprint.orEmpty(),
                        )
                    } else {
                        SftpLogResult.Failure(error.message ?: error.javaClass.simpleName)
                    }
                },
            )
    }

    /** Liste un dossier distant pour le sélecteur de chemin de l'écran Avancées — délibérément
     * sans épinglage TOFU (`PromiscuousVerifier`) : c'est un utilitaire de confort de saisie, pas
     * le chemin de lecture des logs, et le profil n'a souvent pas encore été enregistré (pas d'id
     * pour persister une empreinte). L'épinglage réel reste appliqué par [fetchLog]. */
    suspend fun listDirectory(host: String, port: Int, username: String, password: String, path: String): SftpBrowseResult =
        withContext(Dispatchers.IO) {
            runCatching {
                connect(host, port, username, password, PromiscuousVerifier()).use { ssh ->
                    ssh.newSFTPClient().use { sftp ->
                        sftp.ls(path)
                            .map { SftpEntry(it.name, it.path, it.attributes.type == FileMode.Type.DIRECTORY) }
                            .sortedWith(compareByDescending<SftpEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
                    }
                }
            }.fold(
                onSuccess = { SftpBrowseResult.Success(it) },
                onFailure = { error -> SftpBrowseResult.Failure(error.message ?: error.javaClass.simpleName) },
            )
        }

    /** Version épinglée (TOFU) de [listDirectory], pour l'écran SFTP dédié — le profil y est
     * toujours déjà enregistré (contrairement au sélecteur de chemin d'Avancées), donc l'empreinte
     * peut être persistée normalement comme pour [fetchLog]. */
    suspend fun listDirectorySecure(
        host: String,
        port: Int,
        username: String,
        password: String,
        path: String,
        knownHostKeyFingerprint: String?,
    ): TofuResult<List<SftpEntry>> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        sftp.ls(path)
            .map { SftpEntry(it.name, it.path, it.attributes.type == FileMode.Type.DIRECTORY) }
            .sortedWith(compareByDescending<SftpEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    /** [sink] reçoit le flux distant ouvert — à copier vers sa destination finale (ex. un flux SAF)
     * sans passer par un `ByteArray` intermédiaire, un fichier serveur pouvant être volumineux. */
    suspend fun downloadFile(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        knownHostKeyFingerprint: String?,
        sink: (InputStream) -> Unit,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        sftp.open(remotePath).use { file -> file.RemoteFileInputStream().use { input -> sink(input) } }
    }

    /** Écrit [source] (copié en flux, pas d'un coup) vers [remotePath], créé ou écrasé si présent. */
    suspend fun uploadFile(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        knownHostKeyFingerprint: String?,
        source: InputStream,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        sftp.open(remotePath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).use { file ->
            val buffer = ByteArray(64 * 1024)
            var offset = 0L
            while (true) {
                val n = source.read(buffer)
                if (n <= 0) break
                file.write(offset, buffer, 0, n)
                offset += n
            }
        }
    }

    /** Couper/déplacer, mais aussi Renommer (même opération SFTP) : une seule et même requête de
     * rename, que la destination soit dans le même dossier (renommage) ou un autre (déplacement). */
    suspend fun rename(
        host: String,
        port: Int,
        username: String,
        password: String,
        oldPath: String,
        newPath: String,
        knownHostKeyFingerprint: String?,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp -> sftp.rename(oldPath, newPath) }

    /** Le protocole SFTP n'a pas de commande "copier" — on lit et réécrit en flux (pas en mémoire
     * d'un coup) dans la même session. Fichiers uniquement : une copie récursive de dossier
     * demanderait de parcourir l'arborescence à la main, hors scope de ce sélecteur "basique". */
    suspend fun copyFile(
        host: String,
        port: Int,
        username: String,
        password: String,
        sourcePath: String,
        destPath: String,
        knownHostKeyFingerprint: String?,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        sftp.open(sourcePath).use { source ->
            sftp.open(destPath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).use { dest ->
                val buffer = ByteArray(64 * 1024)
                var offset = 0L
                while (true) {
                    val n = source.read(offset, buffer, 0, buffer.size)
                    if (n <= 0) break
                    dest.write(offset, buffer, 0, n)
                    offset += n
                }
            }
        }
    }

    /** Envoie une arborescence locale entière dans une seule session SSH (une connexion par fichier
     * serait bien trop lent) — [entries] doit lister les dossiers avant leur contenu (parcours en
     * profondeur classique), sinon `mkdir` échouerait sur un parent pas encore créé. `mkdir` d'un
     * dossier déjà présent échoue côté serveur : on vérifie d'abord son existence pour permettre de
     * relancer un envoi partiel sans tout casser. */
    suspend fun uploadTree(
        host: String,
        port: Int,
        username: String,
        password: String,
        baseRemotePath: String,
        entries: List<LocalTreeEntry>,
        knownHostKeyFingerprint: String?,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        entries.forEach { entry ->
            val remotePath = sftpJoin(baseRemotePath, entry.relativePath)
            when (entry) {
                is LocalTreeEntry.Directory -> {
                    if (sftp.statExistence(remotePath) == null) sftp.mkdir(remotePath)
                }
                is LocalTreeEntry.File -> {
                    entry.open().use { source ->
                        sftp.open(remotePath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).use { file ->
                            val buffer = ByteArray(64 * 1024)
                            var offset = 0L
                            while (true) {
                                val n = source.read(buffer)
                                if (n <= 0) break
                                file.write(offset, buffer, 0, n)
                                offset += n
                            }
                        }
                    }
                }
            }
        }
    }

    /** Un dossier ne peut être supprimé que vide (SSH_FXP_RMDIR standard) — l'erreur serveur en cas
     * de dossier non vide remonte telle quelle via [TofuResult.Failure], pas de suppression
     * récursive automatique (destructif, hors scope de ce sélecteur "basique"). */
    suspend fun deleteEntry(
        host: String,
        port: Int,
        username: String,
        password: String,
        path: String,
        isDirectory: Boolean,
        knownHostKeyFingerprint: String?,
    ): TofuResult<Unit> = withTofu(host, port, username, password, knownHostKeyFingerprint) { sftp ->
        if (isDirectory) sftp.rmdir(path) else sftp.rm(path)
    }

    private suspend fun <T> withTofu(
        host: String,
        port: Int,
        username: String,
        password: String,
        knownHostKeyFingerprint: String?,
        block: (SFTPClient) -> T,
    ): TofuResult<T> = withContext(Dispatchers.IO) {
        val verifier = TofuVerifier(knownHostKeyFingerprint?.takeIf { it.isNotBlank() })
        runCatching {
            connect(host, port, username, password, verifier).use { ssh ->
                ssh.newSFTPClient().use { sftp -> block(sftp) }
            }
        }.fold(
            onSuccess = { TofuResult.Success(it, verifier.observedFingerprint.orEmpty()) },
            onFailure = { error ->
                if (verifier.mismatchDetected) {
                    TofuResult.HostKeyMismatch(
                        expectedFingerprint = knownHostKeyFingerprint.orEmpty(),
                        actualFingerprint = verifier.observedFingerprint.orEmpty(),
                    )
                } else {
                    TofuResult.Failure(error.message ?: error.javaClass.simpleName)
                }
            },
        )
    }

    private fun readViaSftp(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        verifier: HostKeyVerifier,
    ): Pair<String, Boolean> {
        connect(host, port, username, password, verifier).use { ssh ->
            ssh.newSFTPClient().use { sftp ->
                val effectivePath = resolveEffectivePath(sftp, remotePath)
                return readTail(sftp, effectivePath)
            }
        }
    }

    private fun connect(host: String, port: Int, username: String, password: String, verifier: HostKeyVerifier): SSHClient {
        ensureBouncyCastleRegistered
        val ssh = SSHClient()
        ssh.addHostKeyVerifier(verifier)
        ssh.connect(host, port)
        ssh.authPassword(username, password)
        return ssh
    }

    /** [remotePath] peut être un dossier (ex. logs PalDefender, un fichier par démarrage serveur) —
     * dans ce cas on prend le plus récemment modifié plutôt que d'exiger un nom de fichier exact. */
    private fun resolveEffectivePath(sftp: SFTPClient, remotePath: String): String {
        val attrs = sftp.stat(remotePath)
        if (attrs.type != FileMode.Type.DIRECTORY) return remotePath
        val newest = sftp.ls(remotePath)
            .filter { it.attributes.type != FileMode.Type.DIRECTORY }
            .maxByOrNull { it.attributes.mtime }
            ?: error("Aucun fichier trouvé dans $remotePath")
        return newest.path
    }

    /** Ne lit que les derniers [MAX_LOG_BYTES] d'un gros fichier plutôt que de tout rapatrier —
     * un log de plusieurs dizaines de Mo n'a pas sa place en mémoire sur mobile, et seule la fin
     * intéresse en pratique pour du diagnostic. */
    private fun readTail(sftp: SFTPClient, path: String): Pair<String, Boolean> {
        sftp.open(path).use { file ->
            val size = file.length()
            val truncated = size > MAX_LOG_BYTES
            val startOffset = if (truncated) size - MAX_LOG_BYTES else 0L
            val length = (size - startOffset).toInt()
            val buffer = ByteArray(length)
            var readTotal = 0
            while (readTotal < length) {
                val n = file.read(startOffset + readTotal, buffer, readTotal, length - readTotal)
                if (n <= 0) break
                readTotal += n
            }
            return String(buffer, 0, readTotal, Charsets.UTF_8) to truncated
        }
    }
}
