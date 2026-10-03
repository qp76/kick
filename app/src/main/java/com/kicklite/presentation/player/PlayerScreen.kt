package com.kicklite.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kicklite.R
import com.kicklite.presentation.player.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    channelSlug: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChat by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Load channel data when screen opens
    LaunchedEffect(channelSlug) {
        viewModel.loadChannelData(channelSlug)
    }

    // Notify MainActivity about video playing state
    LaunchedEffect(state.isPlaying, state.streamUrl) {
        val mainActivity = context as? com.kicklite.presentation.MainActivity
        mainActivity?.setVideoPlayingState(
            isPlaying = state.isPlaying && state.streamUrl != null,
            isOnPlayer = true
        )
        
        // Also update SharedDataViewModel for mini player
        mainActivity?.sharedDataViewModel?.setVideoState(
            isPlaying = state.isPlaying && state.streamUrl != null,
            streamUrl = state.streamUrl
        )
    }

    // Handle back button
    BackHandler {
        if (state.isFullscreen) {
            viewModel.onFullscreenToggle()
        } else {
            // Show mini player if stream is playing
            if (state.isPlaying && state.streamUrl != null && state.stream != null) {
                val mainActivity = context as? com.kicklite.presentation.MainActivity
                mainActivity?.sharedDataViewModel?.showMiniPlayer(state.stream!!)
                // Keep video playing when showing mini player - don't change state
                mainActivity?.setVideoPlayingState(true, false)
            } else {
                // Only clean up video state if not showing mini player
                val mainActivity = context as? com.kicklite.presentation.MainActivity
                mainActivity?.setVideoPlayingState(false, false)
            }
            // Don't call any cleanup - let video continue in background
            onBackClick()
        }
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
            state.isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.loading_stream),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            state.error != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading_streams),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.error ?: stringResource(R.string.error_unknown),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.retry() }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            else -> {
                // Video player
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
                    onLiveClick = { viewModel.seekToLive() },
                    modifier = Modifier.fillMaxSize()
                )

                // Chat overlay (positioned on the right side)
                if (!state.isFullscreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        ChatOverlay(
                            chatMessages = state.chatMessages,
                            isVisible = showChat && state.isChatEnabled,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onClose = { 
                                showChat = false
                                viewModel.setChatEnabled(false)
                            }
                        )
                    }

                    // Chat toggle button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (state.chatMessages.isNotEmpty() && state.isChatEnabled) {
                            ChatToggleButton(
                                isVisible = showChat,
                                onClick = { showChat = !showChat },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }

                    // Center "Show Chat" button when chat is disabled
                    if (!state.isChatEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { 
                                viewModel.setChatEnabled(true)
                                showChat = true
                            }) {
                                Text(stringResource(R.string.show_chat))
                            }
                        }
                    }
                }
            }
        }
    }
}
