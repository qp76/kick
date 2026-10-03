package com.kicklite.domain.repository

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.ChannelDetails
import com.kicklite.domain.model.ChannelVideo
import com.kicklite.domain.model.ChannelClip
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    suspend fun getChannelDetails(username: String): Flow<Resource<ChannelDetails>>
    suspend fun getChannelVideos(username: String): List<ChannelVideo>
    suspend fun getChannelClips(
        username: String,
        sort: String = "view",
        time: String = "week",
        cursor: String? = null
    ): Pair<List<ChannelClip>, String?>
    suspend fun followChannel(channelId: Long): Flow<Resource<Boolean>>
    suspend fun unfollowChannel(channelId: Long): Flow<Resource<Boolean>>
}
