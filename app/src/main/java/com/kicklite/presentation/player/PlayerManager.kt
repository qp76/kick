package com.kicklite.presentation.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import com.kicklite.data.model.StreamQuality
import com.kicklite.data.local.QualityPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@UnstableApi
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qualityPreferences: QualityPreferences,
    private val okHttpClient: OkHttpClient
) {
    private var _exoPlayer: ExoPlayer? = null
    private var _trackSelector: DefaultTrackSelector? = null
    private var currentStreamUrl: String? = null
    private var currentQuality: StreamQuality? = null
    private var currentStreamType: StreamType = StreamType.NONE
    
    // Callback for state updates
    private var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    
    enum class StreamType {
        NONE,
        LIVE_STREAM,
        VIDEO
    }
    
    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _trackSelector = DefaultTrackSelector(context).also { applyDataSaver(it) }
                
                // Configure load control for live streaming with reduced latency
                val loadControl = DefaultLoadControl.Builder()
                    // Minimum buffer - 5 seconds less than default (usually 2.5s for live)
                    .setBufferDurationsMs(
                        50 * 1000,      // min buffer duration: 50 seconds (default: 50s)
                        360 * 1000,     // max buffer duration: 360 seconds (default: 360s)  
                        2500,           // buffer for playback: 2.5 seconds (default: 2.5s)
                        5000            // buffer for playback after rebuffer: 5 seconds (default: 5s)
                    )
                    // Reduce target buffer length for live streams to minimize delay
                    .setTargetBufferBytes(C.LENGTH_UNSET)
                    .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based thresholds for live content
                    .build()
                
                _exoPlayer = ExoPlayer.Builder(context)
                    .setTrackSelector(_trackSelector!!)
                    .setLoadControl(loadControl)
                    .build().apply {
                        // Enable background playback
                        setWakeMode(C.WAKE_MODE_NETWORK)
                        
                        // Add player listener for state changes
                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                super.onIsPlayingChanged(isPlaying)
                                onPlaybackStateChanged?.invoke(isPlaying)
                            }
                        })
                    }
            }
            return _exoPlayer!!
        }
    
    val trackSelector: DefaultTrackSelector?
        get() = _trackSelector
    
    fun setStreamUrl(streamUrl: String?, streamType: StreamType = StreamType.LIVE_STREAM) {
        if (streamUrl != null && streamUrl != currentStreamUrl) {
            // Check if we need to stop current playback
            val needsStop = when {
                currentStreamType == StreamType.NONE -> false
                currentStreamType != streamType -> true // Different stream types
                currentStreamUrl != null && currentStreamUrl != streamUrl -> true // Different URLs
                else -> false
            }
            
            if (needsStop) {
                println("DEBUG: Stopping current ${currentStreamType.name} to play new ${streamType.name}")
                stop()
            }
            
            currentStreamUrl = streamUrl
            currentStreamType = streamType
            
            // Create HTTP data source with proper headers
            val defaultRequestHeaders = mapOf(
                "Origin" to "https://kick.com",
                "Referer" to "https://kick.com/",
                "Accept" to "*/*"
            )

            val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setDefaultRequestProperties(defaultRequestHeaders)
            
            // Create HLS media source for m3u8 streams with live streaming optimizations
            val hlsMediaSourceFactory = HlsMediaSource.Factory(httpDataSourceFactory)
            
            // Apply live streaming optimizations for reduced latency
            if (streamType == StreamType.LIVE_STREAM) {
                hlsMediaSourceFactory
                    .setAllowChunklessPreparation(true) // Allow preparation without downloading full chunks
                    .setUseSessionKeys(false) // Disable session keys for faster startup
            }
            
            val mediaSource = hlsMediaSourceFactory.createMediaSource(MediaItem.fromUri(streamUrl))
            
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            
            // For live streams, seek closer to live edge to reduce initial delay
            if (streamType == StreamType.LIVE_STREAM) {
                // Wait for the player to be ready, then seek to a position 5 seconds back from live edge
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val duration = exoPlayer.duration
                            if (duration != C.TIME_UNSET && duration > 5000) {
                                // Seek to 5 seconds before the end (live edge - 5 seconds)
                                val seekPosition = duration - 5000
                                exoPlayer.seekTo(seekPosition)
                                println("DEBUG: Live stream - seeking to position: ${seekPosition}ms (${seekPosition/1000}s before live edge)")
                            }
                            exoPlayer.removeListener(this)
                        }
                    }
                }
                exoPlayer.addListener(listener)
            }
        }
    }
    
    fun setStreamQuality(
        quality: StreamQuality,
        manifestUrl: String,
        streamType: StreamType = StreamType.LIVE_STREAM,
        savePreference: Boolean = true
    ) {
        currentQuality = quality
        // Save the selected quality only if requested
        if (savePreference) {
            qualityPreferences.saveSelectedQuality(quality)
        }
        
        val urlToUse = if (quality.id == "auto") {
            manifestUrl
        } else {
            quality.url
        }
        
        setStreamUrl(urlToUse, streamType)
    }
    
    fun play() {
        exoPlayer.play()
        onPlaybackStateChanged?.invoke(true)
    }
    
    fun pause() {
        exoPlayer.pause()
        onPlaybackStateChanged?.invoke(false)
    }
    
    fun stop() {
        exoPlayer.stop()
        currentStreamUrl = null
        currentStreamType = StreamType.NONE
    }
    
    fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    @Volatile private var dataSaver = false

    fun setDataSaver(enabled: Boolean) {
        dataSaver = enabled
        _trackSelector?.let { applyDataSaver(it) }
    }

    private fun applyDataSaver(selector: DefaultTrackSelector) {
        val b = selector.parameters.buildUpon()
        if (dataSaver) b.setMaxVideoSize(854, 480) else b.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        selector.setParameters(b)
    }

    /**
     * Enable/disable video rendering while keeping audio tracks.
     * Disabling video selects no video track in the track selector.
     */
    fun setVideoEnabled(enabled: Boolean) {
        val selector = _trackSelector ?: return
        val parametersBuilder = selector.parameters.buildUpon()
        parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        if (!enabled) {
            // Disable video track type so only audio plays
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        } else {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        }
        selector.setParameters(parametersBuilder)
    }
    
    fun seekToLive() {
        if (exoPlayer.isCurrentMediaItemLive) {
            // For live streams, seek to the live edge (current position)
            val duration = exoPlayer.duration
            if (duration != C.TIME_UNSET && duration > 0) {
                // Seek to 2 seconds before live edge to ensure smooth playback and reduce buffering
                val seekPosition = maxOf(0, duration - 2000)
                exoPlayer.seekTo(seekPosition)
            } else {
                // If we can't determine duration, seek to default live position
                exoPlayer.seekToDefaultPosition()
            }
        }
    }
    
    fun isLiveBehind(): Boolean {
        if (!exoPlayer.isCurrentMediaItemLive) return false
        val currentPosition = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0) return false
        
        // Consider "behind" if more than 10 seconds behind live edge
        val behindThreshold = 10000 // 10 seconds
        val liveEdge = duration
        return (liveEdge - currentPosition) > behindThreshold
    }
    
    fun getLiveBehindSeconds(): Long {
        if (!exoPlayer.isCurrentMediaItemLive) return 0
        val currentPosition = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0) return 0
        
        val behindMs = maxOf(0, duration - currentPosition)
        return behindMs / 1000 // Convert to seconds
    }
    
    fun addListener(listener: Player.Listener) {
        exoPlayer.addListener(listener)
    }
    
    fun removeListener(listener: Player.Listener) {
        exoPlayer.removeListener(listener)
    }
    
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        _trackSelector = null
        currentStreamUrl = null
        currentQuality = null
        currentStreamType = StreamType.NONE
    }
    
    fun getCurrentStreamUrl(): String? = currentStreamUrl
    
    fun getCurrentQuality(): StreamQuality? = currentQuality
    
    fun getCurrentStreamType(): StreamType = currentStreamType
    
    fun isPlaying(): Boolean = _exoPlayer?.isPlaying ?: false
    
    fun setPlaybackStateCallback(callback: (Boolean) -> Unit) {
        onPlaybackStateChanged = callback
    }
}
