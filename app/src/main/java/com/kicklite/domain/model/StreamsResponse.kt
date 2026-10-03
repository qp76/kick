package com.kicklite.domain.model

data class StreamsResponse(
    val livestreams: List<LiveStream>,
    val pagination: Pagination
)
