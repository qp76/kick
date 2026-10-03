package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChannelUser

data class ChannelUserDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("instagram")
    val instagram: String?,
    @SerializedName("twitter")
    val twitter: String?,
    @SerializedName("youtube")
    val youtube: String?,
    @SerializedName("discord")
    val discord: String?,
    @SerializedName("tiktok")
    val tiktok: String?,
    @SerializedName("profile_pic")
    val profilePic: String
) {
    fun toDomainModel(): ChannelUser {
        return ChannelUser(
            id = id,
            username = username,
            bio = bio,
            country = country,
            instagram = instagram,
            twitter = twitter,
            youtube = youtube,
            discord = discord,
            tiktok = tiktok,
            profilePic = profilePic
        )
    }
}
