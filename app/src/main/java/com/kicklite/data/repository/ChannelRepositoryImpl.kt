package com.kicklite.data.repository

import com.kicklite.data.remote.KickChannelApi
import com.kicklite.data.local.FollowPreferences
import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.ChannelDetails
import com.kicklite.domain.model.ChannelVideo
import com.kicklite.domain.model.ChannelClip
import com.kicklite.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelApi: KickChannelApi,
    private val followPreferences: FollowPreferences
) : ChannelRepository {

    override suspend fun getChannelDetails(username: String): Flow<Resource<ChannelDetails>> = flow {
        try {
            emit(Resource.Loading())
            
            println("DEBUG: Fetching channel details for username: $username from ${KickChannelApi.BASE_URL}")
            val response = channelApi.getChannelDetails(username)
            println("DEBUG: API response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { channelDto ->
                    println("DEBUG: Channel data received, playback_url: ${channelDto.playbackUrl}")
                    val channelDetails = channelDto.toDomainModel()
                    println("DEBUG: Mapped playback_url: ${channelDetails.playbackUrl}")
                    emit(Resource.Success(channelDetails))
                } ?: run {
                    println("DEBUG: Response body is null")
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                println("DEBUG: HTTP error ${response.code()}: ${response.message()}")
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: HttpException) {
            println("DEBUG: HttpException: ${e.localizedMessage}")
            emit(Resource.Error("Network error: ${e.localizedMessage ?: "Unknown error"}"))
        } catch (e: IOException) {
            println("DEBUG: IOException: ${e.localizedMessage}")
            emit(Resource.Error("IO error: ${e.localizedMessage ?: "Connection failed"}"))
        } catch (e: Exception) {
            println("DEBUG: Exception: ${e.localizedMessage}")
            emit(Resource.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    override suspend fun getChannelVideos(username: String): List<ChannelVideo> {
        // KICK's current Public API documentation does not expose a VOD listing endpoint.
        // Keep the feature boundary honest instead of returning fabricated videos.
        return runCatching {
            val response = channelApi.getChannelVideos(username)
            if (response.isSuccessful) response.body()?.map { it.toDomainModel() } ?: emptyList() else emptyList()
        }.getOrDefault(emptyList())
    }

    override suspend fun getChannelClips(
        username: String,
        sort: String,
        time: String,
        cursor: String?
    ): Pair<List<ChannelClip>, String?> {
        return try {
            println("DEBUG: Fetching channel clips for username: $username sort=$sort time=$time cursor=$cursor")
            val response = channelApi.getChannelClips(username, sort, time, cursor)
            if (response.isSuccessful) {
                val body = response.body()
                val clips = body?.clips?.map { it.toDomainModel() } ?: emptyList()
                val nextCursor = body?.nextCursor
                println("DEBUG: Received ${clips.size} clips for $username, nextCursor=$nextCursor")
                Pair(clips, nextCursor)
            } else {
                println("DEBUG: Clips API error ${response.code()}: ${response.message()}")
                Pair(emptyList(), null)
            }
        } catch (e: Exception) {
            println("DEBUG: Error fetching clips: ${e.localizedMessage}")
            Pair(emptyList(), null)
        }
    }
    
    override suspend fun followChannel(channelId: Long): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            followPreferences.followChannel(channelId.toString())
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to follow channel"))
        }
    }

    override suspend fun unfollowChannel(channelId: Long): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            followPreferences.unfollowChannel(channelId.toString())
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to unfollow channel"))
        }
    }
}
