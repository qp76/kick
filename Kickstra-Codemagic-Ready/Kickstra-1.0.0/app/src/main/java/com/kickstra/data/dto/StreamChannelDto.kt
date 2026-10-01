package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.StreamChannel

data class StreamChannelDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("profile_pic")
    val profilePic: String,
    @SerializedName("username")
    val username: String
) {
    fun toDomainModel(): StreamChannel {
        return StreamChannel(
            id = id,
            slug = slug,
            profilePic = profilePic,
            username = username
        )
    }
}
