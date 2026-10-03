package com.kicklite.domain.repository

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.SearchResponse
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(query: String): Flow<Resource<SearchResponse>>
}
