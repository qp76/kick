package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.LiveStream

data class LiveStreamDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("viewer_count")
    val viewerCount: Int,
    @SerializedName("thumbnail")
    val thumbnail: StreamThumbnailDto?,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("channel")
    val channel: StreamChannelDto,
    @SerializedName("category")
    val category: StreamCategoryDto,
    @SerializedName("language")
    val language: String,
    @SerializedName("is_mature")
    val isMature: Boolean,
    @SerializedName("tags")
    val tags: List<String>
) {
    fun toDomainModel(): LiveStream {
        return LiveStream(
            id = id,
            title = title,
            viewerCount = viewerCount,
            thumbnail = thumbnail?.toDomainModel(),
            startTime = startTime,
            channel = channel.toDomainModel(),
            category = category.toDomainModel(),
            language = language,
            isMature = isMature,
            tags = tags
        )
    }
}
