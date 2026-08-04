package com.paladmin.data.remote.palworld

import com.paladmin.data.model.ServerProfile
import com.paladmin.data.remote.NetworkClients
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalworldClientFactory @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun create(profile: ServerProfile): PalworldApiService {
        val client = NetworkClients.build(
            authInterceptor = PalworldAuthInterceptor(profile.palworldPassword),
        )
        val retrofit = Retrofit.Builder()
            .baseUrl("http://${profile.host}:${profile.palworldPort}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(PalworldApiService::class.java)
    }
}
