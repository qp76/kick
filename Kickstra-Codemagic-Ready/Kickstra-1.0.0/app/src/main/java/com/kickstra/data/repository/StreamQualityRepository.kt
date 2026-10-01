package com.kickstra.data.repository

import com.kickstra.data.model.StreamQuality
import com.kickstra.data.model.StreamManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamQualityRepository @Inject constructor() {
    
    suspend fun getStreamQualities(manifestUrl: String): StreamManifest = withContext(Dispatchers.IO) {
        try {
            val manifestContent = fetchManifestContent(manifestUrl)
            parseM3U8Manifest(manifestContent)
        } catch (e: Exception) {
            // Return auto quality as fallback
            StreamManifest(
                qualities = listOf(StreamQuality.AUTO),
                autoQualityUrl = manifestUrl
            )
        }
    }
    
    private suspend fun fetchManifestContent(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain")
        connection.setRequestProperty("Referer", "https://kick.com/")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36")
        
        connection.getInputStream().bufferedReader().use { it.readText() }
    }
    
    private fun parseM3U8Manifest(content: String): StreamManifest {
        val qualities = mutableListOf<StreamQuality>()
        val lines = content.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val streamInfo = parseStreamInfo(line)
                if (i + 1 < lines.size) {
                    val url = lines[i + 1].trim()
                    if (url.startsWith("http")) {
                        qualities.add(
                            StreamQuality(
                                id = streamInfo["resolution"] ?: "unknown",
                                name = formatQualityName(streamInfo),
                                resolution = streamInfo["resolution"] ?: "Unknown",
                                bandwidth = streamInfo["bandwidth"]?.toLongOrNull() ?: 0L,
                                frameRate = streamInfo["frameRate"]?.toDoubleOrNull() ?: 30.0,
                                url = url,
                                isDefault = streamInfo["default"] == "YES"
                            )
                        )
                    }
                }
                i += 2
            } else {
                i++
            }
        }
        
        // Sort qualities by bandwidth (highest first)
        val sortedQualities = qualities.sortedByDescending { it.bandwidth }
        
        // Add AUTO option at the beginning
        val finalQualities = mutableListOf(StreamQuality.AUTO).apply {
            addAll(sortedQualities)
        }
        
        // Use the first manifest URL as auto quality URL
        val autoUrl = sortedQualities.firstOrNull()?.url ?: ""
        
        return StreamManifest(
            qualities = finalQualities,
            autoQualityUrl = autoUrl
        )
    }
    
    private fun parseStreamInfo(line: String): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        // Parse BANDWIDTH
        val bandwidthRegex = """BANDWIDTH=(\d+)""".toRegex()
        bandwidthRegex.find(line)?.let {
            info["bandwidth"] = it.groupValues[1]
        }
        
        // Parse RESOLUTION
        val resolutionRegex = """RESOLUTION=(\d+x\d+)""".toRegex()
        resolutionRegex.find(line)?.let {
            info["resolution"] = it.groupValues[1]
        }
        
        // Parse FRAME-RATE
        val frameRateRegex = """FRAME-RATE=([\d.]+)""".toRegex()
        frameRateRegex.find(line)?.let {
            info["frameRate"] = it.groupValues[1]
        }
        
        // Parse DEFAULT
        val defaultRegex = """DEFAULT=(\w+)""".toRegex()
        defaultRegex.find(line)?.let {
            info["default"] = it.groupValues[1]
        }
        
        return info
    }
    
    private fun formatQualityName(streamInfo: Map<String, String>): String {
        val resolution = streamInfo["resolution"]
        val frameRate = streamInfo["frameRate"]?.toDoubleOrNull()
        
        return when {
            resolution != null && frameRate != null -> {
                val height = resolution.split("x")[1]
                if (frameRate == 60.0) "${height}p60" else "${height}p"
            }
            resolution != null -> {
                val height = resolution.split("x")[1]
                "${height}p"
            }
            else -> "Unknown"
        }
    }
}
