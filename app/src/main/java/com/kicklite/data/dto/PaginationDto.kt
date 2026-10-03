package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.Pagination

data class PaginationDto(
    @SerializedName("next_cursor")
    val nextCursor: String?
) {
    fun toDomainModel(): Pagination {
        return Pagination(
            nextCursor = nextCursor
        )
    }
}
