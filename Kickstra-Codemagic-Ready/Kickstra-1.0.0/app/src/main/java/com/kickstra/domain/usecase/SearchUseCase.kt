package com.kickstra.domain.usecase

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.SearchResponse
import com.kickstra.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): Flow<Resource<SearchResponse>> {
        return repository.search(query)
    }
}
