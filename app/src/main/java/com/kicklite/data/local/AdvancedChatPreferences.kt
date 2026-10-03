package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedChatPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("advanced_chat_settings", Context.MODE_PRIVATE)
    private val _mutedUsers = MutableStateFlow(readSet("muted_users"))
    val mutedUsers: StateFlow<Set<String>> = _mutedUsers.asStateFlow()
    private val _highlightWords = MutableStateFlow(prefs.getString("highlight_words", "").orEmpty())
    val highlightWords: StateFlow<String> = _highlightWords.asStateFlow()
    private val _chatWidth = MutableStateFlow(prefs.getInt("chat_width", 320))
    val chatWidth: StateFlow<Int> = _chatWidth.asStateFlow()
    private val _chatSide = MutableStateFlow(prefs.getString("chat_side", "right") ?: "right")
    val chatSide: StateFlow<String> = _chatSide.asStateFlow()
    private val _dataSaver = MutableStateFlow(prefs.getBoolean("data_saver", false))
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    fun toggleMute(username: String) {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return
        val next = _mutedUsers.value.toMutableSet().apply { if (!add(normalized)) remove(normalized) }
        _mutedUsers.value = next
        prefs.edit().putStringSet("muted_users", next).apply()
    }
    fun setHighlightWords(value: String) { _highlightWords.value = value; prefs.edit().putString("highlight_words", value).apply() }
    fun setChatWidth(value: Int) { _chatWidth.value = value.coerceIn(240, 520); prefs.edit().putInt("chat_width", _chatWidth.value).apply() }
    fun setChatSide(value: String) { _chatSide.value = if (value == "left") "left" else "right"; prefs.edit().putString("chat_side", _chatSide.value).apply() }
    fun setDataSaver(value: Boolean) { _dataSaver.value = value; prefs.edit().putBoolean("data_saver", value).apply() }

    private fun readSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().map { it.lowercase() }.toSet()
}
