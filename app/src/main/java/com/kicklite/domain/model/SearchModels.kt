package com.kicklite.domain.model

data class SearchResponse(
    val channels: List<ChannelSearchResult>,
    val categories: List<CategorySearchResult>,
    val tags: List<TagSearchResult>
)

data class ChannelSearchResult(
    val id: String,
    val username: String,
    val slug: String,
    val followersCount: Long,
    val isLive: Boolean,
    val isBanned: Boolean,
    val verified: Boolean,
    val profileImageUrl: String? = null
)

data class CategorySearchResult(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val parent: String,
    val categoryId: Int,
    val isLive: Boolean,
    val isMature: Boolean,
    val thumbnailUrl: String?
)

data class TagSearchResult(
    val id: String,
    val label: String,
    val normalizedLabel: String,
    val createdAt: String
)
