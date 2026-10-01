package com.kickstra.domain.model

data class ChannelVideo(
    val id: Long,
    val slug: String,
    val channelId: Long,
    val createdAt: String,
    val sessionTitle: String,
    val isLive: Boolean,
    val startTime: String,
    val source: String?,
    val duration: Int,
    val language: String,
    val isMature: Boolean,
    val viewerCount: Int,
    val thumbnail: StreamThumbnail?,
    val views: Int,
    val video: VideoDetails?,
    val categories: List<StreamCategory>
)

data class VideoDetails(
    val id: Long,
    val liveStreamId: Long,
    val slug: String?,
    val uuid: String,
    val views: Int,
    val isPruned: Boolean,
    val isPrivate: Boolean,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
