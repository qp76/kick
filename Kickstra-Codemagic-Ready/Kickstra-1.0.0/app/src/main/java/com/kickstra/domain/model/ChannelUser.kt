package com.kickstra.domain.model

data class ChannelUser(
    val id: Long,
    val username: String,
    val bio: String?,
    val country: String?,
    val instagram: String?,
    val twitter: String?,
    val youtube: String?,
    val discord: String?,
    val tiktok: String?,
    val profilePic: String
)
