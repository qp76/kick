package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChannelClip
import com.kicklite.domain.model.ClipCategory
import com.kicklite.domain.model.ClipChannel
import com.kicklite.domain.model.ClipCreator

data class ClipsResponseDto(
    @SerializedName("clips") val clips: List<ClipDto>,
    @SerializedName("nextCursor") val nextCursor: String?
)

data class ClipDto(
    @SerializedName("id") val id: String,
    @SerializedName("livestream_id") val livestreamId: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("channel_id") val channelId: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("clip_url") val clipUrl: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("privacy") val privacy: String?,
    @SerializedName("likes") val likes: Int?,
    @SerializedName("liked") val liked: Boolean?,
    @SerializedName("views") val views: Int?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("vod_starts_at") val vodStartsAt: Int?,
    @SerializedName("is_mature") val isMature: Boolean?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("view_count") val viewCount: Int?,
    @SerializedName("likes_count") val likesCount: Int?,
    @SerializedName("category") val category: ClipCategoryDto?,
    @SerializedName("creator") val creator: ClipCreatorDto?,
    @SerializedName("channel") val channel: ClipChannelDto?
){
    fun toDomainModel(): ChannelClip = ChannelClip(
        id = id,
        livestreamId = livestreamId,
        categoryId = categoryId,
        channelId = channelId,
        userId = userId,
        title = title,
        clipUrl = clipUrl,
        thumbnailUrl = thumbnailUrl,
        privacy = privacy,
        likes = likes,
        liked = liked,
        views = views,
        duration = duration,
        startedAt = startedAt,
        createdAt = createdAt,
        vodStartsAt = vodStartsAt,
        isMature = isMature,
        videoUrl = videoUrl,
        viewCount = viewCount,
        likesCount = likesCount,
        category = category?.let { ClipCategory(
            id = it.id,
            name = it.name,
            slug = it.slug,
            parentCategory = it.parentCategory
        ) },
        creator = creator?.let { ClipCreator(
            id = it.id,
            username = it.username,
            slug = it.slug
        ) },
        channel = channel?.let { ClipChannel(
            id = it.id,
            username = it.username,
            slug = it.slug,
            profilePicture = it.profilePicture
        ) }
    )
}

data class ClipCategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("parent_category") val parentCategory: String?
)

data class ClipCreatorDto(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("slug") val slug: String
)

data class ClipChannelDto(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("profile_picture") val profilePicture: String?
)

// Extension mappers removed in favor of member mapper to avoid import ambiguity
