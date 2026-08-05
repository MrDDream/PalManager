package com.paladmin.data.remote.paldefender

import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.NetworkClients
import com.paladmin.debug.DebugLogger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalDefenderClientFactory @Inject constructor(
    private val debugLogger: DebugLogger,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun create(profile: ServerProfile): PalDefenderApiService {
        val client = NetworkClients.build(
            authInterceptor = PalDefenderAuthInterceptor(profile.palDefenderToken),
            debugLogger = debugLogger,
        )
        val retrofit = Retrofit.Builder()
            .baseUrl("http://${profile.host}:${profile.palDefenderPort}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(PalDefenderApiService::class.java)
    }
}
