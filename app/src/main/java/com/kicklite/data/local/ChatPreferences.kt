package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE)

    private val _textSize = MutableStateFlow(prefs.getFloat(TEXT_SIZE, 15f))
    val textSize: StateFlow<Float> = _textSize.asStateFlow()

    private val _compact = MutableStateFlow(prefs.getBoolean(COMPACT, true))
    val compact: StateFlow<Boolean> = _compact.asStateFlow()

    private val _hideCommands = MutableStateFlow(prefs.getBoolean(HIDE_COMMANDS, false))
    val hideCommands: StateFlow<Boolean> = _hideCommands.asStateFlow()

    private val _showBadges = MutableStateFlow(prefs.getBoolean(SHOW_BADGES, true))
    val showBadges: StateFlow<Boolean> = _showBadges.asStateFlow()

    private val _bigEmotes = MutableStateFlow(prefs.getBoolean(BIG_EMOTES, false))
    val bigEmotes: StateFlow<Boolean> = _bigEmotes.asStateFlow()

    private val _hideBots = MutableStateFlow(prefs.getBoolean(HIDE_BOTS, false))
    val hideBots: StateFlow<Boolean> = _hideBots.asStateFlow()

    fun setHideBots(v: Boolean) { _hideBots.value = v; prefs.edit().putBoolean(HIDE_BOTS, v).apply() }

    private val _highlightWords = MutableStateFlow(prefs.getString(HIGHLIGHT, "") ?: "")
    val highlightWords: StateFlow<String> = _highlightWords.asStateFlow()
    fun setHighlightWords(v: String) { _highlightWords.value = v; prefs.edit().putString(HIGHLIGHT, v).apply() }

    private val _mutedUsers = MutableStateFlow(prefs.getString(MUTED, "") ?: "")
    val mutedUsers: StateFlow<String> = _mutedUsers.asStateFlow()
    fun setMutedUsers(v: String) { _mutedUsers.value = v; prefs.edit().putString(MUTED, v).apply() }

    private val _sideWidth = MutableStateFlow(prefs.getFloat(SIDE_WIDTH, 320f))
    val sideWidth: StateFlow<Float> = _sideWidth.asStateFlow()
    fun setSideWidth(v: Float) { _sideWidth.value = v; prefs.edit().putFloat(SIDE_WIDTH, v).apply() }

    private val _chatLeft = MutableStateFlow(prefs.getBoolean(CHAT_LEFT, false))
    val chatLeft: StateFlow<Boolean> = _chatLeft.asStateFlow()
    fun setChatLeft(v: Boolean) { _chatLeft.value = v; prefs.edit().putBoolean(CHAT_LEFT, v).apply() }

    private val _animateEmotes = MutableStateFlow(prefs.getBoolean(ANIMATE, true))
    val animateEmotes: StateFlow<Boolean> = _animateEmotes.asStateFlow()
    fun setAnimateEmotes(v: Boolean) { _animateEmotes.value = v; prefs.edit().putBoolean(ANIMATE, v).apply() }

    private val _performanceMode = MutableStateFlow(prefs.getBoolean(PERF, false))
    val performanceMode: StateFlow<Boolean> = _performanceMode.asStateFlow()
    fun setPerformanceMode(v: Boolean) { _performanceMode.value = v; prefs.edit().putBoolean(PERF, v).apply() }

    private val _maxMessages = MutableStateFlow(prefs.getFloat(MAX_MSG, 300f))
    val maxMessages: StateFlow<Float> = _maxMessages.asStateFlow()
    fun setMaxMessages(v: Float) { _maxMessages.value = v; prefs.edit().putFloat(MAX_MSG, v).apply() }

    private val _showTimestamps = MutableStateFlow(prefs.getBoolean(TIMES, false))
    val showTimestamps: StateFlow<Boolean> = _showTimestamps.asStateFlow()
    fun setShowTimestamps(v: Boolean) { _showTimestamps.value = v; prefs.edit().putBoolean(TIMES, v).apply() }

    private val _emoteScale = MutableStateFlow(prefs.getFloat(EMOTE_SCALE, 1.7f))
    val emoteScale: StateFlow<Float> = _emoteScale.asStateFlow()
    fun setEmoteScale(v: Float) { _emoteScale.value = v; prefs.edit().putFloat(EMOTE_SCALE, v).apply() }

    /** Effective values used by the chat engine (performance mode caps history and slows batching). */
    fun effectiveMaxMessages(): Int =
        if (_performanceMode.value) minOf(100, _maxMessages.value.toInt()) else _maxMessages.value.toInt()
    fun batchDelayMs(): Long = if (_performanceMode.value) 500L else 250L

    fun setTextSize(v: Float) { _textSize.value = v; prefs.edit().putFloat(TEXT_SIZE, v).apply() }
    fun setCompact(v: Boolean) { _compact.value = v; prefs.edit().putBoolean(COMPACT, v).apply() }
    fun setHideCommands(v: Boolean) { _hideCommands.value = v; prefs.edit().putBoolean(HIDE_COMMANDS, v).apply() }
    fun setShowBadges(v: Boolean) { _showBadges.value = v; prefs.edit().putBoolean(SHOW_BADGES, v).apply() }
    fun setBigEmotes(v: Boolean) { _bigEmotes.value = v; prefs.edit().putBoolean(BIG_EMOTES, v).apply() }

    private companion object {
        const val ANIMATE = "animate_emotes"
        const val PERF = "performance_mode"
        const val MAX_MSG = "max_messages"
        const val TIMES = "show_timestamps"
        const val EMOTE_SCALE = "emote_scale"
        const val HIGHLIGHT = "highlight_words"
        const val MUTED = "muted_users"
        const val SIDE_WIDTH = "side_width"
        const val CHAT_LEFT = "chat_left"
        const val HIDE_BOTS = "hide_bots"
        const val TEXT_SIZE = "text_size"
        const val COMPACT = "compact"
        const val HIDE_COMMANDS = "hide_commands"
        const val SHOW_BADGES = "show_badges"
        const val BIG_EMOTES = "big_emotes"
    }
}
