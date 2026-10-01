package com.kickstra.domain.usecase

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.StreamsResponse
import com.kickstra.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPopularStreamsUseCase @Inject constructor(
    private val repository: StreamRepository
) {
    suspend operator fun invoke(
        limit: Int = 24,
        cursor: String? = null,
        language: String? = null
    ): Flow<Resource<StreamsResponse>> {
        return repository.getPopularStreams(limit, cursor, language)
    }
}
