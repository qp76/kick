package com.kicklite.domain.repository

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.StreamsResponse
import kotlinx.coroutines.flow.Flow

interface StreamRepository {
    suspend fun getPopularStreams(
        limit: Int = 24,
        cursor: String? = null,
        language: String? = null
    ): Flow<Resource<StreamsResponse>>
}
