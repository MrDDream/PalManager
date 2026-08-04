package com.paladmin.data.remote.paldefender

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API REST PalDefender (ultimeit.github.io/PalDefender/RESTAPI). Base URL par profil :
 * http(s)://{host}:{port}/v1/pdapi/. Comble le give d'items/Pals absent de l'API officielle.
 */
interface PalDefenderApiService {

    @GET("v1/pdapi/version")
    suspend fun getVersion(): PalDefenderVersion

    @GET("v1/pdapi/players")
    suspend fun getPlayers(): PalDefenderPlayersResponse

    @GET("v1/pdapi/player/{playerIdentifier}")
    suspend fun getPlayer(@Path("playerIdentifier") playerIdentifier: String): PalDefenderPlayer

    @POST("v1/pdapi/kick/{playerIdentifier}")
    suspend fun kick(@Path("playerIdentifier") playerIdentifier: String): Response<Unit>

    @POST("v1/pdapi/ban/{playerIdentifier}")
    suspend fun ban(@Path("playerIdentifier") playerIdentifier: String): Response<Unit>

    @POST("v1/pdapi/unban/{userId}")
    suspend fun unban(@Path("userId") userId: String): Response<Unit>

    @POST("v1/pdapi/banip/{ip}")
    suspend fun banIp(@Path("ip") ip: String): Response<Unit>

    @POST("v1/pdapi/unbanip/{ip}")
    suspend fun unbanIp(@Path("ip") ip: String): Response<Unit>

    @GET("v1/pdapi/banlist")
    suspend fun getBanList(): BanListResponse

    @POST("v1/pdapi/give/items/{playerIdentifier}")
    suspend fun giveItems(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: GiveItemsRequest,
    ): Response<Unit>

    @POST("v1/pdapi/give/pals/{playerIdentifier}")
    suspend fun givePal(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: GivePalRequest,
    ): Response<Unit>

    @POST("v1/pdapi/give/paleggs/{playerIdentifier}")
    suspend fun givePalEgg(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: GivePalEggRequest,
    ): Response<Unit>

    @POST("v1/pdapi/give/paltemplate/{playerIdentifier}")
    suspend fun givePalTemplate(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: GivePalTemplateRequest,
    ): Response<Unit>

    @POST("v1/pdapi/give/progression/{playerIdentifier}")
    suspend fun giveProgression(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: GiveProgressionRequest,
    ): Response<Unit>

    @POST("v1/pdapi/learntech/{playerIdentifier}")
    suspend fun learnTech(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: TechRequest,
    ): Response<Unit>

    @POST("v1/pdapi/forgettech/{playerIdentifier}")
    suspend fun forgetTech(
        @Path("playerIdentifier") playerIdentifier: String,
        @Body body: TechRequest,
    ): Response<Unit>

    @GET("v1/pdapi/techs/{playerIdentifier}")
    suspend fun getTechs(@Path("playerIdentifier") playerIdentifier: String): TechsResponse

    @GET("v1/pdapi/pals/{playerIdentifier}")
    suspend fun getPals(@Path("playerIdentifier") playerIdentifier: String): PlayerPalsResponse

    @GET("v1/pdapi/items/{playerIdentifier}")
    suspend fun getItems(@Path("playerIdentifier") playerIdentifier: String): PlayerItemsResponse

    @GET("v1/pdapi/progression/{playerIdentifier}")
    suspend fun getProgression(@Path("playerIdentifier") playerIdentifier: String): PlayerProgressionResponse

    @GET("v1/pdapi/guilds")
    suspend fun getGuilds(): GuildsResponse

    @GET("v1/pdapi/guild/{guildId}")
    suspend fun getGuild(@Path("guildId") guildId: String): GuildDetailResponse

    @POST("v1/pdapi/deletebase/{baseCampId}")
    suspend fun deleteBase(@Path("baseCampId") baseCampId: String): Response<Unit>

    @POST("v1/pdapi/Broadcast")
    suspend fun broadcast(@Body body: BroadcastRequest): Response<Unit>

    @POST("v1/pdapi/Alert")
    suspend fun alert(@Body body: AlertRequest): Response<Unit>

    @POST("v1/pdapi/SendPlayerMessage")
    suspend fun sendPlayerMessage(@Body body: PlayerMessageRequest): Response<Unit>

    @POST("v1/pdapi/ReloadConfig")
    suspend fun reloadConfig(): Response<Unit>
}
