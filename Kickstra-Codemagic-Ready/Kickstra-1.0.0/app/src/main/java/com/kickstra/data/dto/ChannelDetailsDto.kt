package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChannelDetails

data class ChannelDetailsDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("is_banned")
    val isBanned: Boolean,
    @SerializedName("playback_url")
    val playbackUrl: String?,
    @SerializedName("vod_enabled")
    val vodEnabled: Boolean,
    @SerializedName("subscription_enabled")
    val subscriptionEnabled: Boolean,
    @SerializedName("is_affiliate")
    val isAffiliate: Boolean,
    @SerializedName("followers_count")
    val followersCount: Int,
    @SerializedName("banner_image")
    val bannerImage: ChannelImageDto?,
    @SerializedName("offline_banner_image")
    val offlineBannerImage: ChannelImageDto?,
    @SerializedName("verified")
    val verified: Boolean,
    @SerializedName("can_host")
    val canHost: Boolean,
    @SerializedName("user")
    val user: ChannelUserDto,
    @SerializedName("livestream")
    val livestream: ChannelLivestreamDto?
) {
    fun toDomainModel(): ChannelDetails {
        return ChannelDetails(
            id = id,
            userId = userId,
            slug = slug,
            isBanned = isBanned,
            playbackUrl = playbackUrl,
            vodEnabled = vodEnabled,
            subscriptionEnabled = subscriptionEnabled,
            isAffiliate = isAffiliate,
            followersCount = followersCount,
            bannerImage = bannerImage?.toDomainModel(),
            offlineBannerImage = offlineBannerImage?.toDomainModel(),
            verified = verified,
            canHost = canHost,
            user = user.toDomainModel(),
            livestream = livestream?.toDomainModel()
        )
    }
}
