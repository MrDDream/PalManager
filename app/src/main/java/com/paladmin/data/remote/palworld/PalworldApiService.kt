package com.paladmin.data.remote.palworld

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * API REST officielle Palworld (docs.palworldgame.com/api/rest-api). Base URL par profil :
 * https://{host}:{port}/v1/api/. Ne permet pas de give des items — voir PalDefenderApiService.
 */
interface PalworldApiService {

    @GET("v1/api/info")
    suspend fun getInfo(): PalworldInfo

    @GET("v1/api/players")
    suspend fun getPlayers(): PalworldPlayersResponse

    @GET("v1/api/settings")
    suspend fun getSettings(): PalworldSettings

    @GET("v1/api/metrics")
    suspend fun getMetrics(): PalworldMetrics

    @POST("v1/api/announce")
    suspend fun announce(@Body body: AnnounceRequest): Response<Unit>

    @POST("v1/api/kick")
    suspend fun kick(@Body body: KickBanRequest): Response<Unit>

    @POST("v1/api/ban")
    suspend fun ban(@Body body: KickBanRequest): Response<Unit>

    @POST("v1/api/unban")
    suspend fun unban(@Body body: UnbanRequest): Response<Unit>

    @POST("v1/api/save")
    suspend fun save(): Response<Unit>

    @POST("v1/api/shutdown")
    suspend fun shutdown(@Body body: ShutdownRequest): Response<Unit>

    @POST("v1/api/stop")
    suspend fun stop(): Response<Unit>
}
