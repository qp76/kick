package com.kickstra.domain.repository

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.SearchResponse
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(query: String): Flow<Resource<SearchResponse>>
}
