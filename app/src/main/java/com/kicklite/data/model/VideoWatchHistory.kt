package com.kicklite.data.model

data class VideoWatchHistory(
    val videoId: String,
    val username: String,
    val lastWatchedPosition: Long, // milliseconds
    val totalDuration: Long, // milliseconds
    val lastWatchedAt: Long, // timestamp
    val isCompleted: Boolean = false // Video %90+ izlendiyse true
) {
    companion object {
        // Video %90+ izlendiyse tamamlanmış sayıyoruz
        fun isVideoCompleted(watchedPosition: Long, totalDuration: Long): Boolean {
            return if (totalDuration > 0) {
                (watchedPosition.toDouble() / totalDuration.toDouble()) >= 0.9
            } else false
        }
        
        // Video %5'den fazla izlendiyse kaydetmeye değer
        fun shouldSaveProgress(watchedPosition: Long, totalDuration: Long): Boolean {
            return if (totalDuration > 0) {
                val progress = watchedPosition.toDouble() / totalDuration.toDouble()
                progress >= 0.05 && progress < 0.9 // %5 ile %90 arası
            } else false
        }
    }
}
