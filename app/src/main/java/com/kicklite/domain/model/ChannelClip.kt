package com.kicklite.domain.model

data class ChannelClip(
    val id: String,
    val livestreamId: String?,
    val categoryId: String?,
    val channelId: Long,
    val userId: Long,
    val title: String,
    val clipUrl: String?,
    val thumbnailUrl: String?,
    val privacy: String?,
    val likes: Int?,
    val liked: Boolean?,
    val views: Int?,
    val duration: Int?,
    val startedAt: String?,
    val createdAt: String?,
    val vodStartsAt: Int?,
    val isMature: Boolean?,
    val videoUrl: String?,
    val viewCount: Int?,
    val likesCount: Int?,
    val category: ClipCategory?,
    val creator: ClipCreator?,
    val channel: ClipChannel?
)

data class ClipCategory(
    val id: Int,
    val name: String,
    val slug: String,
    val parentCategory: String?
)

data class ClipCreator(
    val id: Long,
    val username: String,
    val slug: String
)

data class ClipChannel(
    val id: Long,
    val username: String,
    val slug: String,
    val profilePicture: String?
)
