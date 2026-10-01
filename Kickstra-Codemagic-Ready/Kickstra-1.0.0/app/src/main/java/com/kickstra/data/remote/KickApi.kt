package com.kickstra.data.remote

import com.kickstra.data.dto.ChannelDetailsDto
import com.kickstra.data.dto.ChatHistoryApiResponse
import com.kickstra.data.dto.PopularStreamsApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApi {
    
    @GET("v1/livestreams")
    suspend fun getPopularStreams(
        @Query("limit") limit: Int = 24,
        @Query("sort") sort: String = "viewer_count_desc",
        @Query("after") after: String? = null,
        @Query("language") language: String? = null
    ): Response<PopularStreamsApiResponse>
    
    @GET("v1/chat/{channelId}/history")
    suspend fun getChatHistory(
        @Path("channelId") channelId: Long
    ): Response<ChatHistoryApiResponse>
    
    companion object {
        const val BASE_URL = "https://web.kick.com/api/"
    }
}
