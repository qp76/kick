package com.kickstra.data.mapper

import com.kickstra.data.dto.SearchApiResponse
import com.kickstra.data.dto.SearchCategoryDto
import com.kickstra.data.dto.SearchChannelDto
import com.kickstra.data.dto.SearchTagDto
import com.kickstra.domain.model.ChannelSearchResult
import com.kickstra.domain.model.CategorySearchResult
import com.kickstra.domain.model.SearchResponse
import com.kickstra.domain.model.TagSearchResult

fun SearchApiResponse.toDomainModel(): SearchResponse {
    val channels = channels.orEmpty().map { it.toDomain() }
    val categories = categories.orEmpty().map { it.toDomain() }
    val tags = livestreams?.tags.orEmpty().map { it.toDomain() }

    return SearchResponse(
        channels = channels,
        categories = categories,
        tags = tags
    )
}

fun SearchChannelDto.toDomain(): ChannelSearchResult {
    return ChannelSearchResult(
        id = id?.toString() ?: slug.orEmpty(),
        username = user?.username ?: slug.orEmpty(),
        slug = slug.orEmpty(),
        followersCount = followersCount ?: followersCountCamel ?: 0L,
        isLive = isLive ?: false,
        isBanned = isBanned ?: false,
        verified = verified != null,
        profileImageUrl = user?.profilePic
    )
}

fun SearchCategoryDto.toDomain(): CategorySearchResult {
    return CategorySearchResult(
        id = id?.toString() ?: slug.orEmpty(),
        name = name.orEmpty(),
        slug = slug.orEmpty(),
        description = description.orEmpty(),
        parent = parentCategory?.name.orEmpty(),
        categoryId = categoryId ?: 0,
        isLive = (viewers ?: 0L) > 0,
        isMature = isMature ?: false,
        thumbnailUrl = banner?.src
    )
}

fun SearchTagDto.toDomain(): TagSearchResult {
    val labelValue = name ?: slug ?: ""
    return TagSearchResult(
        id = id?.toString() ?: slug.orEmpty(),
        label = labelValue,
        normalizedLabel = normalizedName ?: labelValue.lowercase(),
        createdAt = createdAt.orEmpty()
    )
}
