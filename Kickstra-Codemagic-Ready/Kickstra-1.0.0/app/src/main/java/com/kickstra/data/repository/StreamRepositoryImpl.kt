package com.kickstra.data.repository

import com.kickstra.data.remote.KickApi
import com.kickstra.data.remote.KickChannelApi
import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.StreamsResponse
import com.kickstra.domain.repository.StreamRepository
import com.kickstra.common.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val api: KickApi,
    private val channelApi: KickChannelApi
) : StreamRepository {

    override suspend fun getPopularStreams(
        limit: Int,
        cursor: String?,
        language: String?
    ): Flow<Resource<StreamsResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            DebugLogger.logApi("API call - limit: $limit, after: $cursor, language: $language")
            
            val response = api.getPopularStreams(
                limit = limit,
                after = cursor,
                language = language
            )
            
            DebugLogger.logApi("API response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    DebugLogger.logApi("API response successful, streams count: ${apiResponse.data.livestreams.size}")
                    DebugLogger.logApi("Next cursor from API: ${apiResponse.data.pagination.nextCursor}")
                    emit(Resource.Success(apiResponse.data.toDomainModel()))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                DebugLogger.logApi("API response failed: ${response.code()} - ${response.message()}")
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
