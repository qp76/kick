package com.kicklite.presentation.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.StreamChannel
import com.kicklite.domain.model.StreamCategory
import com.kicklite.domain.usecase.GetFollowedChannelsUseCase
import com.kicklite.domain.usecase.GetPopularStreamsUseCase
import com.kicklite.domain.usecase.GetChannelDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Deferred
import javax.inject.Inject

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val getFollowedChannelsUseCase: GetFollowedChannelsUseCase,
    private val getPopularStreamsUseCase: GetPopularStreamsUseCase,
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FollowingState())
    val state: StateFlow<FollowingState> = _state.asStateFlow()

    init {
        loadFollowedContent()
    }

    fun loadFollowedContent() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Get followed channels with usernames
                getFollowedChannelsUseCase.getFollowedChannelsWithUsernames().collect { followedChannels ->
                    println("DEBUG: Found ${followedChannels.size} followed channels")
                    
                    _state.value = _state.value.copy(
                        followedChannelIds = followedChannels.map { it.channelId }.toSet()
                    )
                    
                    if (followedChannels.isEmpty()) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            liveStreams = emptyList(),
                            offlineChannels = emptyList()
                        )
                        return@collect
                    }
                    
                    // Load channel details for all followed channels
                    loadAllFollowedChannels(followedChannels)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private suspend fun loadAllFollowedChannels(followedChannels: List<com.kicklite.data.local.FollowPreferences.FollowedChannel>) {
        try {
            // Load channel details for each followed channel
            val channelDetailsList: List<com.kicklite.domain.model.ChannelDetails> = followedChannels.map { followedChannel ->
                viewModelScope.async {
                    try {
                        var channelDetails: com.kicklite.domain.model.ChannelDetails? = null
                        getChannelDetailsUseCase(followedChannel.username).collect { resource ->
                            when (resource) {
                                is Resource.Success -> {
                                    channelDetails = resource.data
                                }
                                is Resource.Error -> {
                                    println("DEBUG: Error loading channel ${followedChannel.username}: ${resource.message}")
                                }
                                is Resource.Loading -> {
                                    // Wait for completion
                                }
                            }
                        }
                        channelDetails
                    } catch (e: Exception) {
                        println("DEBUG: Exception loading channel ${followedChannel.username}: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()
            
            println("DEBUG: Successfully loaded ${channelDetailsList.size} channel details")
            
            // Separate live and offline channels based on livestream field
            val liveChannelsResult: List<com.kicklite.domain.model.ChannelDetails>
            val offlineChannelsResult: List<com.kicklite.domain.model.ChannelDetails>
            
            val partitionResult = channelDetailsList.partition { channel ->
                channel.livestream != null
            }
            liveChannelsResult = partitionResult.first
            offlineChannelsResult = partitionResult.second
            
            println("DEBUG: Live channels count: ${liveChannelsResult.size}")
            println("DEBUG: Offline channels count: ${offlineChannelsResult.size}")
            liveChannelsResult.forEach { channel ->
                println("DEBUG: Live channel: ${channel.user.username} - Stream: ${channel.livestream?.sessionTitle}")
            }
            offlineChannelsResult.forEach { channel ->
                println("DEBUG: Offline channel: ${channel.user.username}")
            }
            
            // Convert live channels to LiveStream format for consistency
            val liveStreams = liveChannelsResult.mapNotNull { channel ->
                channel.livestream?.let { livestream ->
                    LiveStream(
                        id = livestream.id.toString(),
                        title = livestream.sessionTitle,
                        viewerCount = livestream.viewerCount,
                        thumbnail = livestream.thumbnail,
                        startTime = livestream.startTime ?: "",
                        channel = StreamChannel(
                            id = channel.id,
                            username = channel.user.username,
                            slug = channel.slug,
                            profilePic = channel.user.profilePic
                        ),
                        category = StreamCategory(
                            id = livestream.categories.firstOrNull()?.id ?: 0,
                            name = livestream.categories.firstOrNull()?.name ?: "Just Chatting",
                            slug = livestream.categories.firstOrNull()?.slug ?: "just-chatting"
                        ),
                        language = livestream.language,
                        isMature = livestream.isMature,
                        tags = livestream.tags
                    )
                }
            }
            
            // Sort live streams by viewer count (highest first)
            val sortedLiveStreams = liveStreams.sortedByDescending { it.viewerCount }
            
            _state.value = _state.value.copy(
                liveStreams = sortedLiveStreams,
                offlineChannels = offlineChannelsResult,
                isLoading = false,
                error = null
            )
            
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private suspend fun loadLiveStreamsAndOfflineChannels(followedIds: Set<String>) {
        try {
            // Load popular streams to find live followed channels
            getPopularStreamsUseCase(language = null).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { response ->
                            // Filter live streams for followed channels
                            val followedLiveStreams = response.livestreams.filter { stream ->
                                followedIds.contains(stream.channel.id.toString())
                            }.sortedByDescending { it.viewerCount } // Sort by viewer count (highest first)
                            
                            // Find offline channels (followed but not live)
                            val liveChannelIds = followedLiveStreams.map { it.channel.id.toString() }.toSet()
                            val offlineChannelIds = followedIds - liveChannelIds
                            
                            _state.value = _state.value.copy(
                                liveStreams = followedLiveStreams,
                                isLoading = false,
                                error = null
                            )
                            
                            // Load details for offline channels
                            if (offlineChannelIds.isNotEmpty()) {
                                loadOfflineChannelDetails(offlineChannelIds)
                            }
                        }
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                    is Resource.Loading -> {
                        // Keep loading state
                    }
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private suspend fun loadOfflineChannelDetails(offlineChannelIds: Set<String>) {
        try {
            // For offline channels, we need to get their details
            // Since we don't have a direct API for channel by ID, we'll use a simplified approach
            // In a real app, you'd want to cache channel details or have a different API endpoint
            
            val offlineChannels = mutableListOf<com.kicklite.domain.model.ChannelDetails>()
            
            // For now, we'll create placeholder offline channels
            // In a real implementation, you'd fetch these from a cache or different endpoint
            offlineChannelIds.forEach { channelId ->
                // Try to load channel details if we have a username mapping
                // For now, we'll skip this complex part and just show a message
            }
            
            _state.value = _state.value.copy(
                offlineChannels = offlineChannels
            )
        } catch (e: Exception) {
            // Don't fail the whole screen for offline channel errors
            println("DEBUG: Error loading offline channel details: ${e.message}")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            
            try {
                // Get followed channels with usernames
                getFollowedChannelsUseCase.getFollowedChannelsWithUsernames().collect { followedChannels ->
                    _state.value = _state.value.copy(
                        followedChannelIds = followedChannels.map { it.channelId }.toSet()
                    )
                    
                    if (followedChannels.isEmpty()) {
                        _state.value = _state.value.copy(
                            isRefreshing = false,
                            liveStreams = emptyList(),
                            offlineChannels = emptyList()
                        )
                        return@collect
                    }
                    
                    // Load channel details for all followed channels
                    loadAllFollowedChannels(followedChannels)
                    _state.value = _state.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = e.message
                )
            }
        }
    }

    fun retry() {
        loadFollowedContent()
    }
}
