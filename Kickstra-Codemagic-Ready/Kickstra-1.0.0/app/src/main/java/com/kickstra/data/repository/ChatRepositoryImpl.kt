package com.kickstra.data.repository

import com.kickstra.data.remote.KickApi
import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.ChatResponse
import com.kickstra.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: KickApi
) : ChatRepository {

    override suspend fun getChatHistory(channelId: Long): Flow<Resource<ChatResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = api.getChatHistory(channelId)
            
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    emit(Resource.Success(apiResponse.data.toDomainModel()))
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
