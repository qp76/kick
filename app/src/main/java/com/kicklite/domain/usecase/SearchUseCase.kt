package com.kicklite.domain.usecase

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.SearchResponse
import com.kicklite.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): Flow<Resource<SearchResponse>> {
        return repository.search(query)
    }
}
