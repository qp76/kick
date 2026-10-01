package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChannelLivestream

data class ChannelLivestreamDto(
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
    val startTime: String?,
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
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("categories")
    val categories: List<StreamCategoryDto>
) {
    fun toDomainModel(): ChannelLivestream {
        return ChannelLivestream(
            id = id,
            slug = slug,
            channelId = channelId,
            createdAt = createdAt,
            sessionTitle = sessionTitle,
            isLive = isLive,
            startTime = startTime,
            duration = duration,
            language = language,
            isMature = isMature,
            viewerCount = viewerCount,
            thumbnail = thumbnail?.toDomainModel(),
            tags = tags,
            categories = categories.map { it.toDomainModel() }
        )
    }
}
