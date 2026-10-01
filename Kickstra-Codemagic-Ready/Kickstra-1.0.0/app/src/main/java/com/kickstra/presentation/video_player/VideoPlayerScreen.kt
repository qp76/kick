package com.kickstra.presentation.video_player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import com.kickstra.R
import com.kickstra.domain.model.ChannelVideo
import com.kickstra.presentation.video_player.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    username: String,
    onNavigateBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit = {},
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableQualities by viewModel.availableQualities.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val resumePosition by viewModel.resumePosition.collectAsState()
    val context = LocalContext.current
    val errorMessage = uiState.errorMessageRes?.let { resId ->
        if (uiState.errorMessageArgs.isEmpty()) {
            stringResource(resId)
        } else {
            stringResource(resId, *uiState.errorMessageArgs.toTypedArray())
        }
    }

    val activity = LocalContext.current as? com.kickstra.presentation.MainActivity
    val launchIntent = activity?.intent
    var placeholderTitle by remember { mutableStateOf<String?>(null) }
    var placeholderThumb by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(videoId, username) {
        // If cold start from notification we may have extras
        if (uiState.video == null) {
            placeholderTitle = launchIntent?.getStringExtra("video_title")
            placeholderThumb = launchIntent?.getStringExtra("video_thumbnail")
        }
        viewModel.loadVideo(videoId, username)
    }
    
    // Handle PiP mode changes
    LaunchedEffect(uiState.isPipModeEnabled) {
        if (uiState.isPipModeEnabled) {
            println("DEBUG: VideoPlayerScreen - PiP mode enabled")
        } else {
            println("DEBUG: VideoPlayerScreen - PiP mode disabled")
        }
    }
    
    // Handle back button - show mini player if video is playing
    BackHandler {
        if (uiState.isFullscreen) {
            viewModel.onFullscreenToggle()
        } else {
            val mainActivity = context as? com.kickstra.presentation.MainActivity
            val sharedDataViewModel = mainActivity?.sharedDataViewModel
            val playerManager = mainActivity?.playerManager
            
            // If video is currently playing, show mini player
            if (uiState.video != null && playerManager?.isPlaying() == true) {
                sharedDataViewModel?.showMiniPlayerForVideo(uiState.video!!, username)
                // Keep notification while playing
                mainActivity?.playbackNotificationManager?.showVideo(uiState.video!!, username)
            }
            
            onNavigateBack()
        }
    }

    // Handle fullscreen orientation changes
    LaunchedEffect(uiState.isFullscreen) {
        onFullscreenChange(uiState.isFullscreen)
    }

    if (uiState.isFullscreen || uiState.isPipModeEnabled) {
        // Fullscreen mode or PiP mode - no scaffold, just the content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (uiState.isPipModeEnabled) {
                // PiP mode - show only video, even if loading or error
                if (uiState.video != null) {
                    VideoPlayerContent(
                        video = uiState.video!!,
                        currentVideoUrl = currentVideoUrl,
                        resumePosition = resumePosition,
                        onResumePositionHandled = { viewModel.clearResumePosition() },
                        availableQualities = availableQualities,
                        selectedQuality = selectedQuality,
                        onQualitySelected = { quality, position -> viewModel.onQualitySelected(quality, position) },
                        onPositionChanged = { currentPos, duration -> viewModel.saveCurrentWatchPosition(currentPos, duration) },
                        onVideoCompleted = { viewModel.markVideoAsCompleted() },
                        onFullscreenToggle = { viewModel.onFullscreenToggle() },
                        isFullscreen = uiState.isFullscreen,
                        isPipMode = uiState.isPipModeEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // In PiP mode, don't show loading or error UI - just video or black screen
            } else {
                // Fullscreen mode - show normal UI states
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { viewModel.loadVideo(videoId, username) }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                    uiState.video != null -> {
                        val video = uiState.video
                        // Show notification when video content is visible; toggle visibility based on player state later
                        val mainActivity = context as? com.kickstra.presentation.MainActivity
                        if (video != null) {
                            mainActivity?.requestNotificationPermissionIfNeeded()
                            mainActivity?.playbackNotificationManager?.showVideo(video, username)
                        }
                        VideoPlayerContent(
                            video = video!!,
                            currentVideoUrl = currentVideoUrl,
                            resumePosition = resumePosition,
                            onResumePositionHandled = { viewModel.clearResumePosition() },
                            availableQualities = availableQualities,
                            selectedQuality = selectedQuality,
                            onQualitySelected = { quality, position -> viewModel.onQualitySelected(quality, position) },
                            onPositionChanged = { currentPos, duration -> viewModel.saveCurrentWatchPosition(currentPos, duration) },
                            onVideoCompleted = { viewModel.markVideoAsCompleted() },
                            onFullscreenToggle = { viewModel.onFullscreenToggle() },
                            isFullscreen = uiState.isFullscreen,
                            isPipMode = uiState.isPipModeEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    } else {
        // Normal mode with scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            val mainActivity = context as? com.kickstra.presentation.MainActivity
                            val sharedDataViewModel = mainActivity?.sharedDataViewModel
                            val playerManager = mainActivity?.playerManager
                            
                            // If video is currently playing, show mini player
                            if (uiState.video != null && playerManager?.isPlaying() == true) {
                                sharedDataViewModel?.showMiniPlayerForVideo(uiState.video!!, username)
                                mainActivity?.playbackNotificationManager?.showVideo(uiState.video!!, username)
                            }
                            
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    if (placeholderTitle != null || placeholderThumb != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            placeholderThumb?.let { thumb ->
                                Image(
                                    painter = rememberAsyncImagePainter(thumb),
                                    contentDescription = placeholderTitle ?: stringResource(R.string.thumbnail_content_description),
                                    modifier = Modifier
                                        .height(180.dp)
                                        .fillMaxWidth()
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            Text(
                                text = placeholderTitle ?: stringResource(R.string.loading_generic),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadVideo(videoId, username) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                uiState.video != null -> {
                    VideoPlayerContent(
                        video = uiState.video!!,
                        currentVideoUrl = currentVideoUrl,
                        resumePosition = resumePosition,
                        onResumePositionHandled = { viewModel.clearResumePosition() },
                        availableQualities = availableQualities,
                        selectedQuality = selectedQuality,
                        onQualitySelected = { quality, position -> viewModel.onQualitySelected(quality, position) },
                        onPositionChanged = { currentPos, duration -> viewModel.saveCurrentWatchPosition(currentPos, duration) },
                        onVideoCompleted = { viewModel.markVideoAsCompleted() },
                        onFullscreenToggle = { viewModel.onFullscreenToggle() },
                        isFullscreen = uiState.isFullscreen,
                        isPipMode = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun VideoPlayerContent(
    video: ChannelVideo,
    currentVideoUrl: String?,
    resumePosition: Long,
    onResumePositionHandled: () -> Unit,
    availableQualities: List<com.kickstra.data.model.StreamQuality>,
    selectedQuality: com.kickstra.data.model.StreamQuality,
    onQualitySelected: (com.kickstra.data.model.StreamQuality, Long) -> Unit,
    onPositionChanged: (Long, Long) -> Unit = { _, _ -> },
    onVideoCompleted: () -> Unit = {},
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    isPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPlayerReady by remember { mutableStateOf(false) }
    
    if (isFullscreen || isPipMode) {
        // Fullscreen mode or PiP mode - video only
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            VideoPlayer(
                videoUrl = currentVideoUrl ?: video.source ?: "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                resumePosition = resumePosition,
                onResumePositionHandled = onResumePositionHandled,
                availableQualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = onQualitySelected,
                onFullscreenToggle = onFullscreenToggle,
                isFullscreen = isFullscreen,
                isPipMode = isPipMode,
                modifier = Modifier.fillMaxSize(),
                onPlayerReady = { _ ->
                    // Player hazır olduğunda yapılacak işlemler
                    isPlayerReady = true
                },
                onPlaybackStateChanged = { playbackState ->
                    // Playback state değişikliklerini handle et
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPlayerReady = true
                        }
                        else -> {
                            // Handle other states if needed
                        }
                    }
                },
                onPositionChanged = onPositionChanged,
                onVideoCompleted = onVideoCompleted
            )
        }
    } else {
        // Normal mode with details
        Column(
            modifier = modifier
        ) {
            // Video Player with custom controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
            VideoPlayer(
                videoUrl = currentVideoUrl ?: video.source ?: "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                resumePosition = resumePosition,
                onResumePositionHandled = onResumePositionHandled,
                availableQualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = onQualitySelected,
                onFullscreenToggle = onFullscreenToggle,
                isFullscreen = isFullscreen,
                isPipMode = false,
                modifier = Modifier.fillMaxSize(),
                onPlayerReady = { _ ->
                    // Player hazır olduğunda yapılacak işlemler
                    isPlayerReady = true
                },
                onPlaybackStateChanged = { playbackState ->
                    // Playback state değişikliklerini handle et
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPlayerReady = true
                        }
                        else -> {
                            // Handle other states if needed
                        }
                    }
                },
                onPositionChanged = onPositionChanged,
                onVideoCompleted = onVideoCompleted
            )
        }

        // Video Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Title
            Text(
                text = video.sessionTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Video Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.views_count, video.views),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = video.createdAt,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Video Categories
            if (video.categories.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.video_categories_list,
                        video.categories.joinToString(", ") { it.name }
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        }
    }
}
