package com.kickstra.data.remote

import com.kickstra.data.dto.SearchApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface KickSearchApi {
    @GET("search")
    suspend fun search(
        @Query("searched_word") query: String
    ): Response<SearchApiResponse>

    companion object {
        const val BASE_URL = "https://kick.com/api/"
    }
}
