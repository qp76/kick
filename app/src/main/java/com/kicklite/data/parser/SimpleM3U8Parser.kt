package com.kicklite.data.parser

import com.kicklite.data.model.StreamQuality

object SimpleM3U8Parser {
    
    fun parseManifest(manifestContent: String, baseUrl: String): List<StreamQuality> {
        val qualities = mutableListOf<StreamQuality>()
        val lines = manifestContent.lines()
        
        // Add AUTO quality first
        qualities.add(StreamQuality.AUTO.copy(url = baseUrl))
        
        var i = 0
        while (i < lines.size - 1) {
            val line = lines[i].trim()
            
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val nextLine = lines[i + 1].trim()
                if (!nextLine.startsWith("#") && nextLine.isNotEmpty()) {
                    
                    // Parse bandwidth
                    val bandwidth = extractBandwidth(line)
                    
                    // Parse resolution
                    val resolution = extractResolution(line)
                    
                    // Parse frame rate
                    val frameRate = extractFrameRate(line)
                    
                    // Parse VIDEO group ID for quality name
                    val videoId = extractVideoId(line)
                    
                    // Generate quality name
                    val qualityName = generateQualityName(resolution, frameRate, videoId)
                    
                    // Build full URL
                    val fullUrl = buildQualityUrl(baseUrl, nextLine)
                    
                    val quality = StreamQuality(
                        id = videoId ?: generateQualityId(resolution, frameRate),
                        name = qualityName,
                        resolution = resolution ?: "Unknown",
                        bandwidth = bandwidth,
                        frameRate = frameRate,
                        url = fullUrl,
                        isDefault = false
                    )
                    
                    qualities.add(quality)
                }
            }
            i++
        }
        
        return qualities.sortedByDescending { it.bandwidth }
    }
    
    private fun extractBandwidth(line: String): Long {
        val bandwidthRegex = "BANDWIDTH=(\\d+)".toRegex()
        return bandwidthRegex.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    
    private fun extractResolution(line: String): String? {
        val resolutionRegex = "RESOLUTION=(\\d+x\\d+)".toRegex()
        return resolutionRegex.find(line)?.groupValues?.get(1)
    }
    
    private fun extractFrameRate(line: String): Double {
        val frameRateRegex = "FRAME-RATE=([\\d.]+)".toRegex()
        return frameRateRegex.find(line)?.groupValues?.get(1)?.toDoubleOrNull() ?: 30.0
    }
    
    private fun extractVideoId(line: String): String? {
        val videoRegex = """VIDEO="([^"]+)"""".toRegex()
        return videoRegex.find(line)?.groupValues?.get(1)
    }
    
    private fun generateQualityName(resolution: String?, frameRate: Double, videoId: String?): String {
        if (videoId != null) {
            return when {
                videoId.contains("1080p") -> "1080p" + if (frameRate >= 60) "60" else ""
                videoId.contains("720p") -> "720p" + if (frameRate >= 60) "60 HD" else " HD"
                videoId.contains("480p") -> "480p"
                videoId.contains("360p") -> "360p"
                videoId.contains("160p") -> "160p"
                else -> videoId
            }
        }
        
        return when {
            resolution?.contains("1920x1080") == true -> "1080p" + if (frameRate >= 60) "60" else ""
            resolution?.contains("1280x720") == true -> "720p" + if (frameRate >= 60) "60 HD" else " HD"
            resolution?.contains("852x480") == true || resolution?.contains("854x480") == true -> "480p"
            resolution?.contains("640x360") == true -> "360p"
            resolution?.contains("284x160") == true -> "160p"
            else -> "Unknown Quality"
        }
    }
    
    private fun generateQualityId(resolution: String?, frameRate: Double): String {
        return when {
            resolution?.contains("1920x1080") == true -> if (frameRate >= 60) "1080p60" else "1080p30"
            resolution?.contains("1280x720") == true -> if (frameRate >= 60) "720p60" else "720p30"
            resolution?.contains("852x480") == true || resolution?.contains("854x480") == true -> "480p30"
            resolution?.contains("640x360") == true -> "360p30"
            resolution?.contains("284x160") == true -> "160p30"
            else -> "unknown"
        }
    }
    
    private fun buildQualityUrl(baseUrl: String, playlistPath: String): String {
        return if (baseUrl.endsWith("master.m3u8")) {
            val basePath = baseUrl.removeSuffix("master.m3u8")
            "$basePath$playlistPath"
        } else {
            // For other URLs, try to find the base path
            val lastSlashIndex = baseUrl.lastIndexOf('/')
            if (lastSlashIndex != -1) {
                val basePath = baseUrl.substring(0, lastSlashIndex + 1)
                "$basePath$playlistPath"
            } else {
                playlistPath
            }
        }
    }
}
