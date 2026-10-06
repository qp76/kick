package com.kicklite.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Official KICK Public API surface.
 * Docs: https://docs.kick.com/
 */
interface KickPublicApi {
    @GET("public/v2/livestreams")
    suspend fun getLivestreams(
        @Query("limit") limit: Int = 25,
        @Query("cursor") cursor: String? = null,
        @Query("language") language: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("broadcaster_user_id") broadcasterUserId: List<Long>? = null
    ): Response<JsonObject>

    @GET("public/v1/users")
    suspend fun getUsers(
        @Query("id") ids: List<Long>? = null,
        @Query("username") usernames: List<String>? = null
    ): Response<JsonObject>

    @GET("public/v1/channels")
    suspend fun getChannels(
        @Query("broadcaster_user_id") broadcasterUserIds: List<Long>? = null,
        @Query("slug") slugs: List<String>? = null
    ): Response<JsonObject>

    @GET("public/v1/categories")
    suspend fun getCategories(
        @Query("id") ids: List<Long>? = null,
        @Query("name") names: List<String>? = null
    ): Response<JsonObject>
}
