package com.kickstra.data.parser

import com.kickstra.data.model.StreamQuality

object M3U8Parser {
    
    fun parseManifest(manifestContent: String, baseUrl: String): List<StreamQuality> {
        println("DEBUG M3U8Parser: Starting to parse manifest")
        println("DEBUG M3U8Parser: Base URL: $baseUrl")
        println("DEBUG M3U8Parser: Manifest content length: ${manifestContent.length}")
        
        val qualities = mutableListOf<StreamQuality>()
        val lines = manifestContent.lines()
        println("DEBUG M3U8Parser: Total lines in manifest: ${lines.size}")
        
        // Add AUTO quality first
        qualities.add(StreamQuality.AUTO.copy(url = baseUrl))
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for stream info lines
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                println("DEBUG M3U8Parser: Found stream info line at $i: $line")
                val streamInfo = parseStreamInfo(line)
                println("DEBUG M3U8Parser: Parsed stream info: $streamInfo")
                
                // Get the next line which should be the playlist URL
                if (i + 1 < lines.size) {
                    val playlistPath = lines[i + 1].trim()
                    println("DEBUG M3U8Parser: Playlist path: $playlistPath")
                    
                    if (playlistPath.isNotEmpty() && !playlistPath.startsWith("#")) {
                        val fullUrl = buildQualityUrl(baseUrl, playlistPath)
                        println("DEBUG M3U8Parser: Built full URL: $fullUrl")
                        
                        val quality = StreamQuality(
                            id = streamInfo["VIDEO"] ?: generateQualityId(streamInfo),
                            name = generateQualityName(streamInfo),
                            resolution = streamInfo["RESOLUTION"] ?: "Unknown",
                            bandwidth = streamInfo["BANDWIDTH"]?.toLongOrNull() ?: 0L,
                            frameRate = streamInfo["FRAME-RATE"]?.toDoubleOrNull() ?: 30.0,
                            url = fullUrl,
                            isDefault = false
                        )
                        
                        println("DEBUG M3U8Parser: Created quality: ${quality.name} - ${quality.resolution}")
                        qualities.add(quality)
                    }
                }
            }
            i++
        }
        
        println("DEBUG M3U8Parser: Total qualities found: ${qualities.size}")
        return qualities.sortedByDescending { it.bandwidth }
    }
    
    private fun parseStreamInfo(streamInfoLine: String): Map<String, String> {
        println("DEBUG M3U8Parser: Parsing stream info line: $streamInfoLine")
        val attributes = mutableMapOf<String, String>()
        
        // Remove the #EXT-X-STREAM-INF: prefix
        val attributeString = streamInfoLine.substring("#EXT-X-STREAM-INF:".length)
        println("DEBUG M3U8Parser: Attribute string: $attributeString")
        
        // Split by comma, but handle quoted values
        val parts = splitAttributes(attributeString)
        println("DEBUG M3U8Parser: Split parts: $parts")
        
        for (part in parts) {
            val keyValue = part.split("=", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim().removePrefix("\"").removeSuffix("\"")
                attributes[key] = value
                println("DEBUG M3U8Parser: Added attribute: $key = $value")
            }
        }
        
        println("DEBUG M3U8Parser: Final attributes: $attributes")
        return attributes
    }
    
    private fun splitAttributes(attributeString: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in attributeString) {
            when (char) {
                '"' -> {
                    inQuotes = !inQuotes
                    current.append(char)
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        parts.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }
        
        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }
        
        return parts
    }
    
    private fun generateQualityId(streamInfo: Map<String, String>): String {
        val video = streamInfo["VIDEO"]
        if (video != null) return video
        
        val resolution = streamInfo["RESOLUTION"]
        val frameRate = streamInfo["FRAME-RATE"]?.toDoubleOrNull()
        
        return when {
            resolution?.contains("1920x1080") == true -> if (frameRate == 60.0) "1080p60" else "1080p30"
            resolution?.contains("1280x720") == true -> if (frameRate == 60.0) "720p60" else "720p30"
            resolution?.contains("852x480") == true || resolution?.contains("854x480") == true -> "480p30"
            resolution?.contains("640x360") == true -> "360p30"
            resolution?.contains("284x160") == true -> "160p30"
            else -> "unknown"
        }
    }
    
    private fun generateQualityName(streamInfo: Map<String, String>): String {
        val resolution = streamInfo["RESOLUTION"]
        val frameRate = streamInfo["FRAME-RATE"]?.toDoubleOrNull()
        
        return when {
            resolution?.contains("1920x1080") == true -> if (frameRate == 60.0) "1080p60" else "1080p"
            resolution?.contains("1280x720") == true -> if (frameRate == 60.0) "720p60 HD" else "720p HD"
            resolution?.contains("852x480") == true || resolution?.contains("854x480") == true -> "480p"
            resolution?.contains("640x360") == true -> "360p"
            resolution?.contains("284x160") == true -> "160p"
            else -> "Unknown Quality"
        }
    }
    
    private fun buildQualityUrl(baseUrl: String, playlistPath: String): String {
        // Base URL format: https://stream.kick.com/ivs/v1/196233775518/cElH0TQoKxP8/2025/9/9/18/43/1tjbbeQukCAr/media/hls/master.m3u8
        // Target URL format: https://stream.kick.com/ivs/v1/196233775518/cElH0TQoKxP8/2025/9/9/18/43/1tjbbeQukCAr/media/hls/720p60/playlist.m3u8
        
        return if (baseUrl.endsWith("master.m3u8")) {
            val basePath = baseUrl.removeSuffix("master.m3u8")
            "$basePath$playlistPath"
        } else {
            // If base URL doesn't end with master.m3u8, try to construct it
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
