package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChannelImage

data class ChannelImageDto(
    @SerializedName("url")
    val url: String?,
    @SerializedName("src")
    val src: String? = null,
    @SerializedName("srcset")
    val srcset: String? = null
) {
    fun toDomainModel(): ChannelImage? {
        return if (url != null) {
            ChannelImage(
                url = url,
                src = src,
                srcset = srcset
            )
        } else {
            null
        }
    }
}
