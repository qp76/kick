package com.kicklite.domain.usecase

import com.kicklite.data.local.FollowPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IsChannelFollowedUseCase @Inject constructor(
    private val followPreferences: FollowPreferences
) {
    operator fun invoke(channelId: String): Flow<Boolean> {
        return followPreferences.getFollowedChannels().map { followedChannels ->
            followedChannels.contains(channelId)
        }
    }
    
    suspend operator fun invoke(channelId: Long): Boolean {
        return followPreferences.isChannelFollowed(channelId.toString())
    }
}
