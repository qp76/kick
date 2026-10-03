package com.kicklite.domain.usecase

import com.kicklite.data.local.FollowPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFollowedChannelsUseCase @Inject constructor(
    private val followPreferences: FollowPreferences
) {
    operator fun invoke(): Flow<Set<String>> {
        return followPreferences.getFollowedChannels()
    }
    
    fun getFollowedChannelsWithUsernames(): Flow<List<FollowPreferences.FollowedChannel>> {
        return followPreferences.getFollowedChannelsWithUsernames().map { channels ->
            println("DEBUG: GetFollowedChannelsWithUsernames - found ${channels.size} channels")
            channels.forEach { channel ->
                println("DEBUG: Followed channel - ID: ${channel.channelId}, Username: ${channel.username}")
            }
            channels
        }
    }
}
