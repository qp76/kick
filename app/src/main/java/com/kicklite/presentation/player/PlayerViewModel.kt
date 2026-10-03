package com.kicklite.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.ChatMessage
import com.kicklite.data.remote.KickChatSocket
import com.kicklite.domain.model.ChannelDetails
import com.kicklite.domain.model.ChannelUser
import com.kicklite.domain.model.ChannelImage
import com.kicklite.domain.model.ChannelLivestream
import com.kicklite.domain.usecase.GetChannelDetailsUseCase
import com.kicklite.domain.usecase.GetChatHistoryUseCase
import com.kicklite.domain.usecase.IsChannelFollowedUseCase
import com.kicklite.domain.usecase.ToggleFollowChannelUseCase
import com.kicklite.data.repository.StreamQualityRepository
import com.kicklite.data.model.StreamQuality
import com.kicklite.data.local.QualityPreferences
import com.kicklite.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val isChannelFollowedUseCase: IsChannelFollowedUseCase,
    private val toggleFollowChannelUseCase: ToggleFollowChannelUseCase,
    private val pipModeManager: PipModeManager,
    private val streamQualityRepository: StreamQualityRepository,
    private val qualityPreferences: QualityPreferences,
    private val playerManager: PlayerManager,
    private val chatSocket: KickChatSocket,
    private val dataSaverPrefs: com.kicklite.data.local.DataSaverPreferences,
    private val chatPrefs: com.kicklite.data.local.ChatPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var chatRefreshJob: Job? = null
    private var liveStatusJob: Job? = null
    private var currentChannelId: Long? = null
    private var isFirstChatLoad = true
    private var latestMessageTimestamp: String? = null
    private var cachedNormalQuality: StreamQuality? = null
    private var socketJob: Job? = null
    @Volatile private var socketConnected = false
    private var chatroomId: Long? = null
    private val socketBuffer = java.util.concurrent.ConcurrentLinkedQueue<ChatMessage>()
    
    init {
        // Load saved quality preference
        val savedQuality = qualityPreferences.getSavedQuality()
        _state.value = _state.value.copy(selectedQuality = savedQuality)
        
        // Load saved playback mode preference
        val savedModeName = qualityPreferences.getSavedPlaybackMode()
        val savedMode = try { savedModeName?.let { PlaybackMode.valueOf(it) } } catch (e: Exception) { null }
        if (savedMode != null) {
            _state.value = _state.value.copy(playbackMode = savedMode)
        }
        
        // Data saver: caps Auto quality at 480p
        viewModelScope.launch {
            dataSaverPrefs.enabled.collect { playerManager.setDataSaver(it) }
        }

        // Listen for PiP mode changes
        viewModelScope.launch {
            pipModeManager.pipModeEvents.collect { isInPip ->
                _state.value = _state.value.copy(isPipMode = isInPip)
            }
        }
    }
    
    companion object {
        private const val MAX_CHAT_MESSAGES = 300 // Maximum chat messages to keep
    }

    fun loadChannelData(channelSlug: String) {
        resetChatState()
        loadChannelDetails(channelSlug)
        // For PlayerScreen (without stream data), we'll use channel slug for follow status
        checkFollowStatus(channelSlug)
        // Ensure chat is enabled by default for fresh loads
        if (!_state.value.isChatEnabled) {
            _state.value = _state.value.copy(isChatEnabled = true)
        }
    }

    fun initializeWithStream(stream: LiveStream) {
        _state.value = _state.value.copy(
            stream = stream,
            isLoading = true,
            error = null
        )
        
        // Load real channel details to get playback URL  
        // Use lowercase username for API call
        val username = stream.channel.username.lowercase()
        println("DEBUG: Stream channel username: ${stream.channel.username}, using: $username")
        loadChannelDetails(username)
        
        // Load chat (only if enabled)
        resetChatState()
        if (_state.value.isChatEnabled) {
            loadChatHistory(stream.channel.id)
            // Start auto refresh for chat
            startChatAutoRefresh(stream.channel.id)
        }
        
        // Start auto refresh for live status
        startLiveStatusRefresh()
        
        // Check if channel is followed
        checkFollowStatus(stream.channel.id.toString())
    }

    private fun loadChannelDetails(channelSlug: String) {
        viewModelScope.launch {
            println("DEBUG: Loading channel details for slug: $channelSlug")
            getChannelDetailsUseCase(channelSlug).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        println("DEBUG: Channel details loading...")
                        _state.value = _state.value.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        resource.data?.let { channel ->
                            println("DEBUG: Channel details loaded successfully")
                            println("DEBUG: Playback URL: ${channel.playbackUrl}")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                channelDetails = channel,
                                streamUrl = channel.playbackUrl, // Use real playback URL from API
                                manifestUrl = channel.playbackUrl, // Store manifest URL for quality loading
                                isPlaying = true, // Auto-start playback
                                error = null
                            )
                            println("DEBUG: Auto-starting playback")
                            
                            // Load available qualities
                            channel.playbackUrl?.let { url ->
                                loadStreamQualities(url)
                            }
                            // Apply current playback mode after URL is set
                            applyPlaybackModeForCurrentState()
                        }
                    }
                    is Resource.Error -> {
                        println("DEBUG: Channel details error: ${resource.message}")
                        // Fallback to mock URL if API fails
                        val currentStream = _state.value.stream
                        val fallbackUrl = if (currentStream != null) {
                            "https://ingest.kick.com/live/${currentStream.channel.slug}_source/playlist.m3u8"
                        } else {
                            null
                        }
                        println("DEBUG: Using fallback URL: $fallbackUrl")
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            streamUrl = fallbackUrl,
                            manifestUrl = fallbackUrl, // Store manifest URL for quality loading
                            isPlaying = fallbackUrl != null, // Auto-start if we have a URL
                            error = if (fallbackUrl == null) resource.message else null
                        )
                        
                        if (fallbackUrl != null) {
                            println("DEBUG: Auto-starting playback with fallback URL")
                            // Load available qualities for fallback URL too
                            loadStreamQualities(fallbackUrl)
                            // Apply current playback mode after URL is set
                            applyPlaybackModeForCurrentState()
                        }
                    }
                }
            }
        }
    }

    private fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
        socketConnected = false
        socketBuffer.clear()
    }

    private fun appendChatMessages(newOnes: List<ChatMessage>) {
        if (newOnes.isEmpty()) return
        val current = _state.value.chatMessages
        val ids = HashSet<String>(current.size * 2)
        current.forEach { ids.add(it.id) }
        val fresh = newOnes.filter { ids.add(it.id) }
        if (fresh.isEmpty()) return
        _state.value = _state.value.copy(chatMessages = (current + fresh).takeLast(chatPrefs.effectiveMaxMessages()))
    }

    // Live chat over WebSocket; messages are batched every 250ms so busy chats stay smooth
    private fun ensureSocket() {
        val room = chatroomId ?: return
        if (socketJob != null) return
        socketJob = viewModelScope.launch {
            launch {
                chatSocket.connect(room).collect { ev ->
                    when (ev) {
                        is KickChatSocket.Event.Connected -> socketConnected = true
                        is KickChatSocket.Event.Disconnected -> socketConnected = false
                        is KickChatSocket.Event.Message -> socketBuffer.add(ev.message)
                        is KickChatSocket.Event.Pinned -> _state.value = _state.value.copy(pinnedMessage = ev.message)
                    }
                }
            }
            while (true) {
                delay(chatPrefs.batchDelayMs())
                val batch = generateSequence { socketBuffer.poll() }.toList()
                if (batch.isNotEmpty()) appendChatMessages(batch)
            }
        }
    }

    private fun resetChatState() {
        stopSocket()
        chatroomId = null
        isFirstChatLoad = true
        latestMessageTimestamp = null
        _state.value = _state.value.copy(chatMessages = emptyList(), pinnedMessage = null)
    }
    
    private fun loadChatHistory(channelId: Long) {
        viewModelScope.launch {
            getChatHistoryUseCase(channelId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { chatResponse ->
                            val newMessages = chatResponse.messages
                            println("DEBUG: Chat loaded - ${newMessages.size} messages, first load: $isFirstChatLoad")
                            
                            if (isFirstChatLoad) {
                                // İlk yükleme - tüm mesajları created_at'e göre sırala
                                val sortedMessages = newMessages.sortedBy { it.createdAt }
                                println("DEBUG: First load - sorted ${sortedMessages.size} messages")
                                _state.value = _state.value.copy(
                                    chatMessages = sortedMessages.takeLast(MAX_CHAT_MESSAGES)
                                )
                                
                                // En son mesajın timestamp'ini kaydet
                                latestMessageTimestamp = sortedMessages.lastOrNull()?.createdAt
                                if (chatroomId == null) chatroomId = sortedMessages.lastOrNull()?.chatId?.takeIf { it > 0 }
                                println("DEBUG: Latest timestamp set to: $latestMessageTimestamp")
                                isFirstChatLoad = false
                            } else {
                                appendChatMessages(newMessages.sortedBy { it.createdAt })
                                if (chatroomId == null) chatroomId = newMessages.lastOrNull()?.chatId?.takeIf { it > 0 }
                            }
                            ensureSocket()
                        }
                    }
                    is Resource.Error -> {
                        println("DEBUG: Chat loading error: ${resource.message}")
                        // Chat errors are not critical, don't show to user
                    }
                    is Resource.Loading -> {
                        // Don't show chat loading
                    }
                }
            }
        }
    }

    fun onPlayPause() {
        val newPlaying = !_state.value.isPlaying
        _state.value = _state.value.copy(isPlaying = newPlaying)
        // Ensure playback toggles even when no VideoPlayer is present (e.g., AUDIO_WITH_CHAT portrait)
        when (_state.value.playbackMode) {
            PlaybackMode.AUDIO_WITH_CHAT -> {
                if (newPlaying) playerManager.play() else playerManager.pause()
            }
            else -> { /* NORMAL handled by VideoPlayer, CHAT_ONLY stays paused */ }
        }
    }

    fun onFullscreenToggle() {
        _state.value = _state.value.copy(
            isFullscreen = !_state.value.isFullscreen
        )
    }
    
    fun prepareForPipMode() {
        // First enter fullscreen if not already
        if (!_state.value.isFullscreen) {
            _state.value = _state.value.copy(isFullscreen = true)
        }
    }

    fun onPipModeEnter() {
        _state.value = _state.value.copy(
            isPipMode = true,
            showControls = false
        )
    }

    fun onPipModeExit() {
        _state.value = _state.value.copy(
            isPipMode = false,
            showControls = true
        )
    }

    fun onControlsToggle() {
        _state.value = _state.value.copy(
            showControls = !_state.value.showControls
        )
    }

    fun onMuteToggle() {
        _state.value = _state.value.copy(
            isMuted = !_state.value.isMuted
        )
    }

    fun seekToLive() {
        playerManager.seekToLive()
    }

    fun updateLiveStatus() {
        val isLiveBehind = playerManager.isLiveBehind()
        val liveBehindSeconds = playerManager.getLiveBehindSeconds()
        
        _state.value = _state.value.copy(
            isLiveBehind = isLiveBehind,
            liveBehindSeconds = liveBehindSeconds
        )
    }

    fun setBuffering(isBuffering: Boolean) {
        _state.value = _state.value.copy(isBuffering = isBuffering)
    }

    fun retry() {
        val channelSlug = _state.value.stream?.channel?.slug ?: _state.value.channelDetails?.slug
        if (channelSlug != null) {
            val mode = _state.value.playbackMode
            _state.value = PlayerState(playbackMode = mode)
            loadChannelData(channelSlug)
        }
    }

    fun onCleanup() {
        // Don't stop video when mini player is showing
        // Just hide controls and keep playing in background
        _state.value = _state.value.copy(
            showControls = false,
            isBuffering = false
            // Keep isPlaying = true to continue audio playback
        )
    }
    
    fun forceCleanup() {
        // Force cleanup when mini player is also closed
        _state.value = _state.value.copy(
            isPlaying = false,
            showControls = false,
            isBuffering = false
        )
    }

    // region Playback modes
    fun setPlaybackMode(mode: PlaybackMode) {
        val previous = _state.value.playbackMode
        if (previous == mode) return

        _state.value = _state.value.copy(playbackMode = mode)
        // Persist playback mode selection
        qualityPreferences.savePlaybackMode(mode.name)

        when (mode) {
            PlaybackMode.CHAT_ONLY -> {
                // Stop playback entirely and show chat only
                _state.value = _state.value.copy(isPlaying = false, isFullscreen = false)
                playerManager.pause()
                playerManager.setVideoEnabled(false)
            }
            PlaybackMode.AUDIO_WITH_CHAT -> {
                // Keep audio playing, hide video track
                _state.value = _state.value.copy(isFullscreen = false)
                playerManager.setVideoEnabled(false)
                // Ensure playback is on if we had a stream
                if (_state.value.streamUrl != null) {
                    // Cache current selected quality to restore on NORMAL
                    if (cachedNormalQuality == null) cachedNormalQuality = _state.value.selectedQuality

                    // Choose lowest bandwidth quality for minimal data usage
                    val lowQuality = _state.value.availableQualities
                        .filter { it.id != "auto" }
                        .minByOrNull { it.bandwidth }
                        ?: _state.value.selectedQuality

                    // Switch player to low quality without saving preference
                    _state.value.manifestUrl?.let { manifest ->
                        playerManager.setStreamQuality(lowQuality, manifest, PlayerManager.StreamType.LIVE_STREAM, savePreference = false)
                    }

                    // Keep audio playing
                    _state.value = _state.value.copy(isPlaying = true, selectedQuality = lowQuality)
                    playerManager.play()
                }
            }
            PlaybackMode.NORMAL -> {
                // Restore video and normal behavior
                playerManager.setVideoEnabled(true)
                // Restore previous quality if cached
                val restore = cachedNormalQuality
                if (restore != null && _state.value.manifestUrl != null) {
                    playerManager.setStreamQuality(restore, _state.value.manifestUrl!!, PlayerManager.StreamType.LIVE_STREAM, savePreference = false)
                    _state.value = _state.value.copy(selectedQuality = restore)
                }
                cachedNormalQuality = null
                // If we have a stream, auto-resume playback when leaving chat-only
                if (_state.value.streamUrl != null) {
                    _state.value = _state.value.copy(isPlaying = true)
                    playerManager.play()
                }
            }
        }
    }
    // endregion

    private fun checkFollowStatus(channelId: String) {
        viewModelScope.launch {
            isChannelFollowedUseCase(channelId).collect { isFollowed ->
                _state.value = _state.value.copy(isFollowed = isFollowed)
            }
        }
    }

    fun toggleFollow() {
        val currentState = _state.value
        val stream = currentState.stream
        val channelDetails = currentState.channelDetails
        
        val channelId = when {
            stream != null -> stream.channel.id.toString()
            channelDetails != null -> channelDetails.user.id.toString()
            else -> return
        }
        
        val username = when {
            stream != null -> stream.channel.username
            channelDetails != null -> channelDetails.user.username
            else -> ""
        }

        viewModelScope.launch {
            try {
                toggleFollowChannelUseCase(channelId, currentState.isFollowed, username)
                // State will be updated automatically through the flow from checkFollowStatus
            } catch (e: Exception) {
                // Handle error if needed
                println("DEBUG: Error toggling follow: ${e.message}")
            }
        }
    }
    
    private fun startChatAutoRefresh(channelId: Long) {
        // Cancel previous refresh job if exists
        chatRefreshJob?.cancel()
        currentChannelId = channelId
        
        chatRefreshJob = viewModelScope.launch {
            while (coroutineContext[Job]?.isActive == true && currentChannelId == channelId) {
                delay(1000)
                if (!socketConnected) loadChatHistory(channelId)
            }
        }
    }
    
    private fun stopChatAutoRefresh() {
        stopSocket()
        chatRefreshJob?.cancel()
        chatRefreshJob = null
        currentChannelId = null
    }

    fun setChatEnabled(enabled: Boolean) {
        val wasEnabled = _state.value.isChatEnabled
        if (wasEnabled == enabled) return

        _state.value = _state.value.copy(isChatEnabled = enabled)
        val channelId = _state.value.stream?.channel?.id
        if (!enabled) {
            // Stop refreshing and clear messages to reduce memory
            stopChatAutoRefresh()
            _state.value = _state.value.copy(chatMessages = emptyList())
        } else if (channelId != null) {
            // Re-load and resume auto refresh
            resetChatState()
            loadChatHistory(channelId)
            startChatAutoRefresh(channelId)
        }
    }
    
    private fun startLiveStatusRefresh() {
        // Cancel previous refresh job if exists
        liveStatusJob?.cancel()
        
        liveStatusJob = viewModelScope.launch {
            while (coroutineContext[Job]?.isActive == true) {
                delay(2000) // 2 saniyede bir güncelle
                updateLiveStatus()
            }
        }
    }
    
    private fun stopLiveStatusRefresh() {
        liveStatusJob?.cancel()
        liveStatusJob = null
    }
    
    fun loadStreamQualities(manifestUrl: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingQualities = true)
            
            try {
                val streamManifest = streamQualityRepository.getStreamQualities(manifestUrl)
                val savedQuality = qualityPreferences.getSavedQuality()
                
                // Try to find the saved quality in available qualities
                val selectedQuality = if (savedQuality.id != "auto") {
                    streamManifest.qualities.find { it.id == savedQuality.id }
                        ?: streamManifest.qualities.find { it.name == savedQuality.name }
                        ?: StreamQuality.AUTO
                } else {
                    StreamQuality.AUTO
                }
                
                _state.value = _state.value.copy(
                    availableQualities = streamManifest.qualities,
                    selectedQuality = selectedQuality,
                    isLoadingQualities = false
                )
                println("DEBUG: Loaded ${streamManifest.qualities.size} stream qualities")
                println("DEBUG: Selected quality: ${selectedQuality.name} (from preferences)")
                
                // If mode is non-normal, adjust behavior now that we know qualities
                when (_state.value.playbackMode) {
                    PlaybackMode.AUDIO_WITH_CHAT -> {
                        // Cache current selected to restore when back to NORMAL
                        if (cachedNormalQuality == null) cachedNormalQuality = selectedQuality
                        val lowQuality = streamManifest.qualities
                            .filter { it.id != "auto" }
                            .minByOrNull { it.bandwidth }
                            ?: selectedQuality
                        _state.value.manifestUrl?.let { manifest ->
                            playerManager.setStreamQuality(lowQuality, manifest, PlayerManager.StreamType.LIVE_STREAM, savePreference = false)
                        }
                        _state.value = _state.value.copy(selectedQuality = lowQuality)
                        playerManager.setVideoEnabled(false)
                        _state.value = _state.value.copy(isPlaying = true)
                        playerManager.play()
                    }
                    PlaybackMode.CHAT_ONLY -> {
                        playerManager.setVideoEnabled(false)
                        _state.value = _state.value.copy(isPlaying = false)
                        playerManager.pause()
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to load stream qualities: ${e.message}")
                _state.value = _state.value.copy(
                    availableQualities = listOf(StreamQuality.AUTO),
                    selectedQuality = StreamQuality.AUTO,
                    isLoadingQualities = false
                )
            }
        }
    }
    
    fun onQualitySelected(quality: StreamQuality) {
        val manifestUrl = _state.value.manifestUrl
        if (manifestUrl != null) {
            _state.value = _state.value.copy(selectedQuality = quality)
            // Save the selected quality preference
            qualityPreferences.saveSelectedQuality(quality)
            println("DEBUG: Quality selected and saved: ${quality.name}")
            // Quality change will be handled by VideoPlayer component
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopChatAutoRefresh()
        stopLiveStatusRefresh()
    }

    private fun applyPlaybackModeForCurrentState() {
        when (_state.value.playbackMode) {
            PlaybackMode.CHAT_ONLY -> {
                _state.value = _state.value.copy(isPlaying = false, isFullscreen = false)
                playerManager.pause()
                playerManager.setVideoEnabled(false)
            }
            PlaybackMode.AUDIO_WITH_CHAT -> {
                _state.value = _state.value.copy(isFullscreen = false)
                playerManager.setVideoEnabled(false)
                if (_state.value.streamUrl != null) {
                    _state.value = _state.value.copy(isPlaying = true)
                    playerManager.play()
                }
            }
            else -> Unit
        }
    }
}
