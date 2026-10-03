package com.kicklite.domain.model

data class LiveStream(
    val id: String,
    val title: String,
    val viewerCount: Int,
    val thumbnail: StreamThumbnail?,
    val startTime: String,
    val channel: StreamChannel,
    val category: StreamCategory,
    val language: String,
    val isMature: Boolean,
    val tags: List<String>
) {
    val channelId: String
        get() = channel.id.toString()
}
