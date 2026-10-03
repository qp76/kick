package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChannelVideo

data class ChannelVideoDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("channel_id")
    val channelId: Long,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("session_title")
    val sessionTitle: String,
    @SerializedName("is_live")
    val isLive: Boolean,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("source")
    val source: String?,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("language")
    val language: String,
    @SerializedName("is_mature")
    val isMature: Boolean,
    @SerializedName("viewer_count")
    val viewerCount: Int,
    @SerializedName("thumbnail")
    val thumbnail: StreamThumbnailDto?,
    @SerializedName("views")
    val views: Int,
    @SerializedName("video")
    val video: VideoDetailsDto?,
    @SerializedName("categories")
    val categories: List<StreamCategoryDto>
) {
    fun toDomainModel(): ChannelVideo {
        val durationSeconds = when {
            duration <= 0 -> 0
            duration < 1000 -> 1
            else -> duration / 1000
        }

        return ChannelVideo(
            id = id,
            slug = slug,
            channelId = channelId,
            createdAt = createdAt,
            sessionTitle = sessionTitle,
            isLive = isLive,
            startTime = startTime,
            source = source,
            duration = durationSeconds,
            language = language,
            isMature = isMature,
            viewerCount = viewerCount,
            thumbnail = thumbnail?.toDomainModel(),
            views = views,
            video = video?.toDomainModel(),
            categories = categories.map { it.toDomainModel() }
        )
    }
}

data class VideoDetailsDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("live_stream_id")
    val liveStreamId: Long,
    @SerializedName("slug")
    val slug: String?,
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("views")
    val views: Int,
    @SerializedName("is_pruned")
    val isPruned: Boolean,
    @SerializedName("is_private")
    val isPrivate: Boolean,
    @SerializedName("status")
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
) {
    fun toDomainModel(): com.kicklite.domain.model.VideoDetails {
        return com.kicklite.domain.model.VideoDetails(
            id = id,
            liveStreamId = liveStreamId,
            slug = slug,
            uuid = uuid,
            views = views,
            isPruned = isPruned,
            isPrivate = isPrivate,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
