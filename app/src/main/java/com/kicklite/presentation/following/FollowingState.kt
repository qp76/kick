package com.kicklite.presentation.following

import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.ChannelDetails

data class FollowingState(
    val isLoading: Boolean = false,
    val liveStreams: List<LiveStream> = emptyList(),
    val offlineChannels: List<ChannelDetails> = emptyList(),
    val followedChannelIds: Set<String> = emptySet(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)
