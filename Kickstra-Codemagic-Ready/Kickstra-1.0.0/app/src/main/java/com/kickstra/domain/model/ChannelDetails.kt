package com.kickstra.domain.model

data class ChannelDetails(
    val id: Long,
    val userId: Long,
    val slug: String,
    val isBanned: Boolean,
    val playbackUrl: String?,
    val vodEnabled: Boolean,
    val subscriptionEnabled: Boolean,
    val isAffiliate: Boolean,
    val followersCount: Int,
    val bannerImage: ChannelImage?,
    val offlineBannerImage: ChannelImage?,
    val verified: Boolean,
    val canHost: Boolean,
    val user: ChannelUser,
    val livestream: ChannelLivestream?
)
