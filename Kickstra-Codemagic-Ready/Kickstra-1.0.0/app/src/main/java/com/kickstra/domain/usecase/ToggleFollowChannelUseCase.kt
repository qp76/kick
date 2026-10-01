package com.kickstra.domain.usecase

import com.kickstra.data.local.FollowPreferences
import javax.inject.Inject

class ToggleFollowChannelUseCase @Inject constructor(
    private val followPreferences: FollowPreferences
) {
    suspend operator fun invoke(channelId: String, isCurrentlyFollowed: Boolean, username: String = "") {
        println("DEBUG: ToggleFollow - channelId: $channelId, isFollowed: $isCurrentlyFollowed, username: '$username'")
        
        if (isCurrentlyFollowed) {
            println("DEBUG: Unfollowing channel $channelId")
            followPreferences.unfollowChannel(channelId)
        } else {
            if (username.isNotEmpty()) {
                println("DEBUG: Following channel $channelId with username '$username'")
                followPreferences.followChannel(channelId, username)
            } else {
                println("DEBUG: Following channel $channelId without username (fallback)")
                followPreferences.followChannel(channelId)
            }
        }
    }
}
