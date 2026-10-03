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
        return try {
            println("DEBUG: Fetching channel videos for username: $username")
            
            // Gerçek API çağrısını dene
            val response = channelApi.getChannelVideos(username)
            
            if (response.isSuccessful) {
                val apiVideos = response.body()?.map { it.toDomainModel() } ?: emptyList()
                println("DEBUG: Received ${apiVideos.size} videos from API for $username")
                if (apiVideos.isNotEmpty()) {
                    return apiVideos
                }
            } else {
                println("DEBUG: Videos API error ${response.code()}: ${response.message()}")
            }
            
            // API başarısız olursa, kanal-specific mock data döndür
            val mockVideos = getMockChannelVideos(username)
            println("DEBUG: Returning ${mockVideos.size} mock videos for $username")
            mockVideos.forEach { video ->
                println("DEBUG: Mock video - ID: ${video.id}, title: ${video.sessionTitle}")
            }
            return mockVideos
            
        } catch (e: Exception) {
            println("DEBUG: Error fetching videos: ${e.localizedMessage}")
            // Hata durumunda kanal-specific mock data döndür
            return getMockChannelVideos(username)
        }
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
    
    private fun getMockChannelVideos(username: String): List<ChannelVideo> {
        // Her kanal için farklı mock video data döndür
        return when (username.lowercase()) {
            "trielba" -> getTrlelbaVideos()
            "blush" -> getBlushVideos()
            "xqc" -> getXqcVideos()
            else -> getGenericVideos(username)
        }
    }
    
    private fun getTrlelbaVideos(): List<ChannelVideo> {
        // Original videos.json'daki Trielba videoları
        return listOf(
            ChannelVideo(
                id = 72601201,
                slug = "71040976-sa-instatrielbaenre-blerp",
                channelId = 3363391,
                createdAt = "2025-09-04 10:55:22",
                sessionTitle = "Sa | insta:trielbaenre !blerp",
                isLive = false,
                startTime = "2025-09-04 10:55:22",
                source = "https://stream.kick.com/ivs/v1/196233775518/zHizv5d6HOfc/2025/9/4/10/55/MNQ5xftjtyF2/media/hls/master.m3u8",
                duration = 3600,
                language = "Turkish",
                isMature = false,
                viewerCount = 734,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://images.kick.com/video_thumbnails/zHizv5d6HOfc/MNQ5xftjtyF2/720.webp",
                    srcset = ""
                ),
                views = 27,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 15,
                        name = "Just Chatting",
                        slug = "just-chatting"
                    )
                )
            ),
            ChannelVideo(
                id = 72450892,
                slug = "69361071-sa-instatrielbaenre-blerp",
                channelId = 3363391,
                createdAt = "2025-09-03 13:48:53",
                sessionTitle = "NBA 2K26 Gameplay",
                isLive = false,
                startTime = "2025-09-03 13:48:50",
                source = "https://stream.kick.com/ivs/v1/196233775518/zHizv5d6HOfc/2025/9/3/13/48/QaHs3ZJR72AW/media/hls/master.m3u8",
                duration = 9491,
                language = "Turkish",
                isMature = false,
                viewerCount = 78,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://images.kick.com/video_thumbnails/zHizv5d6HOfc/QaHs3ZJR72AW/720.webp",
                    srcset = ""
                ),
                views = 395,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 11098,
                        name = "NBA 2K26",
                        slug = "nba-2k26"
                    )
                )
            )
        )
    }
    
    private fun getBlushVideos(): List<ChannelVideo> {
        return listOf(
            ChannelVideo(
                id = 98001001,
                slug = "blush-beauty-stream-1",
                channelId = 9800001,
                createdAt = "2025-09-06 14:30:00",
                sessionTitle = "Beauty & Makeup Tutorial Stream",
                isLive = false,
                startTime = "2025-09-06 14:30:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 7200,
                language = "English",
                isMature = false,
                viewerCount = 1250,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/ff69b4/ffffff?text=Blush+Beauty",
                    srcset = ""
                ),
                views = 5420,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 21,
                        name = "Beauty & Makeup",
                        slug = "beauty-makeup"
                    )
                )
            ),
            ChannelVideo(
                id = 98001002,
                slug = "blush-fashion-haul",
                channelId = 9800001,
                createdAt = "2025-09-05 16:00:00",
                sessionTitle = "Fashion Haul & Try-On",
                isLive = false,
                startTime = "2025-09-05 16:00:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 5400,
                language = "English",
                isMature = false,
                viewerCount = 890,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/ffc0cb/000000?text=Fashion+Haul",
                    srcset = ""
                ),
                views = 3200,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 22,
                        name = "Fashion",
                        slug = "fashion"
                    )
                )
            )
        )
    }
    
    private fun getXqcVideos(): List<ChannelVideo> {
        return listOf(
            ChannelVideo(
                id = 99001001,
                slug = "xqc-variety-gaming-1",
                channelId = 9900001,
                createdAt = "2025-09-06 20:00:00",
                sessionTitle = "Variety Gaming Night - React Content",
                isLive = false,
                startTime = "2025-09-06 20:00:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 28800, // 8 hours
                language = "English",
                isMature = false,
                viewerCount = 45000,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/800080/ffffff?text=xQc+Gaming",
                    srcset = ""
                ),
                views = 125000,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 1,
                        name = "Gaming",
                        slug = "gaming"
                    )
                )
            ),
            ChannelVideo(
                id = 99001002,
                slug = "xqc-react-stream",
                channelId = 9900001,
                createdAt = "2025-09-05 19:30:00",
                sessionTitle = "Reddit React & Chat Interaction",
                isLive = false,
                startTime = "2025-09-05 19:30:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 21600, // 6 hours
                language = "English",
                isMature = false,
                viewerCount = 38000,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/ff4500/ffffff?text=xQc+React",
                    srcset = ""
                ),
                views = 89000,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 15,
                        name = "Just Chatting",
                        slug = "just-chatting"
                    )
                )
            )
        )
    }
    
    private fun getGenericVideos(username: String): List<ChannelVideo> {
        return listOf(
            ChannelVideo(
                id = (username.hashCode().toLong() + 1000000),
                slug = "$username-stream-1",
                channelId = username.hashCode().toLong(),
                createdAt = "2025-09-06 12:00:00",
                sessionTitle = "$username's Stream - Gaming Session",
                isLive = false,
                startTime = "2025-09-06 12:00:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 10800, // 3 hours
                language = "English",
                isMature = false,
                viewerCount = 250,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/4169e1/ffffff?text=${username}+Stream",
                    srcset = ""
                ),
                views = 1500,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 1,
                        name = "Gaming",
                        slug = "gaming"
                    )
                )
            ),
            ChannelVideo(
                id = (username.hashCode().toLong() + 2000000),
                slug = "$username-stream-2",
                channelId = username.hashCode().toLong(),
                createdAt = "2025-09-05 15:30:00",
                sessionTitle = "$username's Previous Stream",
                isLive = false,
                startTime = "2025-09-05 15:30:00",
                source = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                duration = 7200, // 2 hours
                language = "English",
                isMature = false,
                viewerCount = 180,
                thumbnail = com.kicklite.domain.model.StreamThumbnail(
                    src = "https://via.placeholder.com/720x405/28a745/ffffff?text=${username}+Past",
                    srcset = ""
                ),
                views = 950,
                video = null,
                categories = listOf(
                    com.kicklite.domain.model.StreamCategory(
                        id = 15,
                        name = "Just Chatting",
                        slug = "just-chatting"
                    )
                )
            )
        )
    }

    override suspend fun followChannel(channelId: Long): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            // Mock implementation - in real app this would be a proper API call
            println("DEBUG: Mock follow channel: $channelId")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to follow channel"))
        }
    }

    override suspend fun unfollowChannel(channelId: Long): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            // Mock implementation - in real app this would be a proper API call
            println("DEBUG: Mock unfollow channel: $channelId")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to unfollow channel"))
        }
    }
}
