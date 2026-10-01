package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChatSender

data class ChatSenderDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("identity")
    val identity: ChatIdentityDto
) {
    fun toDomainModel(): ChatSender {
        return ChatSender(
            id = id,
            slug = slug,
            username = username,
            identity = identity.toDomainModel()
        )
    }
}
