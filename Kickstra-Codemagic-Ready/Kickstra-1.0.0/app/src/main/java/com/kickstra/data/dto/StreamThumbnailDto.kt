package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.StreamThumbnail

data class StreamThumbnailDto(
    @SerializedName("src")
    val src: String?,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("srcset")
    val srcset: String? = null
) {
    fun toDomainModel(): StreamThumbnail? {
        val resolvedSrc = src ?: url
        if (resolvedSrc == null) return null
        return StreamThumbnail(
            src = resolvedSrc,
            srcset = srcset
        )
    }
}
