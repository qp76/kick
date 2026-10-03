package com.kicklite.data.preview

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.kicklite.domain.common.Resource
import com.kicklite.domain.usecase.GetChannelDetailsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One tiny muted player used for the live preview inside a Home card.
 * Only one preview exists at a time, capped at 240p, and it is released as soon as it stops.
 */
@Singleton
@OptIn(UnstableApi::class)
class StreamPreviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val getChannelDetails: GetChannelDetailsUseCase
) {
    private var exo: ExoPlayer? = null
    private var loadedSlug: String? = null
    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    suspend fun start(slug: String) {
        if (loadedSlug == slug && exo != null) return
        val url = try {
            val result = getChannelDetails(slug).first { it is Resource.Success || it is Resource.Error }
            result.data?.playbackUrl
        } catch (e: Exception) {
            null
        } ?: return

        withContext(Dispatchers.Main) {
            release()
            val selector = DefaultTrackSelector(context).apply {
                setParameters(buildUponParameters().setMaxVideoSize(426, 240))
            }
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2000, 6000, 1000, 1500)
                .build()
            val player = ExoPlayer.Builder(context)
                .setTrackSelector(selector)
                .setLoadControl(loadControl)
                .build()
            player.volume = 0f
            val source = HlsMediaSource.Factory(OkHttpDataSource.Factory(okHttpClient))
                .createMediaSource(MediaItem.fromUri(url))
            player.setMediaSource(source)
            player.playWhenReady = true
            player.prepare()
            exo = player
            loadedSlug = slug
            _player.value = player
        }
    }

    /** Call on the main thread (e.g. from onDispose). */
    fun stopNow() {
        release()
    }

    suspend fun stop() {
        withContext(Dispatchers.Main) { release() }
    }

    private fun release() {
        _player.value = null
        exo?.release()
        exo = null
        loadedSlug = null
    }
}
