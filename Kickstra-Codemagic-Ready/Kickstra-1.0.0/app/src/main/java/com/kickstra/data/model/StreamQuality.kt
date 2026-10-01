package com.kickstra.data.model

data class StreamQuality(
    val id: String,
    val name: String,
    val resolution: String,
    val bandwidth: Long,
    val frameRate: Double,
    val url: String,
    val isDefault: Boolean = false
) {
    companion object {
        val AUTO = StreamQuality(
            id = "auto",
            name = "Auto",
            resolution = "Auto",
            bandwidth = 0,
            frameRate = 0.0,
            url = "",
            isDefault = true
        )
    }
}

data class StreamManifest(
    val qualities: List<StreamQuality>,
    val autoQualityUrl: String
)
