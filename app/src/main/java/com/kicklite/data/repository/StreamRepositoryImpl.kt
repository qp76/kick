package com.kicklite.data.repository

import com.kicklite.common.DebugLogger
import com.kicklite.data.remote.KickPublicApi
import com.kicklite.data.remote.OfficialApiParser
import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.StreamsResponse
import com.kicklite.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val api: KickPublicApi
) : StreamRepository {

    override suspend fun getPopularStreams(
        limit: Int,
        cursor: String?,
        language: String?
    ): Flow<Resource<StreamsResponse>> = flow {
        emit(Resource.Loading())
        try {
            DebugLogger.logApi("Official KICK API /public/v2/livestreams limit=$limit cursor=$cursor language=$language")
            val response = api.getLivestreams(limit = limit.coerceIn(1, 100), cursor = cursor, language = language)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    emit(Resource.Error("KICK API returned an empty response"))
                } else {
                    val parsed = OfficialApiParser.livestreams(body)
                    emit(Resource.Success(parsed))
                }
            } else {
                val message = when (response.code()) {
                    401 -> "KICK API authorization is missing or expired. Configure a valid app/user access token or your API gateway."
                    403 -> "KICK API denied this request. Check the app scopes/token."
                    429 -> "KICK API rate limit reached. Try again in a moment."
                    else -> "KICK API HTTP ${response.code()}: ${response.message()}"
                }
                emit(Resource.Error(message))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("KICK API network error: ${e.localizedMessage ?: "Unknown error"}"))
        } catch (e: IOException) {
            emit(Resource.Error("Connection failed: ${e.localizedMessage ?: "No network connection"}"))
        } catch (e: Exception) {
            emit(Resource.Error("KICK API error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
