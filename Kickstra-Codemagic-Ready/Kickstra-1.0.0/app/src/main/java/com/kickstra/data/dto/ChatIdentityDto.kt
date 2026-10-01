package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChatIdentity

data class ChatIdentityDto(
    @SerializedName("color")
    val color: String,
    @SerializedName("badges")
    val badges: List<ChatBadgeDto>
) {
    fun toDomainModel(): ChatIdentity {
        return ChatIdentity(
            color = color,
            badges = badges.map { it.toDomainModel() }
        )
    }
}
