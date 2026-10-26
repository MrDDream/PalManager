package com.paladmin.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.palworld.PalworldClientFactory
import com.paladmin.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import javax.inject.Inject

/** État "en direct" d'un profil, obtenu en sondant `/v1/api/metrics` avec un délai court. */
sealed interface ServerProbe {
    data object Checking : ServerProbe

    /** [latencyMs] = durée mesurée de la requête metrics (aller-retour HTTP complet). */
    data class Online(val players: Int, val maxPlayers: Int, val fps: Int, val latencyMs: Long) : ServerProbe

    /** Le serveur a répondu mais a refusé la requête (mot de passe admin incorrect). */
    data object Unauthorized : ServerProbe

    /** Le serveur a répondu avec un autre code HTTP d'erreur. */
    data class HttpError(val code: Int) : ServerProbe

    /** Pas de réponse (hôte injoignable, port fermé, délai dépassé). */
    data object Offline : ServerProbe
}

data class ServerListUiState(
    /** Vrai tant que la base n'a pas encore émis la liste (évite d'afficher l'état vide un instant). */
    val isLoading: Boolean = true,
    val profiles: List<ServerProfile> = emptyList(),
    val probes: Map<Long, ServerProbe> = emptyMap(),
)

@HiltViewModel
class ServerProfileListViewModel @Inject constructor(
    private val repository: ServerRepository,
    private val palworldClientFactory: PalworldClientFactory,
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ServerProfile>?>(null)
    private val _probes = MutableStateFlow<Map<Long, ServerProbe>>(emptyMap())

    val state: StateFlow<ServerListUiState> = combine(_profiles, _probes) { profiles, probes ->
        ServerListUiState(isLoading = profiles == null, profiles = profiles.orEmpty(), probes = probes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerListUiState())

    // Accédés uniquement depuis le thread principal (viewModelScope = Dispatchers.Main.immediate).
    private val probeJobs = mutableMapOf<Long, Job>()
    private val probedKeys = mutableMapOf<Long, ConnectionKey>()
    private val lastProbeAt = mutableMapOf<Long, Long>()

    /** Ce qui détermine le résultat d'une sonde : si ça change (profil modifié), on re-sonde. */
    private data class ConnectionKey(val host: String, val port: Int, val password: String)

    private fun ServerProfile.connectionKey() = ConnectionKey(host, palworldPort, palworldPassword)

    init {
        viewModelScope.launch {
            repository.observeProfiles().collect { profiles ->
                _profiles.value = profiles
                val ids = profiles.map { it.id }.toSet()
                (probeJobs.keys - ids).forEach { probeJobs.remove(it)?.cancel() }
                probedKeys.keys.retainAll(ids)
                lastProbeAt.keys.retainAll(ids)
                _probes.update { current -> current.filterKeys { it in ids } }
                profiles.forEach { profile ->
                    if (probedKeys[profile.id] != profile.connectionKey()) probe(profile)
                }
            }
        }
    }

    /** Relance la sonde d'un profil (bouton "Vérifier"). */
    fun probe(profile: ServerProfile) {
        probeJobs.remove(profile.id)?.cancel()
        probedKeys[profile.id] = profile.connectionKey()
        _probes.update { it + (profile.id to ServerProbe.Checking) }
        probeJobs[profile.id] = viewModelScope.launch {
            val start = System.nanoTime()
            val result = try {
                Result.success(withTimeout(PROBE_TIMEOUT_MS) { palworldClientFactory.create(profile).getMetrics() })
            } catch (e: TimeoutCancellationException) {
                Result.failure(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            val probe = result.fold(
                onSuccess = { m -> ServerProbe.Online(m.currentplayernum, m.maxplayernum, m.serverfps, elapsedMs) },
                onFailure = { error ->
                    when {
                        error is HttpException && (error.code() == 401 || error.code() == 403) -> ServerProbe.Unauthorized
                        error is HttpException -> ServerProbe.HttpError(error.code())
                        else -> ServerProbe.Offline
                    }
                },
            )
            _probes.update { it + (profile.id to probe) }
            lastProbeAt[profile.id] = System.nanoTime()
        }
    }

    /** Au retour sur l'écran : re-sonde les profils dont le dernier résultat date un peu. */
    fun refreshStale() {
        val now = System.nanoTime()
        _profiles.value.orEmpty().forEach { profile ->
            val running = probeJobs[profile.id]?.isActive == true
            val last = lastProbeAt[profile.id] ?: return@forEach
            if (!running && now - last > STALE_AFTER_NS) probe(profile)
        }
    }

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch { repository.deleteProfile(profile) }
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 5_000L
        const val STALE_AFTER_NS = 20_000_000_000L
    }
}
