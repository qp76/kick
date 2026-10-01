package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.StreamCategory

data class StreamCategoryDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String
) {
    fun toDomainModel(): StreamCategory {
        return StreamCategory(
            id = id,
            name = name,
            slug = slug
        )
    }
}
