package com.kicklite.domain.model

data class ChannelLivestream(
    val id: Long,
    val slug: String,
    val channelId: Long,
    val createdAt: String,
    val sessionTitle: String,
    val isLive: Boolean,
    val startTime: String?,
    val duration: Int,
    val language: String,
    val isMature: Boolean,
    val viewerCount: Int,
    val thumbnail: StreamThumbnail?,
    val tags: List<String>,
    val categories: List<StreamCategory>
)
