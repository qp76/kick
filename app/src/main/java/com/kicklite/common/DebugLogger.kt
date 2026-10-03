package com.kicklite.common

object DebugLogger {
    private const val DEBUG = true // Set to false for production
    
    fun log(tag: String, message: String) {
        if (DEBUG) {
            println("DEBUG [$tag]: $message")
        }
    }
    
    fun logPagination(message: String) {
        log("PAGINATION", message)
    }
    
    fun logApi(message: String) {
        log("API", message)
    }
}
