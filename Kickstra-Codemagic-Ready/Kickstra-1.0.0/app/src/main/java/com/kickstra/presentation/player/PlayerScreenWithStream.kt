package com.kickstra.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kickstra.R
import com.kickstra.domain.model.LiveStream
import com.kickstra.presentation.MainActivity
import com.kickstra.presentation.player.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenWithStream(
    stream: LiveStream,
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    onFullscreenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Track player screen state in MainActivity
    DisposableEffect(Unit) {
        val mainActivity = context as? MainActivity
        // Note: We don't set isOnPlayerScreen here since we handle it differently
        // PlayerScreen is for live streams, we handle PiP differently
        onDispose {
            // Cleanup if needed
        }
    }

    // Initialize with stream data instead of loading from API
    LaunchedEffect(stream) {
        viewModel.initializeWithStream(stream)
    }

    // Notify MainActivity about video playing state
    LaunchedEffect(state.isPlaying, state.streamUrl) {
        val mainActivity = context as? MainActivity
        mainActivity?.setVideoPlayingState(
            isPlaying = state.isPlaying && state.streamUrl != null,
            isOnPlayer = true
        )
        
        // Also update SharedDataViewModel for mini player
        mainActivity?.sharedDataViewModel?.setVideoState(
            isPlaying = state.isPlaying && state.streamUrl != null,
            streamUrl = state.streamUrl
        )

        // Show/hide playback notification with live metadata
        if (state.isPlaying && state.streamUrl != null && state.stream != null) {
            mainActivity?.requestNotificationPermissionIfNeeded()
            mainActivity?.playbackNotificationManager?.showLive(state.stream!!)
        } else if (!state.isPlaying) {
            mainActivity?.playbackNotificationManager?.hide()
        }
    }

    // Handle back button
    BackHandler {
        if (state.isFullscreen) {
            viewModel.onFullscreenToggle()
            // Orientation will be handled by LaunchedEffect
        } else {
            // Show mini player if stream is playing
            if (state.isPlaying && state.streamUrl != null) {
                val mainActivity = context as? MainActivity
                mainActivity?.sharedDataViewModel?.showMiniPlayer(stream)
                // Keep video playing when showing mini player
                mainActivity?.setVideoPlayingState(true, false)
                // Don't cleanup video - let it continue playing
            } else {
                // Only clean up player state if not showing mini player
                val mainActivity = context as? MainActivity
                mainActivity?.setVideoPlayingState(false, false)
                viewModel.forceCleanup()
            }
            onBackClick()
        }
    }

    // Handle fullscreen orientation changes
    LaunchedEffect(state.isFullscreen) {
        onFullscreenChange(state.isFullscreen)
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(state.showControls) {
        if (state.showControls && !state.isBuffering) {
            kotlinx.coroutines.delay(3000)
            viewModel.onControlsToggle()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isPipMode -> {
                // Picture-in-Picture mode - video only without any controls or chat
                VideoPlayer(
                    streamUrl = state.streamUrl,
                    manifestUrl = state.manifestUrl,
                    selectedQuality = state.selectedQuality,
                    isPlaying = state.isPlaying,
                    isMuted = state.isMuted,
                    onBufferingChanged = { viewModel.setBuffering(it) },
                    onPlayerClick = { 
                        // On PiP click, exit PiP mode and return to normal player
                        // This will be handled by the system automatically
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            state.isLoading && state.streamUrl == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null && state.streamUrl == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: stringResource(R.string.error_unknown),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            else -> {
                if (state.isFullscreen) {
                    // Fullscreen mode - video only
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayer(
                            streamUrl = state.streamUrl,
                            manifestUrl = state.manifestUrl,
                            selectedQuality = state.selectedQuality,
                            isPlaying = state.isPlaying,
                            isMuted = state.isMuted,
                            showVideo = state.playbackMode == PlaybackMode.NORMAL,
                            onBufferingChanged = { viewModel.setBuffering(it) },
                            onPlayerClick = { viewModel.onControlsToggle() },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Fullscreen video controls
                        VideoPlayerControls(
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering,
                            showControls = state.showControls,
                            isFullscreen = state.isFullscreen,
                            stream = state.stream,
                            channelDetails = state.channelDetails,
                            isFollowed = state.isFollowed,
                            isMuted = state.isMuted,
                            isLiveBehind = state.isLiveBehind,
                            liveBehindSeconds = state.liveBehindSeconds,
                            availableQualities = state.availableQualities,
                            selectedQuality = state.selectedQuality,
                            onPlayPause = { viewModel.onPlayPause() },
                            onFullscreenToggle = { viewModel.onFullscreenToggle() },
                            onMuteToggle = { viewModel.onMuteToggle() },
                            onBackClick = onBackClick,
                            onFollowClick = { viewModel.toggleFollow() },
                            onQualitySelected = { quality ->
                                viewModel.setPlaybackMode(PlaybackMode.NORMAL)
                                viewModel.onQualitySelected(quality)
                            },
                            onSelectChatOnly = { viewModel.setPlaybackMode(PlaybackMode.CHAT_ONLY) },
                            onSelectAudioWithChat = { viewModel.setPlaybackMode(PlaybackMode.AUDIO_WITH_CHAT) },
                            currentMode = state.playbackMode,
                            onChannelClick = onChannelClick,
                            onLiveClick = { viewModel.seekToLive() },
                            alwaysShowBottomBar = state.playbackMode != PlaybackMode.NORMAL,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating quality/mode button in non-normal modes
                        if (state.playbackMode != PlaybackMode.NORMAL) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                QualitySelector(
                                    qualities = state.availableQualities,
                                    selectedQuality = state.selectedQuality,
                                    onQualitySelected = { q ->
                                        viewModel.setPlaybackMode(PlaybackMode.NORMAL)
                                        viewModel.onQualitySelected(q)
                                    },
                                    onSelectChatOnly = { viewModel.setPlaybackMode(PlaybackMode.CHAT_ONLY) },
                                    onSelectAudioWithChat = { viewModel.setPlaybackMode(PlaybackMode.AUDIO_WITH_CHAT) },
                                    currentMode = state.playbackMode
                                )
                            }
                        }
                    }
                } else {
                    when (state.playbackMode) {
                        PlaybackMode.CHAT_ONLY -> {
                            // Only chat visible, but keep a small top bar with QualitySelector to switch modes
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // No spinner in Chat-only (video is off by design)
                                    if (state.availableQualities.isNotEmpty()) {
                                        QualitySelector(
                                            qualities = state.availableQualities,
                                            selectedQuality = state.selectedQuality,
                                            onQualitySelected = { q ->
                                                viewModel.setPlaybackMode(PlaybackMode.NORMAL)
                                                viewModel.onQualitySelected(q)
                                            },
                                            onSelectChatOnly = { viewModel.setPlaybackMode(PlaybackMode.CHAT_ONLY) },
                                            onSelectAudioWithChat = { viewModel.setPlaybackMode(PlaybackMode.AUDIO_WITH_CHAT) },
                                            currentMode = state.playbackMode
                                        )
                                    }
                                }

                                ChatSection(
                                    chatMessages = state.chatMessages,
                                    channelName = state.stream?.channel?.username ?: stringResource(R.string.channel_default_name),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClose = {
                                        viewModel.setChatEnabled(false)
                                    }
                                )
                                if (!state.isChatEnabled) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Button(onClick = { viewModel.setChatEnabled(true) }) {
                                            Text(stringResource(R.string.show_chat))
                                        }
                                    }
                                }
                            }
                        }
                        PlaybackMode.AUDIO_WITH_CHAT -> {
                            // Top bar with LIVE + Quality, then chat below (no video area)
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // LIVE indicator / GO TO LIVE
                                        if (state.channelDetails?.livestream?.isLive == true || state.stream != null) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (state.isLiveBehind)
                                                        Color.Gray.copy(alpha = 0.8f)
                                                    else
                                                        Color.Red.copy(alpha = 0.8f)
                                                ),
                                                modifier = Modifier.clickable { viewModel.seekToLive() }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = if (state.isLiveBehind) stringResource(R.string.go_to_live) else stringResource(R.string.live),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                    if (state.isLiveBehind && state.liveBehindSeconds > 0) {
                                                        Text(
                                                            text = stringResource(R.string.live_latency_seconds, state.liveBehindSeconds),
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Play/Pause button for audio-only mode
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.onPlayPause() }) {
                                            Icon(
                                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                            )
                                        }
                                        // Audio-only: mimic normal player's initial loading (no URL yet)
                                        if (state.isLoading && state.streamUrl == null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (state.availableQualities.isNotEmpty()) {
                                            QualitySelector(
                                                qualities = state.availableQualities,
                                                selectedQuality = state.selectedQuality,
                                                onQualitySelected = { q ->
                                                    viewModel.setPlaybackMode(PlaybackMode.NORMAL)
                                                    viewModel.onQualitySelected(q)
                                                },
                                                onSelectChatOnly = { viewModel.setPlaybackMode(PlaybackMode.CHAT_ONLY) },
                                                onSelectAudioWithChat = { viewModel.setPlaybackMode(PlaybackMode.AUDIO_WITH_CHAT) },
                                                currentMode = state.playbackMode
                                            )
                                        }
                                    }
                                }

                                // Chat fills the rest
                                ChatSection(
                                    chatMessages = state.chatMessages,
                                    channelName = state.stream?.channel?.username ?: stringResource(R.string.channel_default_name),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClose = { viewModel.setChatEnabled(false) }
                                )
                                if (!state.isChatEnabled) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Button(onClick = { viewModel.setChatEnabled(true) }) {
                                            Text(stringResource(R.string.show_chat))
                                        }
                                    }
                                }
                            }
                        }
                        PlaybackMode.NORMAL -> {
                            // Portrait mode - video on top, chat on bottom
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Video player section (1/3 of screen)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    VideoPlayer(
                                        streamUrl = state.streamUrl,
                                        manifestUrl = state.manifestUrl,
                                        selectedQuality = state.selectedQuality,
                                        isPlaying = state.isPlaying,
                                        isMuted = state.isMuted,
                                        onBufferingChanged = { viewModel.setBuffering(it) },
                                        onPlayerClick = { viewModel.onControlsToggle() },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Video controls overlay
                                    VideoPlayerControls(
                                        isPlaying = state.isPlaying,
                                        isBuffering = state.isBuffering,
                                        showControls = state.showControls,
                                        isFullscreen = state.isFullscreen,
                                        stream = state.stream,
                                        channelDetails = state.channelDetails,
                                        isFollowed = state.isFollowed,
                                        isMuted = state.isMuted,
                                        isLiveBehind = state.isLiveBehind,
                                        liveBehindSeconds = state.liveBehindSeconds,
                                        availableQualities = state.availableQualities,
                                        selectedQuality = state.selectedQuality,
                                        onPlayPause = { viewModel.onPlayPause() },
                                        onFullscreenToggle = { viewModel.onFullscreenToggle() },
                                        onMuteToggle = { viewModel.onMuteToggle() },
                                        onBackClick = onBackClick,
                                        onFollowClick = { viewModel.toggleFollow() },
                                        onQualitySelected = { quality -> viewModel.onQualitySelected(quality) },
                                        onSelectChatOnly = { viewModel.setPlaybackMode(PlaybackMode.CHAT_ONLY) },
                                        onSelectAudioWithChat = { viewModel.setPlaybackMode(PlaybackMode.AUDIO_WITH_CHAT) },
                                        onChannelClick = onChannelClick,
                                        onLiveClick = { viewModel.seekToLive() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Chat section (2/3 of screen)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(2f)
                                ) {
                                    ChatSection(
                                        chatMessages = state.chatMessages,
                                        channelName = state.stream?.channel?.username ?: stringResource(R.string.channel_default_name),
                                        modifier = Modifier.fillMaxSize(),
                                        onClose = { viewModel.setChatEnabled(false) }
                                    )
                                    if (!state.isChatEnabled) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(onClick = { viewModel.setChatEnabled(true) }) {
                                                Text(stringResource(R.string.show_chat))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
