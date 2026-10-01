package com.kickstra.domain.model

data class StreamsResponse(
    val livestreams: List<LiveStream>,
    val pagination: Pagination
)
