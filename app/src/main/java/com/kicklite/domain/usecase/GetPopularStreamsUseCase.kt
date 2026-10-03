package com.kicklite.domain.usecase

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.StreamsResponse
import com.kicklite.domain.repository.StreamRepository
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
