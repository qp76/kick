package com.kicklite.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kicklite.data.model.VideoWatchHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoWatchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREF_NAME = "video_watch_history"
        private const val KEY_HISTORY = "watch_history"
        private const val MAX_HISTORY_SIZE = 100 // Maksimum 100 video geçmişi tut
    }
    
    /**
     * Video izleme pozisyonunu kaydet
     */
    fun saveWatchPosition(
        videoId: String,
        username: String,
        currentPosition: Long,
        totalDuration: Long
    ) {
        // Kaydetmeye değer mi kontrol et
        if (!VideoWatchHistory.shouldSaveProgress(currentPosition, totalDuration)) {
            println("DEBUG: Watch position not worth saving (${currentPosition}ms/${totalDuration}ms)")
            return
        }
        
        val historyList = getWatchHistoryList().toMutableList()
        val existingIndex = historyList.indexOfFirst { 
            it.videoId == videoId && it.username == username 
        }
        
        val isCompleted = VideoWatchHistory.isVideoCompleted(currentPosition, totalDuration)
        val watchHistory = VideoWatchHistory(
            videoId = videoId,
            username = username,
            lastWatchedPosition = currentPosition,
            totalDuration = totalDuration,
            lastWatchedAt = System.currentTimeMillis(),
            isCompleted = isCompleted
        )
        
        if (existingIndex != -1) {
            // Varsa güncelle
            historyList[existingIndex] = watchHistory
        } else {
            // Yoksa ekle
            historyList.add(0, watchHistory) // En başa ekle
        }
        
        // Maksimum boyutu aş
        if (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(historyList.size - 1)
        }
        
        // Kaydet
        saveWatchHistoryList(historyList)
        
        println("DEBUG: Saved watch position: $videoId at ${currentPosition}ms (${if(isCompleted) "completed" else "in progress"})")
    }
    
    /**
     * Video için son izleme pozisyonunu al
     */
    fun getWatchPosition(videoId: String, username: String): VideoWatchHistory? {
        return getWatchHistoryList().find { 
            it.videoId == videoId && it.username == username && !it.isCompleted
        }
    }
    
    /**
     * Video tamamlandığında işaretle (izleme geçmişinden kaldırmak için)
     */
    fun markVideoAsCompleted(videoId: String, username: String) {
        val historyList = getWatchHistoryList().toMutableList()
        val index = historyList.indexOfFirst { 
            it.videoId == videoId && it.username == username 
        }
        
        if (index != -1) {
            historyList[index] = historyList[index].copy(isCompleted = true)
            saveWatchHistoryList(historyList)
            println("DEBUG: Marked video as completed: $videoId")
        }
    }
    
    /**
     * Belirli bir video için geçmişi temizle
     */
    fun clearWatchHistory(videoId: String, username: String) {
        val historyList = getWatchHistoryList().toMutableList()
        historyList.removeAll { it.videoId == videoId && it.username == username }
        saveWatchHistoryList(historyList)
        println("DEBUG: Cleared watch history for: $videoId")
    }
    
    /**
     * Tüm izleme geçmişini al (UI için)
     */
    fun getAllWatchHistory(): List<VideoWatchHistory> {
        return getWatchHistoryList()
            .filter { !it.isCompleted }
            .sortedByDescending { it.lastWatchedAt }
    }
    
    /**
     * Video'nun tamamlanıp tamamlanmadığını kontrol et
     */
    fun isVideoCompleted(videoId: String, username: String): Boolean {
        return getWatchHistoryList().any { 
            it.videoId == videoId && it.username == username && it.isCompleted
        }
    }
    
    /**
     * Eski geçmişi temizle (30 günden eski)
     */
    fun cleanOldHistory() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val historyList = getWatchHistoryList().toMutableList()
        val originalSize = historyList.size
        
        historyList.removeAll { it.lastWatchedAt < thirtyDaysAgo }
        
        if (historyList.size < originalSize) {
            saveWatchHistoryList(historyList)
            println("DEBUG: Cleaned old watch history: ${originalSize - historyList.size} entries removed")
        }
    }
    
    // Private helper methods
    private fun getWatchHistoryList(): List<VideoWatchHistory> {
        return try {
            val json = sharedPreferences.getString(KEY_HISTORY, null) ?: return emptyList()
            val type = object : TypeToken<List<VideoWatchHistory>>() {}.type
            gson.fromJson<List<VideoWatchHistory>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            println("DEBUG: Error reading watch history: ${e.message}")
            emptyList()
        }
    }
    
    private fun saveWatchHistoryList(historyList: List<VideoWatchHistory>) {
        try {
            val json = gson.toJson(historyList)
            sharedPreferences.edit()
                .putString(KEY_HISTORY, json)
                .apply()
        } catch (e: Exception) {
            println("DEBUG: Error saving watch history: ${e.message}")
        }
    }
}
