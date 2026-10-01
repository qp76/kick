package com.kickstra.domain.repository

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.StreamsResponse
import kotlinx.coroutines.flow.Flow

interface StreamRepository {
    suspend fun getPopularStreams(
        limit: Int = 24,
        cursor: String? = null,
        language: String? = null
    ): Flow<Resource<StreamsResponse>>
}
