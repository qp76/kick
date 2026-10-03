package com.kicklite.presentation.video_player.components

import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayer(
    videoUrl: String?,
    resumePosition: Long = 0L,
    onResumePositionHandled: () -> Unit = {},
    availableQualities: List<com.kicklite.data.model.StreamQuality> = emptyList(),
    selectedQuality: com.kicklite.data.model.StreamQuality = com.kicklite.data.model.StreamQuality.AUTO,
    onQualitySelected: (com.kicklite.data.model.StreamQuality, Long) -> Unit = { _, _ -> },
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    isPipMode: Boolean = false,
    modifier: Modifier = Modifier,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPositionChanged: ((Long, Long) -> Unit)? = null, // currentPosition, duration
    onVideoCompleted: (() -> Unit)? = null, // called when video ends
    controlsPersistent: Boolean = false,
    showQualitySelector: Boolean = true
) {
    val context = LocalContext.current
    
    // Get PlayerManager from MainActivity
    val mainActivity = context as? com.kicklite.presentation.MainActivity
    val playerManager = mainActivity?.playerManager
    val exoPlayer = playerManager?.exoPlayer
    val window = mainActivity?.window

    fun shouldKeepScreenOn(player: Player?): Boolean = player?.let {
        it.playWhenReady &&
            (it.playbackState == Player.STATE_READY || it.playbackState == Player.STATE_BUFFERING)
    } == true

    fun updateKeepScreenOn(player: Player?) {
        if (window == null) return
        if (shouldKeepScreenOn(player)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Player state management
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var hasAutoPlayed by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Set video URL when component initializes or URL changes
    LaunchedEffect(videoUrl) {
        if (!videoUrl.isNullOrEmpty() && playerManager != null) {
            println("DEBUG: Setting video URL: $videoUrl")
            hasAutoPlayed = false // Reset auto play flag for new video
            
            playerManager.setStreamUrl(
                videoUrl, 
                com.kicklite.presentation.player.PlayerManager.StreamType.VIDEO
            )
            
            // Player ready callback
            exoPlayer?.let { player ->
                onPlayerReady?.invoke(player)
                // Re-apply playback speed when player is ready for new source
                player.playbackParameters = player.playbackParameters.withSpeed(playbackSpeed)
            }
        }
    }
    
    // Handle resume position after URL change
    LaunchedEffect(resumePosition, videoUrl) {
        if (resumePosition > 0L && !videoUrl.isNullOrEmpty() && exoPlayer != null) {
            println("DEBUG: Waiting for player to be ready, then seeking to position: $resumePosition ms")
            
            // Wait for the player to be ready
            var attempts = 0
            while (attempts < 50 && exoPlayer?.playbackState != Player.STATE_READY) {
                delay(100)
                attempts++
            }
            
            // Seek to the resume position and auto play
            if (exoPlayer?.playbackState == Player.STATE_READY) {
                println("DEBUG: Player ready, seeking to position: $resumePosition ms and starting auto-play")
                exoPlayer.seekTo(resumePosition)
                hasAutoPlayed = true // Mark as auto played
                exoPlayer.play() // Auto play after seeking
                onResumePositionHandled()
            } else {
                println("DEBUG: Player not ready after waiting, seeking anyway to position: $resumePosition ms")
                exoPlayer?.seekTo(resumePosition)
                hasAutoPlayed = true // Mark as auto played
                exoPlayer?.play() // Auto play after seeking
                onResumePositionHandled()
            }
        }
    }

    // Update position and duration
    LaunchedEffect(exoPlayer) {
        while (true) {
            exoPlayer?.let { player ->
                val newPosition = player.currentPosition
                val newDuration = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                
                currentPosition = newPosition
                duration = newDuration
                isPlaying = player.isPlaying
                
                // Notify position changes for saving watch history
                if (newDuration > 0L) {
                    onPositionChanged?.invoke(newPosition, newDuration)
                }
            }
            delay(500) // Update every 500ms
        }
    }

    // Auto-hide controls (disabled when controlsPersistent is true)
    LaunchedEffect(showControls, controlsPersistent, isPlaying, isBuffering) {
        if (!controlsPersistent && showControls && isPlaying && !isBuffering) {
            delay(3000) // Hide after 3 seconds
            showControls = false
        }
    }

    // Listen for player state changes
    DisposableEffect(playerManager, window) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        println("DEBUG: Player is buffering...")
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        println("DEBUG: Player is ready - checking auto-play (hasAutoPlayed: $hasAutoPlayed)")
                        
                        // Auto play when player is ready (only once per video)
                        exoPlayer?.let { player ->
                            if (!hasAutoPlayed && !player.isPlaying) {
                                println("DEBUG: Starting auto-play")
                                hasAutoPlayed = true
                                player.play()
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        println("DEBUG: Video playback ended - marking as completed")
                        onVideoCompleted?.invoke()
                    }
                    else -> {
                        isBuffering = false
                    }
                }
                onPlaybackStateChanged?.invoke(playbackState)
                updateKeepScreenOn(exoPlayer)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateKeepScreenOn(exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateKeepScreenOn(exoPlayer)
            }
        }
        
        playerManager?.addListener(listener)
        updateKeepScreenOn(exoPlayer)
        
        onDispose {
            playerManager?.removeListener(listener)
            if (!shouldKeepScreenOn(exoPlayer)) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        // Player view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Disable default controls
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { 
                    showControls = !showControls 
                },
            update = { view ->
                view.player = exoPlayer
            }
        )

        // Custom controls overlay - hide in PiP mode
        if (!isPipMode) {
            VideoPlayerControls(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            showControls = showControls,
            currentPosition = currentPosition,
            duration = duration,
            availableQualities = availableQualities,
            selectedQuality = selectedQuality,
            showQualitySelector = showQualitySelector,
            playbackSpeed = playbackSpeed,
            onPlayPause = {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
            },
            onSeekTo = { position ->
                exoPlayer?.seekTo(position)
            },
            onSeekBack = {
                exoPlayer?.let { player ->
                    val newPosition = (player.currentPosition - 15000).coerceAtLeast(0)
                    player.seekTo(newPosition)
                }
            },
            onSeekForward = {
                exoPlayer?.let { player ->
                    val newPosition = (player.currentPosition + 15000).coerceAtMost(player.duration)
                    player.seekTo(newPosition)
                }
            },
            onQualitySelected = { quality ->
                // Get current position before quality change
                val currentPos = exoPlayer?.currentPosition ?: 0L
                println("DEBUG: Quality change requested at position: $currentPos ms")
                onQualitySelected(quality, currentPos)
            },
            onSpeedSelected = { speed ->
                playbackSpeed = speed
                exoPlayer?.let { player ->
                    val params = player.playbackParameters
                    player.playbackParameters = params.withSpeed(speed)
                }
            },
            onControlsToggle = {
                showControls = !showControls
            },
            onFullscreenToggle = onFullscreenToggle,
            isFullscreen = isFullscreen,
            modifier = Modifier.fillMaxSize()
        )
        }
    }
}
