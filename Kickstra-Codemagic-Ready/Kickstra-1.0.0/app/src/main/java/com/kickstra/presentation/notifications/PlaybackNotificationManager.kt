package com.kickstra.presentation.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.kickstra.R
import com.kickstra.domain.model.ChannelVideo
import com.kickstra.domain.model.LiveStream
import com.kickstra.presentation.ExpandNowPlayingActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.kickstra.presentation.player.PlayerManager
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.common.Player
import coil.Coil
import coil.request.ImageRequest
import androidx.core.graphics.drawable.toBitmap

@Singleton
class PlaybackNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerManager: PlayerManager
) {
    private val channelId = "playback_channel"
    private val notificationId = 1001
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var largeIconCacheUrl: String? = null
    private var largeIconCache: android.graphics.Bitmap? = null

    private sealed class NowPlayingMeta {
        data class Live(val stream: LiveStream): NowPlayingMeta()
        data class Video(val video: ChannelVideo, val username: String): NowPlayingMeta()
    }

    private var lastMeta: NowPlayingMeta? = null

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name, context.getString(R.string.app_name))
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun canPost(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, ExpandNowPlayingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_now_playing", true)
            when (val meta = lastMeta) {
                is NowPlayingMeta.Live -> {
                    putExtra("np_type", "live")
                    val s = meta.stream
                    putExtra("live_id", s.id)
                    putExtra("live_title", s.title)
                    putExtra("live_viewers", s.viewerCount)
                    putExtra("live_thumbnail", s.thumbnail?.src)
                    putExtra("live_start", s.startTime)
                    putExtra("live_channel_id", s.channel.id) // long extra
                    putExtra("live_channel_slug", s.channel.slug)
                    putExtra("live_channel_username", s.channel.username)
                    putExtra("live_channel_profile", s.channel.profilePic)
                    putExtra("live_category_id", s.category.id)
                    putExtra("live_category_name", s.category.name)
                    putExtra("live_category_slug", s.category.slug)
                    putExtra("live_language", s.language)
                    putExtra("live_is_mature", s.isMature)
                    putExtra("live_tags", s.tags.joinToString(","))
                }
                is NowPlayingMeta.Video -> {
                    putExtra("np_type", "video")
                    val v = meta.video
                    putExtra("video_id", v.id)
                    putExtra("video_username", meta.username)
                    putExtra("video_title", v.sessionTitle)
                    putExtra("video_views", v.views)
                    putExtra("video_duration", v.duration)
                    putExtra("video_thumbnail", v.thumbnail?.src)
                    putExtra("video_is_mature", v.isMature)
                    putExtra("video_language", v.language)
                    putExtra("video_created_at", v.createdAt)
                }
                null -> { /* no-op */ }
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun ensurePlayerNotificationManager() {
        if (playerNotificationManager != null) return
        playerNotificationManager = PlayerNotificationManager.Builder(context, notificationId, channelId)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return when (val meta = lastMeta) {
                        is NowPlayingMeta.Live -> meta.stream.title.ifBlank { context.getString(R.string.app_name) }
                        is NowPlayingMeta.Video -> meta.video.sessionTitle.ifBlank { context.getString(R.string.app_name) }
                        null -> context.getString(R.string.app_name)
                    }
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? = mainActivityPendingIntent()

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return when (val meta = lastMeta) {
                        is NowPlayingMeta.Live -> context.getString(
                            R.string.notification_live_content_text,
                            meta.stream.channel.username
                        )
                        is NowPlayingMeta.Video -> meta.username
                        null -> null
                    }
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): android.graphics.Bitmap? {
                    val url: String? = when (val meta = lastMeta) {
                        is NowPlayingMeta.Live -> meta.stream.channel.profilePic.ifBlank { meta.stream.thumbnail?.src ?: "" }
                        is NowPlayingMeta.Video -> meta.video.thumbnail?.src
                        null -> null
                    }?.takeIf { it.isNotBlank() }

                    if (url == null) return null
                    // Return from cache if available
                    if (largeIconCacheUrl == url && largeIconCache != null) {
                        return largeIconCache
                    }

                    largeIconCacheUrl = url
                    // Load asynchronously; callback will update the notification icon
                    try {
                        val loader = Coil.imageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .size(256)
                            .target(
                                onSuccess = { drawable ->
                                    val bmp = drawable.toBitmap()
                                    if (bmp != null && largeIconCacheUrl == url) {
                                        largeIconCache = bmp
                                        callback.onBitmap(bmp)
                                    }
                                }
                            )
                            .build()
                        loader.enqueue(request)
                    } catch (_: Exception) {
                        // Ignore icon failures
                    }
                    return null
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(notificationId: Int, notification: android.app.Notification, ongoing: Boolean) {
                    // no-op
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    // Keep state; we'll rebuild on next playback
                }
            })
            .build().apply {
                setSmallIcon(R.mipmap.ic_launcher)
            }
    }

    fun showLive(stream: LiveStream) {
        lastMeta = NowPlayingMeta.Live(stream)
        if (!canPost()) return
        ensurePlayerNotificationManager()
        playerNotificationManager?.setPlayer(playerManager.exoPlayer)
        playerNotificationManager?.invalidate()
    }

    fun showVideo(video: ChannelVideo, username: String) {
        lastMeta = NowPlayingMeta.Video(video, username)
        if (!canPost()) return
        ensurePlayerNotificationManager()
        playerNotificationManager?.setPlayer(playerManager.exoPlayer)
        playerNotificationManager?.invalidate()
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            hide()
            return
        }
        // If playing resumed, show last known meta again
        when (val meta = lastMeta) {
            is NowPlayingMeta.Live -> showLive(meta.stream)
            is NowPlayingMeta.Video -> showVideo(meta.video, meta.username)
            null -> Unit
        }
    }

    fun hide() {
        playerNotificationManager?.setPlayer(null)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun refresh() {
        // Rebuild with current state and last meta
        when (val meta = lastMeta) {
            is NowPlayingMeta.Live -> showLive(meta.stream)
            is NowPlayingMeta.Video -> showVideo(meta.video, meta.username)
            null -> if (!playerManager.isPlaying()) hide() else Unit
        }
    }
}
