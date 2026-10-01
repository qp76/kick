package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName

data class SearchApiResponse(
    @SerializedName("channels") val channels: List<SearchChannelDto>?,
    @SerializedName("categories") val categories: List<SearchCategoryDto>?,
    @SerializedName("livestreams") val livestreams: SearchLivestreamsDto?
)

data class SearchChannelDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("followers_count") val followersCount: Long?,
    @SerializedName("followersCount") val followersCountCamel: Long?,
    @SerializedName("is_banned") val isBanned: Boolean?,
    @SerializedName("isLive") val isLive: Boolean?,
    @SerializedName("user") val user: SearchChannelUserDto?,
    @SerializedName("verified") val verified: SearchChannelVerifiedDto?
)

data class SearchChannelUserDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("profilePic") val profilePic: String?
)

data class SearchChannelVerifiedDto(
    @SerializedName("id") val id: Long?
)

data class SearchCategoryDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("is_mature") val isMature: Boolean?,
    @SerializedName("viewers") val viewers: Long?,
    @SerializedName("category") val parentCategory: SearchParentCategoryDto?,
    @SerializedName("banner") val banner: SearchCategoryBannerDto?
)

data class SearchParentCategoryDto(
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?
)

data class SearchCategoryBannerDto(
    @SerializedName("src") val src: String?,
    @SerializedName("srcset") val srcset: String?
)

data class SearchLivestreamsDto(
    @SerializedName("tags") val tags: List<SearchTagDto>?
)

data class SearchTagDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("normalized_name") val normalizedName: String?,
    @SerializedName("created_at") val createdAt: String?
)
