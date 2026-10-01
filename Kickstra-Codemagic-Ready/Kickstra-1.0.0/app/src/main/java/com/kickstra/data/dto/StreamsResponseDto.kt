package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.StreamsResponse

data class StreamsResponseDto(
    @SerializedName("livestreams")
    val livestreams: List<LiveStreamDto>,
    @SerializedName("pagination")
    val pagination: PaginationDto
) {
    fun toDomainModel(): StreamsResponse {
        return StreamsResponse(
            livestreams = livestreams.map { it.toDomainModel() },
            pagination = pagination.toDomainModel()
        )
    }
}

data class PopularStreamsApiResponse(
    @SerializedName("data")
    val data: StreamsResponseDto
)
