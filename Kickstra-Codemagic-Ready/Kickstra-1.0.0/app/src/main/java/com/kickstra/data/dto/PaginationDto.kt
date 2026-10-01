package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.Pagination

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
