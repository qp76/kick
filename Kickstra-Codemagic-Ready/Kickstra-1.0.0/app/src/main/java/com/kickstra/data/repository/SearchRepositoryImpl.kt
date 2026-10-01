package com.kickstra.data.repository

import com.kickstra.data.mapper.toDomainModel
import com.kickstra.data.remote.KickSearchApi
import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.SearchResponse
import com.kickstra.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchApi: KickSearchApi
) : SearchRepository {

    override suspend fun search(query: String): Flow<Resource<SearchResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = searchApi.search(query)
            
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    emit(Resource.Success(apiResponse.toDomainModel()))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Unknown error"}"))
        } catch (e: IOException) {
            emit(Resource.Error("IO error: ${e.localizedMessage ?: "Connection failed"}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
