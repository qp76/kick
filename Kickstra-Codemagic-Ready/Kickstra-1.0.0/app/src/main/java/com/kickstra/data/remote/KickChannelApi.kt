package com.kickstra.data.remote

import com.kickstra.data.dto.ChannelDetailsDto
import com.kickstra.data.dto.ChannelVideoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickChannelApi {
    
    @GET("v2/channels/{username}")
    suspend fun getChannelDetails(
        @Path("username") username: String
    ): Response<ChannelDetailsDto>
    
    @GET("v2/channels/{username}/videos")
    suspend fun getChannelVideos(
        @Path("username") username: String
    ): Response<List<ChannelVideoDto>>
    
    @GET("v2/channels/{username}/clips")
    suspend fun getChannelClips(
        @Path("username") username: String,
        @Query("sort") sort: String = "view", // view | date
        @Query("time") time: String = "week", // day | week | month | all
        @Query("cursor") cursor: String? = null
    ): Response<com.kickstra.data.dto.ClipsResponseDto>
    
    companion object {
        const val BASE_URL = "https://kick.com/api/"
    }
}
